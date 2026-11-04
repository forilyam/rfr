package io.student.rangiffler.data.db;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.ValidateResult;
import org.flywaydb.core.api.pattern.ValidatePattern;

public final class Migrations {
    private final Databases database;
    private final Path location;

    public Migrations(Databases database, String location) {
        this(database, Path.of(location));
    }

    public Migrations(Databases database, Path location) {
        this.database = database;
        this.location = location;
    }

    public Optional<String> mismatch() {
        ValidateResult result = flyway().validateWithResult();
        return result.validationSuccessful
                ? Optional.empty()
                : Optional.of(database.schema() + ": " + String.join("; ", problems(result)));
    }

    public String currentVersion() {
        try {
            var current = flyway().info().current();
            return current == null || current.getVersion() == null
                    ? "0"
                    : current.getVersion().toString();
        } catch (RuntimeException exception) {
            return "unavailable";
        }
    }

    private Flyway flyway() {
        if (!Files.isDirectory(location)) {
            throw new IllegalStateException(
                    "Migrations directory " + location.toAbsolutePath() + " of schema " + database.schema() + " does not exist");
        }
        return Flyway.configure()
                .dataSource(database.dataSource())
                .schemas(database.schema())
                .locations("filesystem:" + location.toAbsolutePath())
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .ignoreMigrationPatterns(new ValidatePattern[0])
                .load();
    }

    private List<String> problems(ValidateResult result) {
        return Stream.concat(
                        result.invalidMigrations.stream().map(migration -> migration.errorDetails),
                        Stream.ofNullable(result.errorDetails))
                .filter(Objects::nonNull)
                .map(details -> details.errorMessage.lines().findFirst().orElse(String.valueOf(details.errorCode)))
                .distinct()
                .toList();
    }
}