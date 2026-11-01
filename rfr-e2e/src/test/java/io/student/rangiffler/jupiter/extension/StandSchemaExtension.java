package io.student.rangiffler.jupiter.extension;

import io.student.rangiffler.config.TestConfig;
import io.student.rangiffler.data.db.Databases;
import io.student.rangiffler.data.db.Migrations;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.jspecify.annotations.NullMarked;
import org.junit.platform.launcher.LauncherSession;
import org.junit.platform.launcher.LauncherSessionListener;

@NullMarked
public final class StandSchemaExtension implements LauncherSessionListener {
    @Override
    public void launcherSessionOpened(LauncherSession session) {
        System.getLogger("io.student.rangiffler.jupiter.extension.StandSchemaExtension").log(System.Logger.Level.DEBUG,
                "Launcher session opened in JVM {0}, migration check enabled: {1}",
                Long.toString(ProcessHandle.current().pid()), TestConfig.migrationCheck());
        if (TestConfig.migrationCheck()) {
            List<String> mismatches = Stream.of(
                            new Migrations(Databases.AUTH, TestConfig.authMigrations()),
                            new Migrations(Databases.API, TestConfig.apiMigrations()))
                    .map(Migrations::mismatch)
                    .flatMap(Optional::stream)
                    .toList();
            if (!mismatches.isEmpty()) {
                throw new IllegalStateException(
                        "Local database schema does not match the backend migrations of the current branch, "
                                + "restart the backends with './gradlew runAll' so Flyway applies pending migrations, "
                                + "and if the mismatch remains recreate the database with "
                                + "'docker compose down -v && docker compose up -d --wait' and restart the backends again"
                                + System.lineSeparator()
                                + String.join(System.lineSeparator(), mismatches));
            }
        }
    }
}
