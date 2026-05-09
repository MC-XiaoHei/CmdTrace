package cn.xor7.xiaohei.cmdTrace.config;

import java.nio.file.Path;
import java.util.List;

public record PluginConfig(
    Path databaseFile,
    int retentionDays,
    int defaultLimit,
    int maxLimit,
    int defaultRecentLimit,
    boolean regexEnabled,
    List<String> customCmdPrefixes
) {}
