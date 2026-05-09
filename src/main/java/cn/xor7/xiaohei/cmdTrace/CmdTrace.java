package cn.xor7.xiaohei.cmdTrace;

import cn.xor7.xiaohei.cmdTrace.command.CmdLogCommand;
import cn.xor7.xiaohei.cmdTrace.command.CommandArgumentParser;
import cn.xor7.xiaohei.cmdTrace.config.ConfigLoader;
import cn.xor7.xiaohei.cmdTrace.config.PluginConfig;
import cn.xor7.xiaohei.cmdTrace.database.DatabaseManager;
import cn.xor7.xiaohei.cmdTrace.listener.PlayerCommandListener;
import cn.xor7.xiaohei.cmdTrace.service.CommandLogService;
import cn.xor7.xiaohei.cmdTrace.service.CommandMessages;
import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;

@Plugin(id = "cmd-log", name = "CmdTrace", version = BuildConstants.VERSION)
public class CmdTrace {

    private final ProxyServer proxyServer;
    private final Logger logger;
    private final Path dataDirectory;
    private DatabaseManager databaseManager;

    @Inject
    public CmdTrace(
        ProxyServer proxyServer,
        Logger logger,
        @DataDirectory Path dataDirectory
    ) {
        this.proxyServer = proxyServer;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        try {
            PluginConfig config = ConfigLoader.load(dataDirectory);
            databaseManager = new DatabaseManager(config);
            databaseManager.initialize();

            CommandLogService commandLogService = new CommandLogService(
                config,
                databaseManager
            );
            CommandMessages commandMessages = new CommandMessages(config);
            CommandArgumentParser argumentParser = new CommandArgumentParser(
                config,
                commandMessages
            );
            registerCommand(
                config,
                commandLogService,
                commandMessages,
                argumentParser
            );
            proxyServer
                .getEventManager()
                .register(
                    this,
                    new PlayerCommandListener(logger, config, commandLogService)
                );
            scheduleCleanup(commandLogService);
            logger.info("CmdTrace enabled");
        } catch (IOException | SQLException | RuntimeException exception) {
            logger.error("Failed to start CmdTrace", exception);
            closeDatabaseQuietly();
            throw new IllegalStateException(
                "Failed to start CmdTrace",
                exception
            );
        }
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        closeDatabaseQuietly();
    }

    private void registerCommand(
        PluginConfig config,
        CommandLogService commandLogService,
        CommandMessages commandMessages,
        CommandArgumentParser argumentParser
    ) {
        CommandManager commandManager = proxyServer.getCommandManager();
        CommandMeta commandMeta = commandManager
            .metaBuilder("cmdlog")
            .plugin(this)
            .build();
        commandManager.register(
            commandMeta,
            new CmdLogCommand(
                config,
                commandLogService,
                commandMessages,
                argumentParser,
                logger
            )
        );
    }

    private void scheduleCleanup(CommandLogService commandLogService) {
        runCleanup(commandLogService, "startup");
        proxyServer
            .getScheduler()
            .buildTask(this, () -> runCleanup(commandLogService, "daily"))
            .repeat(1, TimeUnit.DAYS)
            .schedule();
    }

    private void runCleanup(
        CommandLogService commandLogService,
        String triggerSource
    ) {
        commandLogService
            .cleanupExpiredLogsAsync()
            .thenAccept(deletedCount ->
                logger.info(
                    "CmdTrace cleanup completed (trigger: {}), deleted {} expired records",
                    triggerSource,
                    deletedCount
                )
            )
            .exceptionally(exception -> {
                logger.error(
                    "CmdTrace cleanup failed (trigger: {})",
                    triggerSource,
                    unwrapCompletionException(exception)
                );
                return null;
            });
    }

    private Throwable unwrapCompletionException(Throwable throwable) {
        if (
            throwable instanceof CompletionException &&
            throwable.getCause() != null
        ) {
            return throwable.getCause();
        }
        return throwable;
    }

    private void closeDatabaseQuietly() {
        if (databaseManager == null) {
            return;
        }

        try {
            databaseManager.close();
        } catch (SQLException exception) {
            logger.error("Failed to close CmdTrace database", exception);
        } finally {
            databaseManager = null;
        }
    }
}
