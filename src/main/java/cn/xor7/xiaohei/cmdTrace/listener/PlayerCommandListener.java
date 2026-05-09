package cn.xor7.xiaohei.cmdTrace.listener;

import cn.xor7.xiaohei.cmdTrace.config.PluginConfig;
import cn.xor7.xiaohei.cmdTrace.model.CommandLogRecord;
import cn.xor7.xiaohei.cmdTrace.service.CommandLogService;
import com.velocitypowered.api.command.CommandResult;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.command.PostCommandInvocationEvent;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.proxy.Player;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;

public final class PlayerCommandListener {

    private final Logger logger;
    private final PluginConfig config;
    private final CommandLogService commandLogService;

    public PlayerCommandListener(
        Logger logger,
        PluginConfig config,
        CommandLogService commandLogService
    ) {
        this.logger = logger;
        this.config = config;
        this.commandLogService = commandLogService;
    }

    @Subscribe
    public void onPostCommandInvocation(PostCommandInvocationEvent event) {
        if (!(event.getCommandSource() instanceof Player player)) {
            return;
        }

        if (event.getResult() != CommandResult.FORWARDED) {
            return;
        }

        logPlayerCommand(player, "/" + event.getCommand());
    }

    @Subscribe
    public void onPlayerChat(PlayerChatEvent event) {
        String message = event.getMessage();
        if (!isTrackedChatCommand(message)) {
            return;
        }

        logPlayerCommand(event.getPlayer(), message);
    }

    private boolean isTrackedChatCommand(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }

        List<String> prefixes = config.customCmdPrefixes();
        for (String prefix : prefixes) {
            if (message.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private void logPlayerCommand(Player player, String rawCommand) {
        player
            .getCurrentServer()
            .ifPresent(serverConnection -> {
                CommandLogRecord record = new CommandLogRecord(
                    LocalDateTime.now(),
                    player.getUsername(),
                    player.getUniqueId().toString(),
                    rawCommand,
                    serverConnection.getServerInfo().getName()
                );
                commandLogService
                    .logAsync(record)
                    .exceptionally(exception -> {
                        logger.error("Failed to write command log", exception);
                        return null;
                    });
            });
    }
}
