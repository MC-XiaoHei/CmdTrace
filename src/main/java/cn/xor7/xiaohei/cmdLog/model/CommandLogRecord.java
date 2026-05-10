package cn.xor7.xiaohei.cmdLog.model;

import java.time.LocalDateTime;

public record CommandLogRecord(
    LocalDateTime timestamp,
    String username,
    String uuid,
    String rawCommand,
    String server
) {}
