package cn.xor7.xiaohei.cmdLog.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public final class TimeParser {

    private static final DateTimeFormatter DATE_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DISPLAY_TIME_FORMATTER =
        DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter DISPLAY_DAY_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private TimeParser() {}

    public static LocalDateTime parseFrom(String input) {
        return parse(input, true);
    }

    public static LocalDateTime parseTo(String input) {
        return parse(input, false);
    }

    public static String formatTime(LocalDateTime time) {
        return DISPLAY_TIME_FORMATTER.format(time);
    }

    public static String formatDay(LocalDate date) {
        return DISPLAY_DAY_FORMATTER.format(date);
    }

    private static LocalDateTime parse(String input, boolean startOfDay) {
        try {
            return LocalDateTime.parse(input, DATE_TIME_FORMATTER);
        } catch (DateTimeParseException ignored) {}

        LocalDate date = LocalDate.parse(input, DATE_FORMATTER);
        if (startOfDay) {
            return date.atStartOfDay();
        }
        return date.atTime(23, 59, 59);
    }
}
