/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.seata.rm.datasource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Lists;
import io.netty.channel.ChannelHandlerContext;
import org.apache.seata.common.thread.NamedThreadFactory;
import org.apache.seata.common.util.IOUtil;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.core.model.BranchStatus;
import org.apache.seata.core.protocol.RpcMessage;
import org.apache.seata.core.rpc.processor.client.RmBranchCommitProcessor;
import org.apache.seata.rm.datasource.undo.UndoLogManager;
import org.apache.seata.rm.datasource.undo.UndoLogManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.seata.common.DefaultValues.DEFAULT_CLIENT_ASYNC_COMMIT_BUFFER_LIMIT;
import static org.apache.seata.core.constants.ConfigurationKeys.CLIENT_ASYNC_COMMIT_BUFFER_LIMIT;

/**
 * The type Async worker.
 *
 */
public class AsyncWorker {

    private static final Logger LOGGER = LoggerFactory.getLogger(AsyncWorker.class);

    private static final int DEFAULT_RESOURCE_SIZE = 16;

    private static final int UNDOLOG_DELETE_LIMIT_SIZE = 1000;

    private static final int ASYNC_COMMIT_BUFFER_LIMIT = ConfigurationFactory.getInstance().getInt(
            CLIENT_ASYNC_COMMIT_BUFFER_LIMIT, DEFAULT_CLIENT_ASYNC_COMMIT_BUFFER_LIMIT);

    private final DataSourceManager dataSourceManager;

    /**
     * <p>二阶段任务队列</p>
     * 添加入口
     * @see RmBranchCommitProcessor#process(ChannelHandlerContext, RpcMessage)
     */
    private final BlockingQueue<Phase2Context> commitQueue;

    private final ScheduledExecutorService scheduledExecutor;

    // 执行二阶段任务的 worker。
    // 线程名前缀是：AsyncWorker
    // 线程都为 daemon 线程。
    public AsyncWorker(DataSourceManager dataSourceManager) {
        this.dataSourceManager = dataSourceManager;

        LOGGER.info("Async Commit Buffer Limit: {}", ASYNC_COMMIT_BUFFER_LIMIT);
        commitQueue = new LinkedBlockingQueue<>(ASYNC_COMMIT_BUFFER_LIMIT);

        ThreadFactory threadFactory = new NamedThreadFactory("AsyncWorker", 2, true);
        scheduledExecutor = new ScheduledThreadPoolExecutor(2, threadFactory);
        scheduledExecutor.scheduleAtFixedRate(this::doBranchCommitSafely, 10, 1000, TimeUnit.MILLISECONDS);
    }

    // 分支事务提交。
    // 添加到任务队列后，直接返回[二阶段已提交]信号。
    public BranchStatus branchCommit(String xid, long branchId, String resourceId) {
        Phase2Context context = new Phase2Context(xid, branchId, resourceId);
        addToCommitQueue(context);
        return BranchStatus.PhaseTwo_Committed;
    }

    /**
     * try add context to commitQueue directly, if fail(which means the queue is full),
     * then doBranchCommit urgently(so that the queue could be empty again) and retry this process.
     */
    private void addToCommitQueue(Phase2Context context) {
        // 向队列中添加任务成功。
        if (commitQueue.offer(context)) {
            return;
        }
        // 添加失败，先执行自信已有的任务后，腾出空间，再次重试添加。
        CompletableFuture.runAsync(this::doBranchCommitSafely, scheduledExecutor)
                .thenRun(() -> addToCommitQueue(context));
    }

    private void addAllToCommitQueue(List<Phase2Context> contexts) {
        for (Phase2Context context : contexts) {
            addToCommitQueue(context);
        }
    }

    void doBranchCommitSafely() {
        try {
            doBranchCommit();
        } catch (Throwable e) {
            LOGGER.error("Exception occur when doing branch commit", e);
        }
    }

