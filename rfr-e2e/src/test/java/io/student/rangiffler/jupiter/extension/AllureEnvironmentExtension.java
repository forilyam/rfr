package io.student.rangiffler.jupiter.extension;

import io.student.rangiffler.config.TestConfig;
import io.student.rangiffler.data.db.Databases;
import io.student.rangiffler.data.db.Migrations;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;
import java.util.function.Supplier;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.extension.ExtensionContext;

@NullMarked
public final class AllureEnvironmentExtension implements SuiteExtension {
    private final Path directory;
    private final Supplier<Map<String, String>> environment;

    public AllureEnvironmentExtension() {
        this(TestConfig.allureResults(), () -> Map.of(
                "frontUrl", TestConfig.frontUrl(),
                "authUrl", TestConfig.authUrl(),
                "apiUrl", TestConfig.apiUrl(),
                "browser", TestConfig.browser(),
                "headless", Boolean.toString(TestConfig.headless()),
                "commit", TestConfig.commit(),
                "rangiffler-auth.version", new Migrations(Databases.AUTH, TestConfig.authMigrations()).currentVersion(),
                "rangiffler-api.version", new Migrations(Databases.API, TestConfig.apiMigrations()).currentVersion()
        ));
    }

    public AllureEnvironmentExtension(Path directory, Supplier<Map<String, String>> environment) {
        this.directory = directory;
        this.environment = environment;
    }

    @Override
    public void afterSuite(ExtensionContext context) {
        Properties properties = new Properties();
        properties.putAll(environment.get());
        try {
            Files.createDirectories(directory);
            try (OutputStream output = Files.newOutputStream(directory.resolve("environment.properties"))) {
                properties.store(output, "");
            }
        } catch (IOException exception) {
            throw new UncheckedIOException("Cannot write Allure environment to " + directory.toAbsolutePath(), exception);
        }
    }
}
