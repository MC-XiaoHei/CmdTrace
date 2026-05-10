package cn.xor7.xiaohei.cmdLog.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Collectors;

public final class ConfigLoader {

    private ConfigLoader() {}

    public static PluginConfig load(Path dataDirectory) throws IOException {
        Files.createDirectories(dataDirectory);
        Path configFile = dataDirectory.resolve("cmdlog.properties");
        copyDefaultConfigIfMissing(configFile);
        Properties properties = loadProperties(configFile);

        Path databaseFile = dataDirectory.resolve(
            require(properties, "database.file")
        );
        int retentionDays = parsePositiveInt(properties, "retention.days");
        int defaultLimit = parsePositiveInt(properties, "query.default-limit");
        int maxLimit = parsePositiveInt(properties, "query.max-limit");
        int defaultRecentLimit = parsePositiveInt(
            properties,
            "query.default-recent-limit"
        );
        boolean regexEnabled = parseBoolean(properties, "query.enable-regex");
        List<String> customPrefixes = parsePrefixes(
            properties,
            "query.custom-cmd-prefixes"
        );

        validateLimitRelationship(
            "query.default-limit",
            defaultLimit,
            maxLimit
        );
        validateLimitRelationship(
            "query.default-recent-limit",
            defaultRecentLimit,
            maxLimit
        );

        return new PluginConfig(
            databaseFile,
            retentionDays,
            defaultLimit,
            maxLimit,
            defaultRecentLimit,
            regexEnabled,
            customPrefixes
        );
    }

    private static void copyDefaultConfigIfMissing(Path configFile)
        throws IOException {
        if (Files.exists(configFile)) {
            return;
        }

        try (
            InputStream inputStream =
                ConfigLoader.class.getClassLoader().getResourceAsStream(
                    "cmdlog.properties"
                )
        ) {
            if (inputStream == null) {
                throw new IOException(
                    "Missing default config file: cmdlog.properties"
                );
            }

            try (
                OutputStream outputStream = Files.newOutputStream(configFile)
            ) {
                inputStream.transferTo(outputStream);
            }
        }
    }

    private static Properties loadProperties(Path configFile)
        throws IOException {
        Properties properties = new Properties();
        try (InputStream inputStream = Files.newInputStream(configFile)) {
            properties.load(inputStream);
        }
        return properties;
    }

    private static String require(Properties properties, String key) {
        return Objects.requireNonNull(
            properties.getProperty(key),
            () -> "Missing required config key: " + key
        ).trim();
    }

    private static int parsePositiveInt(Properties properties, String key) {
        String value = require(properties, key);
        int parsed;
        try {
            parsed = Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                "Invalid config format: " +
                    key +
                    "='" +
                    value +
                    "' must be a positive integer",
                exception
            );
        }
        if (parsed <= 0) {
            throw new IllegalArgumentException(
                "Invalid config value: " +
                    key +
                    "='" +
                    value +
                    "' must be greater than 0"
            );
        }
        return parsed;
    }

    private static boolean parseBoolean(Properties properties, String key) {
        String value = require(properties, key);
        if ("true".equalsIgnoreCase(value)) {
            return true;
        }
        if ("false".equalsIgnoreCase(value)) {
            return false;
        }
        throw new IllegalArgumentException(
            "Invalid config format: " +
                key +
                "='" +
                value +
                "' must be either true or false"
        );
    }

    private static List<String> parsePrefixes(
        Properties properties,
        String key
    ) {
        String value = require(properties, key);
        List<String> prefixes = Arrays.stream(value.split(","))
            .map(String::trim)
            .filter(prefix -> !prefix.isEmpty())
            .distinct()
            .collect(Collectors.toList());
        if (prefixes.isEmpty()) {
            throw new IllegalArgumentException(
                "Invalid config value: " +
                    key +
                    "='" +
                    value +
                    "' must contain at least one non-empty prefix"
            );
        }
        return List.copyOf(prefixes);
    }

    private static void validateLimitRelationship(
        String key,
        int value,
        int maxLimit
    ) {
        if (value > maxLimit) {
            throw new IllegalArgumentException(
                "Invalid config value: " +
                    key +
                    "='" +
                    value +
                    "' must not be greater than query.max-limit='" +
                    maxLimit +
                    "'"
            );
        }
    }
}
