#!/bin/bash

echo "MySQL数据备份和恢复工具"

case "$1" in
    "backup")
        echo "正在备份MySQL数据..."
        BACKUP_DIR="~/mysql_backup_$(date +%Y%m%d_%H%M%S)"
        mkdir -p "$BACKUP_DIR"
        
        # 备份数据目录
        cp -r ~/mysql_data "$BACKUP_DIR/"
        
        # 备份数据库结构
        docker exec mysql mysqldump -uroot -pmysql123 --all-databases > "$BACKUP_DIR/all_databases.sql"
        
        echo "✅ 备份完成，位置: $BACKUP_DIR"
        ;;
    "restore")
        if [ -z "$2" ]; then
            echo "❌ 请指定备份目录"
            echo "用法: $0 restore <backup_directory>"
            exit 1
        fi
        
        BACKUP_DIR="$2"
        echo "正在从 $BACKUP_DIR 恢复数据..."
        
        # 停止MySQL容器
        docker stop mysql
        
        # 恢复数据目录
        rm -rf ~/mysql_data
        cp -r "$BACKUP_DIR/mysql_data" ~/
        
        # 启动MySQL容器
        docker start mysql
        
        echo "✅ 数据恢复完成"
        ;;
    "status")
        echo "📊 MySQL容器状态："
        docker ps | grep mysql || echo "MySQL容器未运行"
        
        echo ""
        echo "📁 数据卷状态："
        echo "数据目录: ~/mysql_data"
        ls -la ~/mysql_data 2>/dev/null || echo "数据目录不存在"
        
        echo ""
        echo "💾 磁盘使用情况："
        du -sh ~/mysql_data 2>/dev/null || echo "无法获取磁盘使用情况"
        ;;
    *)
        echo "用法: $0 {backup|restore <backup_dir>|status}"
        echo ""
        echo "命令说明："
        echo "  backup   - 备份MySQL数据"
        echo "  restore  - 从备份恢复数据"
        echo "  status   - 查看容器和数据状态"
        ;;
esac 