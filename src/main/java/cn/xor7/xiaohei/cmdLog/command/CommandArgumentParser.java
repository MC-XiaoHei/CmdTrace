package cn.xor7.xiaohei.cmdLog.command;

import cn.xor7.xiaohei.cmdLog.config.PluginConfig;
import cn.xor7.xiaohei.cmdLog.model.CommandSearchCriteria;
import cn.xor7.xiaohei.cmdLog.model.MatchMode;
import cn.xor7.xiaohei.cmdLog.service.CommandException;
import cn.xor7.xiaohei.cmdLog.service.CommandMessages;
import cn.xor7.xiaohei.cmdLog.util.TimeParser;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public final class CommandArgumentParser {

    private static final String DEFAULT_COMMAND_PREFIX = "/";

    private final PluginConfig config;
    private final CommandMessages messages;

    public CommandArgumentParser(
        PluginConfig config,
        CommandMessages messages
    ) {
        this.config = config;
        this.messages = messages;
    }

    public ParseResult parse(CommandSource source, String[] args) {
        if (shouldShowHelp(args)) {
            return ParseResult.help();
        }

        CommandSearchCriteria criteria = new CommandSearchCriteria();
        criteria.setLimit(config.defaultLimit());

        int index = 0;
        while (index < args.length) {
            String argument = args[index];
            if (!argument.startsWith("--")) {
                throw new CommandException(
                    messages.buildUnknownOptionMessage(argument)
                );
            }

            index = parseOption(criteria, args, index, argument);
        }

        applyDefaults(source, criteria, args.length == 0);
        validateCriteria(criteria);
        return ParseResult.search(criteria);
    }

    private boolean shouldShowHelp(String[] args) {
        return (
            args.length == 1 &&
            ("help".equalsIgnoreCase(args[0]) ||
                "--help".equalsIgnoreCase(args[0]) ||
                "-h".equalsIgnoreCase(args[0]))
        );
    }

    private int parseOption(
        CommandSearchCriteria criteria,
        String[] args,
        int index,
        String argument
    ) {
        return switch (argument) {
            case "--server" -> parseServer(criteria, args, index);
            case "--player" -> parsePlayer(criteria, args, index);
            case "--uuid" -> parseUuid(criteria, args, index);
            case "--startswith" -> parseMode(
                criteria,
                args,
                index,
                MatchMode.STARTSWITH
            );
            case "--contains" -> parseMode(
                criteria,
                args,
                index,
                MatchMode.CONTAINS
            );
            case "--regex" -> parseMode(criteria, args, index, MatchMode.REGEX);
            case "--from" -> parseFrom(criteria, args, index);
            case "--to" -> parseTo(criteria, args, index);
            case "--limit" -> parseLimit(criteria, args, index);
            default -> throw new CommandException(
                messages.buildUnknownOptionMessage(argument)
            );
        };
    }

    private int parseServer(
        CommandSearchCriteria criteria,
        String[] args,
        int index
    ) {
        ParsedValue parsedValue = readSingleValue(args, index, "--server");
        criteria.setServer(parsedValue.value());
        return parsedValue.nextIndex();
    }

    private int parsePlayer(
        CommandSearchCriteria criteria,
        String[] args,
        int index
    ) {
        ParsedValue parsedValue = readSingleValue(args, index, "--player");
        criteria.setPlayerName(parsedValue.value());
        return parsedValue.nextIndex();
    }

    private int parseUuid(
        CommandSearchCriteria criteria,
        String[] args,
        int index
    ) {
        ParsedValue parsedValue = readSingleValue(args, index, "--uuid");
        criteria.setUuid(parsedValue.value());
        return parsedValue.nextIndex();
    }

    private int parseMode(
        CommandSearchCriteria criteria,
        String[] args,
        int index,
        MatchMode matchMode
    ) {
        if (criteria.getMatchMode() != null) {
            throw new CommandException(messages.buildDuplicateModeMessage());
        }
        if (matchMode == MatchMode.REGEX && !config.regexEnabled()) {
            throw new CommandException(messages.regexDisabled());
        }

        ParsedValue parsedValue = readMultiValue(
            args,
            index,
            optionName(matchMode)
        );
        criteria.setMatchMode(matchMode);
        criteria.setKeyword(parsedValue.value());
        if (matchMode == MatchMode.REGEX) {
            validateRegex(parsedValue.value());
        }
        if (matchMode == MatchMode.STARTSWITH) {
            criteria.setStartswithKeywords(
                resolveStartswithKeywords(parsedValue.value())
            );
        }
        return parsedValue.nextIndex();
    }

    private int parseFrom(
        CommandSearchCriteria criteria,
        String[] args,
        int index
    ) {
        ParsedValue parsedValue = readMultiValue(args, index, "--from");
        try {
            criteria.setFrom(TimeParser.parseFrom(parsedValue.value()));
        } catch (DateTimeParseException exception) {
            throw new CommandException(
                messages.invalidArgument("--from 时间格式无效")
            );
        }
        return parsedValue.nextIndex();
    }

    private int parseTo(
        CommandSearchCriteria criteria,
        String[] args,
        int index
    ) {
        ParsedValue parsedValue = readMultiValue(args, index, "--to");
        try {
            criteria.setTo(TimeParser.parseTo(parsedValue.value()));
        } catch (DateTimeParseException exception) {
            throw new CommandException(
                messages.invalidArgument("--to 时间格式无效")
            );
        }
        return parsedValue.nextIndex();
    }

    private int parseLimit(
        CommandSearchCriteria criteria,
        String[] args,
        int index
    ) {
        ParsedValue parsedValue = readSingleValue(args, index, "--limit");
        try {
            criteria.setLimit(Integer.parseInt(parsedValue.value()));
        } catch (NumberFormatException exception) {
            throw new CommandException(messages.buildInvalidLimitMessage());
        }
        return parsedValue.nextIndex();
    }

    private void applyDefaults(
        CommandSource source,
        CommandSearchCriteria criteria,
        boolean noArguments
    ) {
        if (!criteria.hasServer()) {
            if (source instanceof Player player) {
                player
                    .getCurrentServer()
                    .ifPresent(serverConnection ->
                        criteria.setServer(
                            serverConnection.getServerInfo().getName()
                        )
                    );
            }
        }

        if (noArguments) {
            criteria.setLimit(config.defaultRecentLimit());
        }
    }

    private void validateCriteria(CommandSearchCriteria criteria) {
        if (
            criteria.getFrom() != null &&
            criteria.getTo() != null &&
            criteria.getFrom().isAfter(criteria.getTo())
        ) {
            throw new CommandException(messages.buildInvalidRangeMessage());
        }

        if (
            criteria.getLimit() <= 0 || criteria.getLimit() > config.maxLimit()
        ) {
            throw new CommandException(messages.buildInvalidLimitMessage());
        }
    }

    private void validateRegex(String regex) {
        try {
            Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        } catch (PatternSyntaxException exception) {
            throw new CommandException(messages.invalidRegex());
        }
    }

    private List<String> resolveStartswithKeywords(String keyword) {
        if (keyword.startsWith(DEFAULT_COMMAND_PREFIX)) {
            return List.of(keyword);
        }

        return List.of(DEFAULT_COMMAND_PREFIX + keyword);
    }

    private ParsedValue readSingleValue(
        String[] args,
        int index,
        String option
    ) {
        if (index + 1 >= args.length) {
            throw new CommandException(
                messages.buildMissingValueMessage(option)
            );
        }
        return new ParsedValue(stripQuotes(args[index + 1]), index + 2);
    }

    private ParsedValue readMultiValue(
        String[] args,
        int index,
        String option
    ) {
        if (index + 1 >= args.length) {
            throw new CommandException(
                messages.buildMissingValueMessage(option)
            );
        }

        StringBuilder value = new StringBuilder();
        int nextIndex = index + 1;
        while (nextIndex < args.length) {
            String current = args[nextIndex];
            if (nextIndex > index + 1 && current.startsWith("--")) {
                break;
            }
            if (!value.isEmpty()) {
                value.append(' ');
            }
            value.append(current);
            nextIndex++;
        }

        return new ParsedValue(stripQuotes(value.toString()), nextIndex);
    }

    private String stripQuotes(String value) {
        if (value.length() >= 2) {
            if (value.startsWith("\"") && value.endsWith("\"")) {
                return value.substring(1, value.length() - 1);
            }
            if (value.startsWith("'") && value.endsWith("'")) {
                return value.substring(1, value.length() - 1);
            }
        }
        return value;
    }

    private String optionName(MatchMode matchMode) {
        return switch (matchMode) {
            case STARTSWITH -> "--startswith";
            case CONTAINS -> "--contains";
            case REGEX -> "--regex";
        };
    }

    public record ParseResult(
        boolean showHelp,
        CommandSearchCriteria criteria
    ) {
        public static ParseResult help() {
            return new ParseResult(true, null);
        }

        public static ParseResult search(CommandSearchCriteria criteria) {
            return new ParseResult(false, criteria);
        }
    }

    private record ParsedValue(String value, int nextIndex) {}
}