    private void doBranchCommit() {
        // 先查看队列中是否有任务，无任务直接返回。
        if (commitQueue.isEmpty()) {
            return;
        }

        // 把队列中的二阶段任务全部移动到列表中。
        // transfer all context currently received to this list
        List<Phase2Context> allContexts = new LinkedList<>();
        commitQueue.drainTo(allContexts);

        // 按 resourceId 进行分组。应该是批量进行处理。
        // group context by their resourceId
        Map<String, List<Phase2Context>> groupedContexts = groupedByResourceId(allContexts);

        groupedContexts.forEach(this::dealWithGroupedContexts);
    }

    Map<String, List<Phase2Context>> groupedByResourceId(List<Phase2Context> contexts) {
        Map<String, List<Phase2Context>> groupedContexts = new HashMap<>(DEFAULT_RESOURCE_SIZE);
        contexts.forEach(context -> {
            if (StringUtils.isBlank(context.resourceId)) {
                LOGGER.warn("resourceId is empty, resource:{}", context);
                return;
            }
            List<Phase2Context> group = groupedContexts.computeIfAbsent(context.resourceId, key -> new LinkedList<>());
            group.add(context);
        });
        return groupedContexts;
    }

    private void dealWithGroupedContexts(String resourceId, List<Phase2Context> contexts) {
        if (StringUtils.isBlank(resourceId)) {
            //ConcurrentHashMap required notNull key
            LOGGER.warn("resourceId is empty and will skip.");
            return;
        }
        // 没有数据源，直接返回。
        DataSourceProxy dataSourceProxy = dataSourceManager.get(resourceId);
        if (dataSourceProxy == null) {
            LOGGER.warn("failed to find resource for {} and requeue", resourceId);
            addAllToCommitQueue(contexts);
            return;
        }

        // 有数据源，处理二阶段的任务。
        Connection conn = null;
        try {
            // 新申请一个原始物理连接
            conn = dataSourceProxy.getPlainConnection();
            UndoLogManager undoLogManager = UndoLogManagerFactory.getUndoLogManager(dataSourceProxy.getDbType());

            // 分批次进行删除。
            // split contexts into several lists, with each list contain no more element than limit size
            List<List<Phase2Context>> splitByLimit = Lists.partition(contexts, UNDOLOG_DELETE_LIMIT_SIZE);
            for (List<Phase2Context> partition : splitByLimit) {
                deleteUndoLog(conn, undoLogManager, partition);
            }
        } catch (SQLException sqlExx) {
            addAllToCommitQueue(contexts);
            LOGGER.error("failed to get connection for async committing on {} and requeue", resourceId, sqlExx);
        } finally {
            IOUtil.close(conn);
        }

    }

    private void deleteUndoLog(final Connection conn, UndoLogManager undoLogManager, List<Phase2Context> contexts) {
        Set<String> xids = new LinkedHashSet<>(contexts.size());
        Set<Long> branchIds = new LinkedHashSet<>(contexts.size());
        contexts.forEach(context -> {
            xids.add(context.xid);
            branchIds.add(context.branchId);
        });

        try {
            undoLogManager.batchDeleteUndoLog(xids, branchIds, conn);
            if (!conn.getAutoCommit()) {
                conn.commit();
            }
        } catch (SQLException e) {
            LOGGER.error("Failed to batch delete undo log", e);
            try {
                conn.rollback();
                addAllToCommitQueue(contexts);
            } catch (SQLException rollbackEx) {
                LOGGER.error("Failed to rollback JDBC resource after deleting undo log failed", rollbackEx);
            }
        }
    }

    // 二阶段上下文
    static class Phase2Context {

        /**
         * AT Phase 2 context
         * @param xid             the xid
         * @param branchId        the branch id
         * @param resourceId      the resource id
         */
        public Phase2Context(String xid, long branchId, String resourceId) {
            this.xid = xid;
            this.branchId = branchId;
            this.resourceId = resourceId;
        }

        /**
         * The Xid.
         */
        String xid;
        /**
         * The Branch id.
         */
        long branchId;
        /**
         * The Resource id.
         */
        String resourceId;

        @Override
        public String toString() {
            return "Phase2Context{" + "xid='" + xid + '\'' + ", branchId=" + branchId + ", resourceId='" + resourceId
                + '\'' + '}';
        }
    }
}
