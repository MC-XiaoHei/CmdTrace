package cn.xor7.xiaohei.cmdLog.service;

import cn.xor7.xiaohei.cmdLog.config.PluginConfig;
import cn.xor7.xiaohei.cmdLog.model.CommandLogEntry;
import cn.xor7.xiaohei.cmdLog.model.CommandSearchCriteria;
import cn.xor7.xiaohei.cmdLog.model.MatchMode;
import cn.xor7.xiaohei.cmdLog.util.TimeParser;
import java.time.LocalDate;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentBuilder;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;

public final class CommandMessages {

    private final PluginConfig config;

    public CommandMessages(PluginConfig config) {
        this.config = config;
    }

    public List<String> buildHelpLines() {
        return List.of(
            "§6CmdLog 查询帮助",
            "§7用法：/cmdlog [--server <服务器名>] [--player <玩家名>] [--uuid <UUID>]",
            "§7      [--startswith <前缀> | --contains <关键词> | --regex <正则>]",
            "§7      [--from <时间>] [--to <时间>] [--limit <数量>]",
            "§7--server <服务器名>  指定服务器；默认是当前服务器",
            "§7--player <玩家名>    只看这个玩家的命令",
            "§7--uuid <UUID>        按 UUID 精确查询",
            "§7--startswith <前缀>  查命令前缀；不写 '/' 时会自动补上",
            "§7--contains <关键词>  查包含指定内容的命令；不会自动补 '/'",
            "§7--regex <正则>       用正则查询；不会自动补 '/'",
            "§7--from <时间>        起始时间，如 2026-01-01 12:00:00",
            "§7--to <时间>          结束时间，如 2026-01-01 18:00:00",
            "§7--limit <数量>       限制返回条数",
            "§7示例：",
            "§7  /cmdlog §8(查看当前服务器最近记录)",
            "§7  /cmdlog --startswith home §8(实际按 /home 查询)",
            "§7  /cmdlog --contains \"/time \" §8(查包含 /time 的命令)",
            "§7  /cmdlog --regex \"/time .*\" §8(匹配 /time 后面的任意内容)"
        );
    }

    public String noPermission() {
        return "§c你没有权限使用此命令";
    }

    public String consoleMustSpecifyServer() {
        return "§c控制台执行时必须指定 --server <服务器名>";
    }

    public String regexDisabled() {
        return "§c当前未启用正则查询";
    }

    public String invalidArgument(String message) {
        return "§c参数错误：" + message;
    }

    public String invalidRegex() {
        return "§c正则表达式无效";
    }

    public String searchFailed() {
        return "§c查询失败，请稍后查看后台日志";
    }

    public String emptyResult(long elapsedMillis) {
        return "§e未找到符合条件的命令记录 §8(耗时 " + elapsedMillis + "ms)";
    }

    public Component buildSearchHeader(
        CommandSearchCriteria criteria,
        int resultSize,
        long elapsedMillis
    ) {
        String server = criteria.getServer();
        String player = criteria.hasPlayerName()
            ? criteria.getPlayerName()
            : null;

        Component conditionComponent = Component.text(
            buildModeLabel(criteria),
            NamedTextColor.GRAY
        ).hoverEvent(
            HoverEvent.showText(
                Component.text(
                    "耗时 " + elapsedMillis + "ms",
                    NamedTextColor.GRAY
                )
            )
        );

        TextComponent.Builder header = Component.text()
            .append(Component.text(resultSize + "条", NamedTextColor.GOLD))
            .append(Component.text(" · ", NamedTextColor.DARK_GRAY))
            .append(Component.text(server, NamedTextColor.AQUA))
            .append(Component.text(" · ", NamedTextColor.DARK_GRAY));

        if (player != null) {
            header
                .append(Component.text(player, NamedTextColor.YELLOW))
                .append(Component.text(" · ", NamedTextColor.DARK_GRAY));
        }

        return header.append(conditionComponent).build();
    }

    public Component formatDaySeparator(LocalDate date) {
        return Component.text(
            "--- " + TimeParser.formatDay(date) + " ---",
            NamedTextColor.GOLD
        );
    }

    public Component formatEntry(CommandLogEntry entry) {
        Component nameComponent = Component.text(
            entry.username(),
            NamedTextColor.WHITE
        );
        if (entry.uuid() != null && !entry.uuid().isBlank()) {
            nameComponent = nameComponent.hoverEvent(
                HoverEvent.showText(
                    Component.text("UUID: " + entry.uuid(), NamedTextColor.GRAY)
                )
            );
        }

        return Component.text()
            .append(
                Component.text(
                    TimeParser.formatTime(entry.timestamp()),
                    NamedTextColor.GRAY
                )
            )
            .append(Component.text(" ", NamedTextColor.GRAY))
            .append(nameComponent)
            .append(Component.text(": ", NamedTextColor.GRAY))
            .append(Component.text(entry.rawCommand(), NamedTextColor.WHITE))
            .build();
    }

    public String buildInvalidRangeMessage() {
        return invalidArgument("--from 不能晚于 --to");
    }

    public String buildInvalidLimitMessage() {
        return invalidArgument(
            "--limit 必须是 1 到 " + config.maxLimit() + " 之间的整数"
        );
    }

    public String buildMissingValueMessage(String option) {
        return invalidArgument(option + " 缺少值");
    }

    public String buildUnknownOptionMessage(String option) {
        return invalidArgument("未知参数：" + option);
    }

    public String buildDuplicateModeMessage() {
        return "§c只能指定一种匹配模式：--startswith、--contains、--regex";
    }

    public String buildUsageHint() {
        return "§7输入 /cmdlog help 查看使用说明";
    }

    private String buildModeLabel(CommandSearchCriteria criteria) {
        if (criteria.getMatchMode() == null) {
            return "最近记录";
        }
        if (criteria.getMatchMode() == MatchMode.STARTSWITH) {
            return "前缀为 " + criteria.getKeyword();
        }
        if (criteria.getMatchMode() == MatchMode.CONTAINS) {
            return "包含 " + criteria.getKeyword();
        }
        return "匹配正则 " + criteria.getKeyword();
    }
}
