package cn.xor7.xiaohei.cmdTrace.config;

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
        return new PluginConfig(
            dataDirectory.resolve(require(properties, "database.file")),
            parsePositiveInt(properties, "retention.days"),
            parsePositiveInt(properties, "query.default-limit"),
            parsePositiveInt(properties, "query.max-limit"),
            parsePositiveInt(properties, "query.default-recent-limit"),
            Boolean.parseBoolean(require(properties, "query.enable-regex")),
            parsePrefixes(properties, "query.custom-cmd-prefixes")
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
                throw new IOException("缺少默认配置文件 cmdlog.properties");
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
            () -> "缺少配置项: " + key
        ).trim();
    }

    private static int parsePositiveInt(Properties properties, String key) {
        String value = require(properties, key);
        int parsed = Integer.parseInt(value);
        if (parsed <= 0) {
            throw new IllegalArgumentException("配置项必须大于 0: " + key);
        }
        return parsed;
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
            throw new IllegalArgumentException("配置项不能为空: " + key);
        }
        return List.copyOf(prefixes);
    }
}
