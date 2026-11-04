package io.student.rangiffler.jupiter.probe;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

import com.sun.net.httpserver.HttpServer;
import io.student.rangiffler.jupiter.annotation.ProbeTarget;
import io.student.rangiffler.jupiter.extension.StandReadinessExtension;
import io.student.rangiffler.stand.HttpReadiness;

import java.net.InetSocketAddress;
import java.net.URI;

import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestReporter;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.testkit.engine.EngineExecutionResults;
import org.junit.platform.testkit.engine.EngineTestKit;

@Timeout(15)
@NullMarked
final class StandReadinessExtensionTest {
    @Test
    void serviceCannotPreventTestExecutionAfterBecomingReady() throws Exception {
        AtomicInteger requests = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 7);
        long readiness = System.nanoTime() + Duration.ofSeconds(2).toNanos();
        server.createContext("/probe", exchange -> {
            requests.incrementAndGet();
            exchange.sendResponseHeaders(System.nanoTime() >= readiness ? 204 : 503, -1);
            exchange.close();
        });
        server.start();
        EngineExecutionResults execution;
        try {
            execution = execute(server, true, Duration.ofSeconds(7), selectClass(UiProbeTest.class));
        } finally {
            server.stop(0);
        }
        assertThat("A service becoming ready does not release the UI test",
                List.of(execution.testEvents().succeeded().count(), requests.get() >= 2), is(List.of(1L, true)));
    }

    @Test
    void unavailableServiceCannotReachTestBody() throws Exception {
        AtomicInteger requests = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 11);
        CountDownLatch release = new CountDownLatch(1);
        server.createContext("/probe", exchange -> {
            requests.incrementAndGet();
            try {
                if (!release.await(5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Probe request was not released before its timeout");
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            } finally {
                exchange.close();
            }
        });
        server.start();
        EngineExecutionResults execution;
        long start = System.nanoTime();
        try {
            execution = execute(server, true, Duration.ofMillis(719), selectClass(UiProbeTest.class));
        } finally {
            release.countDown();
            server.stop(0);
        }
        assertThat("An unavailable service reaches the test body or loses its diagnostic",
                List.of(execution.testEvents().started().count(), requests.get() > 0,
                        failures(execution).contains("rfr-gql-client"), failures(execution).contains("npm run dev"),
                        System.nanoTime() - start < Duration.ofSeconds(3).toNanos()),
                is(List.of(0L, true, true, true, true)));
    }

    @Test
    void classWithoutUiTagCannotContactStand() throws Exception {
        AtomicInteger requests = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 13);
        server.createContext("/probe", exchange -> {
            requests.incrementAndGet();
            exchange.sendResponseHeaders(503, -1);
            exchange.close();
        });
        server.start();
        EngineExecutionResults execution;
        try {
            execution = execute(server, true, Duration.ofMillis(431), selectClass(PlainProbeTest.class));
        } finally {
            server.stop(0);
        }
        assertThat("An untagged class contacts the stand or does not execute",
                List.of(execution.testEvents().succeeded().count(), requests.get()), is(List.of(1L, 0)));
    }

    @Test
    void disabledCheckCannotContactStand() throws Exception {
        AtomicInteger requests = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 17);
        server.createContext("/probe", exchange -> {
            requests.incrementAndGet();
            exchange.sendResponseHeaders(503, -1);
            exchange.close();
        });
        server.start();
        EngineExecutionResults execution;
        try {
            execution = execute(server, false, Duration.ofMillis(613), selectClass(UiProbeTest.class));
        } finally {
            server.stop(0);
        }
        assertThat("A disabled stand check sends requests or prevents test execution",
                List.of(execution.testEvents().succeeded().count(), requests.get()), is(List.of(1L, 0)));
    }

    @Test
    void untaggedClassCannotConsumePreparationForLaterUiClass() throws Exception {
        AtomicInteger requests = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 19);
        server.createContext("/probe", exchange -> {
            requests.incrementAndGet();
            exchange.sendResponseHeaders(503, -1);
            exchange.close();
        });
        server.start();
        EngineExecutionResults execution;
        try {
            execution = execute(server, true, Duration.ofMillis(587),
                    selectClass(PlainProbeTest.class), selectClass(UiProbeTest.class));
        } finally {
            server.stop(0);
        }
        assertThat("An earlier untagged class bypasses readiness for a later UI class",
                List.of(execution.testEvents().succeeded().count(), requests.get() > 0,
                        execution.containerEvents().failed().count()), is(List.of(1L, true, 1L)));
    }

    private EngineExecutionResults execute(
            HttpServer server, boolean check, Duration timeout, DiscoverySelector... selectors
    ) {
        return EngineTestKit.engine("junit-jupiter").selectors(selectors)
                .configurationParameter("junit.jupiter.extensions.autodetection.enabled", "false")
                .configurationParameter("junit.jupiter.execution.parallel.enabled", "false")
                .configurationParameter("junit.jupiter.testclass.order.default", "org.junit.jupiter.api.ClassOrderer$OrderAnnotation")
                .configurationParameter("junit.jupiter.execution.timeout.default", "9s")
                .configurationParameter("probe.url", "http://127.0.0.1:" + server.getAddress().getPort() + "/probe")
                .configurationParameter("probe.check", Boolean.toString(check))
                .configurationParameter("probe.timeout", Long.toString(timeout.toMillis()))
                .execute();
    }

    private String failures(EngineExecutionResults execution) {
        return execution.containerEvents().failed()
                .map(event -> event.getRequiredPayload(TestExecutionResult.class).getThrowable().orElseThrow().getMessage())
                .reduce("", (left, right) -> left + right);
    }

    @ProbeTarget
    @Tag("ui")
    @Order(2)
    @ExtendWith(ProbeReadiness.class)
    static final class UiProbeTest {
        @Test
        void scenario(TestReporter reporter) {
            reporter.publishEntry("phase", "body");
        }
    }

    @ProbeTarget
    @Order(1)
    @ExtendWith(ProbeReadiness.class)
    static final class PlainProbeTest {
        @Test
        void scenario(TestReporter reporter) {
            reporter.publishEntry("phase", "body");
        }
    }

    static final class ProbeReadiness implements BeforeAllCallback {
        @Override
        public void beforeAll(ExtensionContext context) {
            new StandReadinessExtension(
                    () -> Boolean.parseBoolean(context.getConfigurationParameter("probe.check").orElseThrow()),
                    () -> Duration.ofMillis(Long.parseLong(context.getConfigurationParameter("probe.timeout").orElseThrow())),
                    () -> List.of(new HttpReadiness(
                            "rfr-gql-client",
                            ClassicRequestBuilder.get(URI.create(context.getConfigurationParameter("probe.url").orElseThrow())).build(),
                            true,
                            "npm run dev"
                    ))
            ).beforeAll(context);
        }
    }
}
