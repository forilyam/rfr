package io.student.rangiffler.jupiter.probe;

import io.student.rangiffler.data.TestUser;
import io.student.rangiffler.data.UserFixture;
import io.student.rangiffler.data.db.ApiProfile;
import io.student.rangiffler.data.db.AuthAccount;
import io.student.rangiffler.data.db.Friendship;
import io.student.rangiffler.jupiter.annotation.ProbeTarget;
import io.student.rangiffler.jupiter.extension.ProbeTargetCondition;
import io.student.rangiffler.jupiter.annotation.User;
import io.student.rangiffler.jupiter.extension.UserExtension;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestReporter;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.reporting.ReportEntry;
import org.junit.platform.testkit.engine.EngineExecutionResults;
import org.junit.platform.testkit.engine.EngineTestKit;

import static io.student.rangiffler.data.db.FriendshipStatus.PENDING;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectMethod;

@Timeout(30)
final class UserExtensionTest {
    @Test
    void usersExistDuringBodyAndDontRemainAfterBody() {
        EngineExecutionResults execution = execute(selectMethod(Cases.class, "prepared", TestReporter.class, TestUser.class));
        assertThat("User preparation or cleanup is outside the method lifecycle", outcome(execution),
                is(List.of(1L, 0L, 1L, true)));
    }

    @Test
    void annotationValuesReachFixture() {
        EngineExecutionResults execution = execute(selectMethod(Cases.class, "requested", TestReporter.class, TestUser.class));
        assertThat("Annotation values dont reach the fixture or partners remain after cleanup", outcome(execution),
                is(List.of(1L, 0L, 16L, true)));
    }

    @Test
    void outcomeInvitationExistsDuringBodyAndDoesntRemainAfterBody() {
        EngineExecutionResults execution = execute(selectClass(OutcomeInvitationCase.class));
        assertThat("An outgoing invitation is missing during the test or survives fixture cleanup", List.of(
                        outcome(execution), reports(execution).stream().map(entry ->
                                new Friendship(entry.get("username"), entry.get("invitee"), PENDING).exist()).toList()),
                is(List.of(List.of(1L, 0L, 2L, true), List.of(false))));
    }

    @Test
    void multipleParametersReceiveIndependentUsers() {
        EngineExecutionResults execution = execute(selectMethod(Cases.class, "multiple", TestReporter.class,
                TestUser.class, TestUser.class));
        assertThat("Multiple parameters share a user or leave records after cleanup", outcome(execution),
                is(List.of(1L, 0L, 2L, true)));
    }

    @Test
    void methodStoresDontReuseUsers() {
        EngineExecutionResults execution = execute(selectClass(IsolatedCases.class));
        assertThat("Method stores reuse a user or retain records", outcome(execution), is(List.of(2L, 0L, 2L, true)));
    }

    @Test
    void concurrentMethodStoresDontShareFixtures() {
        EngineExecutionResults execution = EngineTestKit.engine("junit-jupiter").selectors(selectClass(IsolatedCases.class))
                .configurationParameter(ProbeTargetCondition.PARAMETER, "true")
                .configurationParameter("junit.jupiter.extensions.autodetection.enabled", "false")
                .configurationParameter("junit.jupiter.execution.parallel.enabled", "true")
                .configurationParameter("junit.jupiter.execution.parallel.mode.default", "concurrent")
                .configurationParameter("junit.jupiter.execution.parallel.config.strategy", "fixed")
                .configurationParameter("junit.jupiter.execution.parallel.config.fixed.parallelism", "2")
                .configurationParameter("junit.jupiter.execution.timeout.default", "10s").execute();
        assertThat("Concurrent method stores share a user or retain records", outcome(execution), is(List.of(2L, 0L, 2L, true)));
    }

