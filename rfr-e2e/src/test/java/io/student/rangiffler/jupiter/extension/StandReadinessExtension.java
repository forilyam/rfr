package io.student.rangiffler.jupiter.extension;

import io.student.rangiffler.config.TestConfig;
import io.student.rangiffler.stand.HttpReadiness;
import io.student.rangiffler.stand.SocketReadiness;
import io.student.rangiffler.stand.StandService;

import java.net.URI;

import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.extension.ExtensionContext;

@NullMarked
public final class StandReadinessExtension implements SuiteExtension {
    private final BooleanSupplier check;
    private final Supplier<Duration> timeout;
    private final Supplier<List<StandService>> services;

    public StandReadinessExtension() {
        this(
                TestConfig::standCheck,
                TestConfig::standTimeout,
                () -> List.of(
                        new SocketReadiness("MySQL", TestConfig.dbAddress(), "docker compose up -d --wait"),
                        new HttpReadiness(
                                "rfr-auth",
                                ClassicRequestBuilder.get(URI.create(TestConfig.authUrl()).resolve("/.well-known/openid-configuration"))
                                        .build(),
                                true,
                                "./gradlew runAll"
                        ),
                        new HttpReadiness(
                                "rfr-api",
                                ClassicRequestBuilder.post(URI.create(TestConfig.apiUrl()).resolve("/graphql"))
                                        .setEntity("{\"query\":\"{__typename}\"}", ContentType.APPLICATION_JSON).build(),
                                false,
                                "./gradlew runAll"
                        ),
                        new HttpReadiness(
                                "rfr-gql-client",
                                ClassicRequestBuilder.get(URI.create(TestConfig.frontUrl())).build(),
                                true,
                                "npm run dev from rfr-gql-client"
                        )
                )
        );
    }

    public StandReadinessExtension(
            BooleanSupplier check,
            Supplier<Duration> timeout,
            Supplier<List<StandService>> services
    ) {
        this.check = check;
        this.timeout = timeout;
        this.services = services;
    }

    @Override
    public boolean supports(ExtensionContext context) {
        return check.getAsBoolean() && context.getTags().contains("ui");
    }

    @Override
    public void beforeSuite(ExtensionContext context) {
        Duration limit = timeout.get();
        if (limit.isZero() || limit.isNegative()) {
            throw new IllegalArgumentException("Stand timeout must be positive");
        }
        System.getLogger("io.student.rangiffler.stand").log(System.Logger.Level.DEBUG,
                "Waiting for stand readiness, timeout {0} ms", limit.toMillis());
        long deadline = System.nanoTime() + limit.toNanos();
        List<StandService> pending = new ArrayList<>(services.get());
        while (!pending.isEmpty()) {
            Iterator<StandService> iterator = pending.iterator();
            while (iterator.hasNext()) {
                long remaining = deadline - System.nanoTime();
                if (remaining <= 0) {
                    throw failure(pending, limit);
                }
                if (iterator.next().readiness(Duration.ofNanos(Math.min(remaining, Duration.ofSeconds(5).toNanos())))) {
                    iterator.remove();
                }
            }
            if (!pending.isEmpty()) {
                long remaining = deadline - System.nanoTime();
                if (remaining <= 0) {
                    throw failure(pending, limit);
                }
                try {
                    TimeUnit.NANOSECONDS.sleep(Math.min(remaining, Duration.ofSeconds(2).toNanos()));
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Interrupted while waiting for stand readiness", exception);
                }
            }
        }
        System.getLogger("io.student.rangiffler.stand").log(System.Logger.Level.DEBUG, "Stand is ready");
    }

    private IllegalStateException failure(List<StandService> pending, Duration timeout) {
        return new IllegalStateException("Stand did not become ready within " + timeout.toMillis()
                + " ms; unavailable services: " + pending.stream()
                .map(service -> service.name() + " (run " + service.command() + ")")
                .collect(Collectors.joining(", ")));
    }
}
