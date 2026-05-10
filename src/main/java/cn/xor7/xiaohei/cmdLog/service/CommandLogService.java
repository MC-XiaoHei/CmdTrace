package cn.xor7.xiaohei.cmdLog.service;

import cn.xor7.xiaohei.cmdLog.config.PluginConfig;
import cn.xor7.xiaohei.cmdLog.database.DatabaseManager;
import cn.xor7.xiaohei.cmdLog.model.CommandLogEntry;
import cn.xor7.xiaohei.cmdLog.model.CommandLogRecord;
import cn.xor7.xiaohei.cmdLog.model.CommandSearchCriteria;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class CommandLogService {

    private final PluginConfig config;
    private final DatabaseManager databaseManager;

    public CommandLogService(
        PluginConfig config,
        DatabaseManager databaseManager
    ) {
        this.config = config;
        this.databaseManager = databaseManager;
    }

    public CompletableFuture<Void> logAsync(CommandLogRecord record) {
        return databaseManager.execute(repository -> {
            repository.insert(record);
            return null;
        });
    }

    public CompletableFuture<List<CommandLogEntry>> searchAsync(
        CommandSearchCriteria criteria
    ) {
        return databaseManager.execute(repository ->
            repository.search(criteria)
        );
    }

    public CompletableFuture<Integer> cleanupExpiredLogsAsync() {
        LocalDateTime expiredBefore = LocalDateTime.now().minusDays(
            config.retentionDays()
        );
        return databaseManager.execute(repository ->
            repository.deleteOlderThan(expiredBefore)
        );
    }
}
