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
package org.apache.seata.core.model;

import java.util.Map;

/**
 * <p>资源管理器：
 * <ul>1、注册资源</ul>
 * <ul>2、注销资源</ul>
 * <ul>3、获取资源</ul>
 * </p>
 * Resource Manager: common behaviors.
 *
 */
public interface ResourceManager extends ResourceManagerInbound, ResourceManagerOutbound {

    /**
     * <p>注册资源到资源管理器。</p>
     * Register a Resource to be managed by Resource Manager.
     *
     * @param resource The resource to be managed.
     */
    void registerResource(Resource resource);

    /**
     * <p>从资源管理器中注销资源。</p>
     * Unregister a Resource from the Resource Manager.
     *
     * @param resource The resource to be removed.
     */
    void unregisterResource(Resource resource);

    /**
     * <p>获取管理器管理的所有资源。</p>
     * Get all resources managed by this manager.
     *
     * @return resourceId -- Resource Map
     */
    Map<String, Resource> getManagedResources();

    /**
     * <p>获取分支事务类型。</p>
     * Get the BranchType.
     *
     * @return The BranchType of ResourceManager.
     */
    BranchType getBranchType();

    /**
     * Get the GlobalStatus.
     *
     * @param branchType The BranchType of ResourceManager.
     * @param xid The xid of transaction.
     * @return The GlobalStatus of transaction.
     */
    GlobalStatus getGlobalStatus(BranchType branchType, String xid);
}
