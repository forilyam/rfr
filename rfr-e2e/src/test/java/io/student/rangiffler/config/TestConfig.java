package io.student.rangiffler.config;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;

public final class TestConfig {
    private TestConfig() {
    }

    public static String frontUrl() {
        return value("frontUrl", "http://localhost:3001");
    }

    public static String authUrl() {
        return value("authUrl", "http://localhost:9001");
    }

    public static String apiUrl() {
        return value("apiUrl", "http://localhost:8081");
    }

    public static String dbUrl(String schema) {
        return value("dbUrl", "jdbc:mysql://localhost:3306") + "/" + schema + "?serverTimezone=UTC";
    }

    public static InetSocketAddress dbAddress() {
        URI address = URI.create(value("dbUrl", "jdbc:mysql://localhost:3306").replaceFirst("^jdbc:", ""));
        return InetSocketAddress.createUnresolved(
                Optional.ofNullable(address.getHost()).orElseThrow(() ->
                        new IllegalArgumentException("MySQL address has no host in rfr.dbUrl")),
                address.getPort() < 0 ? 3306 : address.getPort()
        );
    }

    public static boolean standCheck() {
        return Boolean.parseBoolean(value("standCheck", "true"));
    }

    public static Duration dbTimeout() {
        return Duration.ofSeconds(Long.parseLong(value("dbTimeout", "5")));
    }

    public static Duration standTimeout() {
        return Duration.ofSeconds(Long.parseLong(value("standTimeout", "18")));
    }

    public static String dbUser() {
        return value("dbUser", "root");
    }

    public static String dbPassword() {
        return value("dbPassword", "secret");
    }

    public static boolean migrationCheck() {
        return Boolean.parseBoolean(value("migrationCheck", "false"));
    }

    public static String authMigrations() {
        return value("authMigrations", "../rfr-auth/src/main/resources/db/migration/rangiffler-auth");
    }

    public static String apiMigrations() {
        return value("apiMigrations", "../rfr-api/src/main/resources/db/migration/rangiffler-api");
    }

    public static String browser() {
        return value("browser", "chromium");
    }

    public static boolean headless() {
        return Boolean.parseBoolean(value("headless", "true"));
    }

    public static Duration actionTimeout() {
        return Duration.ofSeconds(Long.parseLong(value("actionTimeout", "5")));
    }

    public static Duration navigationTimeout() {
        return Duration.ofSeconds(Long.parseLong(value("navigationTimeout", "30")));
    }

    public static int viewportWidth() {
        return Integer.parseInt(value("viewportWidth", "1920"));
    }

    public static int viewportHeight() {
        return Integer.parseInt(value("viewportHeight", "1080"));
    }

    public static String commit() {
        return value("commit", "unknown");
    }

    public static Path allureResults() {
        return Path.of(System.getProperty("allure.results.directory", "rfr-e2e/build/allure-results"));
    }

    public static Path traces() {
        return Path.of(value("traces", "build/traces"));
    }

    public static String testClasspath() {
        return value("testClasspath", System.getProperty("java.class.path"));
    }

    static String value(String key, String fallback) {
        String property = System.getProperty("rfr." + key);
        if (property != null && !property.isBlank()) {
            return property;
        }
        String variable = System.getenv("RFR_" + key.replaceAll("([A-Z])", "_$1").toUpperCase(Locale.ROOT));
        if (variable != null && !variable.isBlank()) {
            return variable;
        }
        return fallback;
    }
}
