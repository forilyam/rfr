package io.student.rangiffler.jupiter.probe;

import io.student.rangiffler.data.TestUser;
import io.student.rangiffler.jupiter.annotation.StaticUser;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.TestReporter;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.reporting.ReportEntry;
import org.junit.platform.testkit.engine.EngineExecutionResults;
import org.junit.platform.testkit.engine.EngineTestKit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

@Timeout(20)
@Execution(ExecutionMode.SAME_THREAD)
final class UsersQueueExtensionTest {
    @Test
    void fourSequentialTestsCanUseThreeStaticUsers() {
        EngineExecutionResults execution = EngineTestKit.engine("junit-jupiter")
                .selectors(selectClass(SequentialCases.class))
                .configurationParameter("junit.jupiter.execution.timeout.default", "7s").execute();
        assertThat("Four sequential tests exhaust the pool or dont reuse its three users", List.of(
                        outcome(execution), users(execution, "body").size(),
                        users(execution, "body").stream().distinct().count()),
                is(List.of(List.of(4L, 0L, 0L), 4, 3L)));
    }

    @Test
    void failedTestDoesntKeepItsStaticUser() {
        EngineExecutionResults execution = EngineTestKit.engine("junit-jupiter")
                .selectors(selectClass(FailureCases.class))
                .configurationParameter("junit.jupiter.execution.timeout.default", "7s").execute();
        assertThat("A failed test keeps its user or the next test cannot borrow the full pool", List.of(
                        outcome(execution), failures(execution), users(execution, "failed").size(),
                        users(execution, "borrowed").size(), users(execution, "borrowed").stream().distinct().count(),
                        users(execution, "borrowed").containsAll(users(execution, "failed"))),
                is(List.of(List.of(1L, 1L, 0L), List.of("Static user scenario failed"), 1, 3, 3L, true)));
    }

    @Test
    void concurrentTestsCannotShareStaticUsers() {
        EngineExecutionResults execution = EngineTestKit.engine("junit-jupiter")
                .selectors(selectClass(ConcurrentCases.class))
                .configurationParameter("junit.jupiter.execution.parallel.enabled", "true")
                .configurationParameter("junit.jupiter.execution.parallel.mode.default", "concurrent")
                .configurationParameter("junit.jupiter.execution.parallel.config.strategy", "fixed")
                .configurationParameter("junit.jupiter.execution.parallel.config.fixed.parallelism", "3")
                .configurationParameter("junit.jupiter.execution.timeout.default", "7s").execute();
        assertThat("Concurrent tests share a static user or cannot run together", List.of(
                        outcome(execution), users(execution, "body").size(),
                        users(execution, "body").stream().distinct().count()),
                is(List.of(List.of(3L, 0L, 0L), 3, 3L)));
    }

    @Test
    void exhaustedPoolTimesOutAndDoesntLoseBorrowedUsers() {
        EngineExecutionResults execution = EngineTestKit.engine("junit-jupiter")
                .selectors(selectClass(ExhaustionCases.class))
                .configurationParameter("junit.jupiter.execution.timeout.default", "7s").execute();
        assertThat("Pool exhaustion hangs, loses users or returns an unborrowed user", List.of(
                        outcome(execution), failures(execution).stream().anyMatch(message ->
                                message.contains("Timed out") && message.contains("static user")),
                        users(execution, "borrowed").size(), users(execution, "borrowed").stream().distinct().count()),
                is(List.of(List.of(1L, 1L, 0L), true, 3, 3L)));
    }

    private List<Long> outcome(EngineExecutionResults execution) {
        return List.of(execution.testEvents().succeeded().count(), execution.testEvents().failed().count(),
                execution.containerEvents().failed().count());
    }

    private List<String> users(EngineExecutionResults execution, String phase) {
        return execution.testEvents().reportingEntryPublished()
                .map(event -> event.getRequiredPayload(ReportEntry.class).getKeyValuePairs())
                .filter(entry -> phase.equals(entry.get("phase"))).map(entry -> entry.get("username")).toList();
    }

    private List<String> failures(EngineExecutionResults execution) {
        return execution.testEvents().failed().map(event -> event.getRequiredPayload(TestExecutionResult.class)
                .getThrowable().orElseThrow().getMessage()).toList();
    }

    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    static final class SequentialCases {
        @Test
        @Order(1)
        void first(TestReporter reporter, @StaticUser TestUser user) {
            reporter.publishEntry(Map.of("phase", "body", "username", user.username()));
        }

        @Test
        @Order(2)
        void second(TestReporter reporter, @StaticUser TestUser user) {
            reporter.publishEntry(Map.of("phase", "body", "username", user.username()));
        }

        @Test
        @Order(3)
        void third(TestReporter reporter, @StaticUser TestUser user) {
            reporter.publishEntry(Map.of("phase", "body", "username", user.username()));
        }

        @Test
        @Order(4)
        void fourth(TestReporter reporter, @StaticUser TestUser user) {
            reporter.publishEntry(Map.of("phase", "body", "username", user.username()));
        }
    }

    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    static final class FailureCases {
        @Test
        @Order(1)
        void failing(TestReporter reporter, @StaticUser TestUser user) {
            reporter.publishEntry(Map.of("phase", "failed", "username", user.username()));
            throw new IllegalStateException("Static user scenario failed");
        }

        @Test
        @Order(2)
        void borrowed(TestReporter reporter, @StaticUser TestUser first,
                      @StaticUser TestUser second, @StaticUser TestUser third) {
            reporter.publishEntry(Map.of("phase", "borrowed", "username", first.username()));
            reporter.publishEntry(Map.of("phase", "borrowed", "username", second.username()));
            reporter.publishEntry(Map.of("phase", "borrowed", "username", third.username()));
        }
    }

    @ExtendWith(ParallelBarrier.class)
    static final class ConcurrentCases {
        @Test
        void first(TestReporter reporter, @StaticUser TestUser user) {
            reporter.publishEntry(Map.of("phase", "body", "username", user.username()));
        }

        @Test
        void second(TestReporter reporter, @StaticUser TestUser user) {
            reporter.publishEntry(Map.of("phase", "body", "username", user.username()));
        }

        @Test
        void third(TestReporter reporter, @StaticUser TestUser user) {
            reporter.publishEntry(Map.of("phase", "body", "username", user.username()));
        }
    }

    private static final class ParallelBarrier implements BeforeTestExecutionCallback {
        @Override
        public void beforeTestExecution(ExtensionContext context) throws Exception {
            context.getRoot().getStore(ExtensionContext.Namespace.create(ParallelBarrier.class))
                    .computeIfAbsent("barrier", key -> new CyclicBarrier(3), CyclicBarrier.class)
                    .await(3, TimeUnit.SECONDS);
        }
    }

    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    static final class ExhaustionCases {
        @Test
        @Order(1)
        void exhausted(@StaticUser TestUser first, @StaticUser TestUser second,
                       @StaticUser TestUser third, @StaticUser TestUser fourth) {
            throw new AssertionError("An exhausted pool reached the test body");
        }

        @Test
        @Order(2)
        void borrowed(TestReporter reporter, @StaticUser TestUser first,
                      @StaticUser TestUser second, @StaticUser TestUser third) {
            reporter.publishEntry(Map.of("phase", "borrowed", "username", first.username()));
            reporter.publishEntry(Map.of("phase", "borrowed", "username", second.username()));
            reporter.publishEntry(Map.of("phase", "borrowed", "username", third.username()));
        }
    }
}
