package cn.xor7.xiaohei.cmdTrace.command;

import cn.xor7.xiaohei.cmdTrace.config.PluginConfig;
import cn.xor7.xiaohei.cmdTrace.model.CommandLogEntry;
import cn.xor7.xiaohei.cmdTrace.model.CommandSearchCriteria;
import cn.xor7.xiaohei.cmdTrace.service.CommandException;
import cn.xor7.xiaohei.cmdTrace.service.CommandLogService;
import cn.xor7.xiaohei.cmdTrace.service.CommandMessages;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.slf4j.Logger;

public final class CmdLogCommand implements SimpleCommand {

    private static final LegacyComponentSerializer LEGACY_SERIALIZER =
        LegacyComponentSerializer.legacySection();
    private static final String QUERY_PERMISSION = "cmdlog.query";

    private final PluginConfig config;
    private final CommandLogService commandLogService;
    private final CommandMessages messages;
    private final CommandArgumentParser argumentParser;
    private final Logger logger;

    public CmdLogCommand(
        PluginConfig config,
        CommandLogService commandLogService,
        CommandMessages messages,
        CommandArgumentParser argumentParser,
        Logger logger
    ) {
        this.config = config;
        this.commandLogService = commandLogService;
        this.messages = messages;
        this.argumentParser = argumentParser;
        this.logger = logger;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        if (!hasPermission(invocation)) {
            sendLine(source, messages.noPermission());
            return;
        }

        try {
            CommandArgumentParser.ParseResult parseResult =
                argumentParser.parse(source, invocation.arguments());
            if (parseResult.showHelp()) {
                sendLines(source, messages.buildHelpLines());
                return;
            }

            CommandSearchCriteria criteria = parseResult.criteria();
            if (!criteria.hasServer() && !(source instanceof Player)) {
                sendLine(source, messages.consoleMustSpecifyServer());
                sendLine(source, messages.buildUsageHint());
                return;
            }

            long startTime = System.nanoTime();
            commandLogService
                .searchAsync(criteria)
                .thenAccept(entries ->
                    handleSearchResult(source, criteria, entries, startTime)
                )
                .exceptionally(exception -> {
                    Throwable cause = unwrapCompletionException(exception);
                    sendLine(source, messages.searchFailed());
                    logger.error("Failed to search command logs", cause);
                    return null;
                });
        } catch (CommandException exception) {
            sendLine(source, exception.getMessage());
            sendLine(source, messages.buildUsageHint());
        }
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission(QUERY_PERMISSION);
    }

    @Override
    public CompletableFuture<List<String>> suggestAsync(Invocation invocation) {
        String[] arguments = invocation.arguments();
        if (arguments.length == 0) {
            return CompletableFuture.completedFuture(
                List.of("--server", "--player", "--startswith", "help")
            );
        }

        String lastArgument = arguments[arguments.length - 1];
        List<String> suggestions = new ArrayList<>();
        if (lastArgument.startsWith("--") || arguments.length == 1) {
            addIfMatches(suggestions, lastArgument, "--server");
            addIfMatches(suggestions, lastArgument, "--player");
            addIfMatches(suggestions, lastArgument, "--uuid");
            addIfMatches(suggestions, lastArgument, "--startswith");
            addIfMatches(suggestions, lastArgument, "--contains");
            if (config.regexEnabled()) {
                addIfMatches(suggestions, lastArgument, "--regex");
            }
            addIfMatches(suggestions, lastArgument, "--from");
            addIfMatches(suggestions, lastArgument, "--to");
            addIfMatches(suggestions, lastArgument, "--limit");
            addIfMatches(suggestions, lastArgument, "help");
        }
        return CompletableFuture.completedFuture(suggestions);
    }

    private void handleSearchResult(
        CommandSource source,
        CommandSearchCriteria criteria,
        List<CommandLogEntry> entries,
        long startTime
    ) {
        long elapsedMillis = (System.nanoTime() - startTime) / 1_000_000;
        if (entries.isEmpty()) {
            sendLine(source, messages.emptyResult(elapsedMillis));
            return;
        }

        sendComponent(
            source,
            messages.buildSearchHeader(criteria, entries.size(), elapsedMillis)
        );
        sendEntries(source, entries);
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

    private void sendEntries(
        CommandSource source,
        List<CommandLogEntry> entries
    ) {
        LocalDate currentDate = null;
        for (CommandLogEntry entry : entries) {
            LocalDate entryDate = entry.timestamp().toLocalDate();
            if (!entryDate.equals(currentDate)) {
                sendComponent(source, messages.formatDaySeparator(entryDate));
                currentDate = entryDate;
            }
            sendComponent(source, messages.formatEntry(entry));
        }
    }

    private void addIfMatches(
        List<String> suggestions,
        String input,
        String candidate
    ) {
        if (candidate.startsWith(input)) {
            suggestions.add(candidate);
        }
    }

    private void sendLines(CommandSource source, List<String> lines) {
        for (String line : lines) {
            sendLine(source, line);
        }
    }

    private void sendLine(CommandSource source, String line) {
        source.sendMessage(LEGACY_SERIALIZER.deserialize(line));
    }

    private void sendComponent(CommandSource source, Component component) {
        source.sendMessage(component);
    }
}