    @Test
    void bodyFailureDoesntPreventCleanup() {
        EngineExecutionResults execution = execute(selectMethod(Cases.class, "failing", TestReporter.class, TestUser.class));
        assertThat("A failed test body prevents fixture cleanup", outcome(execution), is(List.of(0L, 1L, 1L, true)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"negativeFriends", "negativeIncome", "negativeOutcome"})
    void invalidAnnotationCannotReachBody(String method) {
        EngineExecutionResults execution = execute(selectMethod(Cases.class, method, TestUser.class));
        assertThat("An invalid annotation reaches the body or throws the wrong exception", execution.testEvents().failed()
                .map(event -> event.getRequiredPayload(TestExecutionResult.class).getThrowable().orElseThrow().getClass()).toList(),
                is(List.of(IllegalArgumentException.class)));
    }

    @Test
    void invalidSecondAnnotationDoesntLeaveFirstFixture() {
        EngineExecutionResults execution = execute(selectClass(InvalidSecondCase.class));
        assertThat("An invalid second annotation leaves the first fixture unclosed", outcome(execution),
                is(List.of(0L, 1L, 1L, true)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"unannotated", "wrongType"})
    void unsupportedParameterCannotReachBody(String method) {
        EngineExecutionResults execution = execute(selectMethod(Cases.class, method,
                method.equals("wrongType") ? String.class : TestUser.class));
        assertThat("An unsupported parameter reaches the body", execution.testEvents().failed()
                .map(event -> event.getRequiredPayload(TestExecutionResult.class).getThrowable().orElseThrow().getMessage()
                        .contains("No ParameterResolver registered")).toList(), is(List.of(true)));
    }

    @Test
    void lifecycleMethodCannotResolveUser() {
        EngineExecutionResults execution = execute(selectClass(LifecycleCase.class));
        assertThat("A lifecycle method receives a test method fixture", execution.testEvents().failed()
                .map(event -> event.getRequiredPayload(TestExecutionResult.class).getThrowable().orElseThrow().getMessage()
                        .contains("No ParameterResolver registered")).toList(), is(List.of(true)));
    }

    @Test
    void testWithoutUserDoesntRequireFixture() {
        EngineExecutionResults execution = execute(selectMethod(Cases.class, "withoutUser"));
        assertThat("A test without user parameters cannot run", outcome(execution), is(List.of(1L, 0L, 0L, true)));
    }

    private EngineExecutionResults execute(DiscoverySelector selector) {
        return EngineTestKit.engine("junit-jupiter").selectors(selector)
                .configurationParameter(ProbeTargetCondition.PARAMETER, "true")
                .configurationParameter("junit.jupiter.extensions.autodetection.enabled", "false")
                .configurationParameter("junit.jupiter.execution.parallel.enabled", "false")
                .configurationParameter("junit.jupiter.execution.timeout.default", "10s").execute();
    }

    private List<Object> outcome(EngineExecutionResults execution) {
        List<String> users = reports(execution).stream().flatMap(entry -> entry.values().stream()).toList();
        return List.of(execution.testEvents().succeeded().count(), execution.testEvents().failed().count(),
                users.stream().distinct().count(), users.stream().allMatch(username ->
                        !new ApiProfile(username).exist() && !new AuthAccount(new TestUser(username, "unused-83")).exist()));
    }

    private List<Map<String, String>> reports(EngineExecutionResults execution) {
        return execution.testEvents().reportingEntryPublished()
                .map(event -> event.getRequiredPayload(ReportEntry.class).getKeyValuePairs()).toList();
    }

    @ProbeTarget
    static final class Cases {
        @Test
        void prepared(TestReporter reporter, @User TestUser user) {
            reporter.publishEntry("username", user.username());
            assertThat("The user account or profile is missing during the body",
                    List.of(new AuthAccount(user).exist(), new ApiProfile(user.username()).exist()), is(List.of(true, true)));
        }

        @Test
        void requested(TestReporter reporter, @User(friends = 3, incomeInvitations = 7, outcomeInvitations = 5) TestUser user) {
            Stream.concat(Stream.of(user), Stream.of(user.friends(), user.incomeInvitations(), user.outcomeInvitations())
                    .flatMap(List::stream)).forEach(partner -> reporter.publishEntry("username", partner.username()));
            assertThat("Annotation values dont produce the requested partners",
                    List.of(user.friends().size(), user.incomeInvitations().size(), user.outcomeInvitations().size()),
                    is(List.of(3, 7, 5)));
        }

        @Test
        void multiple(TestReporter reporter, @User TestUser left, @User TestUser right) {
            reporter.publishEntry("username", left.username());
            reporter.publishEntry("username", right.username());
        }

        @Test
        void failing(TestReporter reporter, @User TestUser user) {
            reporter.publishEntry("username", user.username());
            throw new IllegalStateException("Test body failed");
        }

        @Test
        void negativeFriends(@User(friends = -3) TestUser user) {
            throw new AssertionError("Invalid preparation reached the body");
        }

        @Test
        void negativeIncome(@User(incomeInvitations = -7) TestUser user) {
            throw new AssertionError("Invalid preparation reached the body");
        }

        @Test
        void negativeOutcome(@User(outcomeInvitations = -5) TestUser user) {
            throw new AssertionError("Invalid preparation reached the body");
        }

        @Test
        void unannotated(TestUser user) {
            throw new AssertionError("An unannotated parameter reached the body");
        }

        @Test
        void wrongType(@User String user) {
            throw new AssertionError("A parameter with the wrong type reached the body");
        }

        @Test
        void withoutUser() {
        }
    }

    @ProbeTarget
    static final class OutcomeInvitationCase {
        @Test
        void scenario(TestReporter reporter, @User(outcomeInvitations = 1) TestUser user) {
            String invitee = user.outcomeInvitations().getFirst().username();
            reporter.publishEntry(Map.of("username", user.username(), "invitee", invitee));
            assertThat("An outgoing invitation or invitee profile is missing during the test", List.of(
                            new Friendship(user.username(), invitee, PENDING).exist(), new ApiProfile(invitee).exist()),
                    is(List.of(true, true)));
        }
    }

    @ExtendWith(ParallelBarrier.class)
    @ProbeTarget
    static final class IsolatedCases {
        @Test
        void first(TestReporter reporter, @User TestUser user) {
            reporter.publishEntry("username", user.username());
        }

        @Test
        void second(TestReporter reporter, @User TestUser user) {
            reporter.publishEntry("username", user.username());
        }
    }

    private static final class ParallelBarrier implements BeforeTestExecutionCallback {
        @Override
        public void beforeTestExecution(ExtensionContext context) throws Exception {
            if (context.getConfigurationParameter("junit.jupiter.execution.parallel.enabled", Boolean::parseBoolean).orElse(false)) {
                context.getRoot().getStore(ExtensionContext.Namespace.create(ParallelBarrier.class))
                        .computeIfAbsent("barrier", key -> new CyclicBarrier(2), CyclicBarrier.class).await(5, TimeUnit.SECONDS);
            }
        }
    }

    @ExtendWith(Observation.class)
    @ProbeTarget
    static final class InvalidSecondCase {
        @Test
        void scenario(@User TestUser first, @User(incomeInvitations = -5) TestUser second) {
            throw new AssertionError("Invalid preparation reached the body");
        }
    }

    private static final class Observation implements AfterEachCallback {
        @Override
        public void afterEach(ExtensionContext context) {
            UserFixture fixture = context.getStore(ExtensionContext.Namespace.create(UserExtension.class))
                    .get("user-0", UserFixture.class);
            context.publishReportEntry("username", Objects.requireNonNull(fixture).user().username());
        }
    }

    @ProbeTarget
    static final class LifecycleCase {
        @BeforeEach
        void prepare(@User TestUser user) {
            throw new AssertionError("A lifecycle parameter was resolved");
        }

        @Test
        void scenario() {
        }
    }
}
