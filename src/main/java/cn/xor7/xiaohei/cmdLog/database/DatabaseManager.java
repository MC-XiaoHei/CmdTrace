package cn.xor7.xiaohei.cmdLog.database;

import cn.xor7.xiaohei.cmdLog.config.PluginConfig;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public final class DatabaseManager {

    private final Connection connection;
    private final CommandLogRepository repository;
    private final ExecutorService executorService;

    public DatabaseManager(PluginConfig config) throws SQLException {
        loadSqliteDriver();
        this.connection = DriverManager.getConnection(
            "jdbc:sqlite:" + config.databaseFile()
        );
        this.repository = new CommandLogRepository(connection);
        this.executorService = Executors.newSingleThreadExecutor(
            new DatabaseThreadFactory()
        );
    }

    public void initialize() throws SQLException {
        repository.initialize();
    }

    public CommandLogRepository repository() {
        return repository;
    }

    public <T> CompletableFuture<T> execute(DatabaseTask<T> task) {
        return CompletableFuture.supplyAsync(
            () -> {
                try {
                    return task.run(repository);
                } catch (SQLException exception) {
                    throw new CompletionException(exception);
                }
            },
            executorService
        );
    }

    public void close() throws SQLException {
        Future<?> closeFuture = executorService.submit(() -> {
            try {
                connection.close();
            } catch (SQLException exception) {
                throw new CompletionException(exception);
            }
        });
        executorService.shutdown();
        try {
            closeFuture.get();
        } catch (CompletionException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof SQLException sqlException) {
                throw sqlException;
            }
            throw exception;
        } catch (Exception exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof SQLException sqlException) {
                throw sqlException;
            }
            throw new SQLException("关闭数据库连接失败", exception);
        }
    }

    private void loadSqliteDriver() {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException(
                "未找到 SQLite JDBC 驱动",
                exception
            );
        }
    }

    @FunctionalInterface
    public interface DatabaseTask<T> {
        T run(CommandLogRepository repository) throws SQLException;
    }

    private static final class DatabaseThreadFactory implements ThreadFactory {

        private static final AtomicInteger THREAD_COUNTER = new AtomicInteger(
            1
        );

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(
                runnable,
                "cmdlog-db-" + THREAD_COUNTER.getAndIncrement()
            );
            thread.setDaemon(true);
            return thread;
        }
    }
}
