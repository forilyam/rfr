package io.student.rangiffler.jupiter.probe;

import io.student.rangiffler.data.TestUser;
import io.student.rangiffler.data.UserFixture;
import io.student.rangiffler.data.UserRequest;
import io.student.rangiffler.data.db.ApiProfile;
import io.student.rangiffler.data.db.Friendship;
import io.student.rangiffler.jupiter.annotation.User;
import io.student.rangiffler.jupiter.extension.UserExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestReporter;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.RegisterExtension;
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static io.student.rangiffler.data.db.FriendshipStatus.PENDING;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectMethod;

@Timeout(20)
final class UserExtensionTest {
    @Test
    void userIsCreatedBeforeBodyAndClosedAfterBody() {
        EngineExecutionResults execution = execute(selectMethod(Cases.class, "prepared", TestReporter.class, TestUser.class));
        assertThat("User preparation or cleanup is outside the method lifecycle", outcome(execution),
                is(List.of(1L, 0L, 0L, List.of("create", "resolve", "body", "close"))));
    }

    @Test
    void annotationValuesReachFixtureFactory() {
        EngineExecutionResults execution = execute(selectMethod(Cases.class, "requested", TestReporter.class, TestUser.class));
        assertThat("Annotation values dont reach the fixture factory", reports(execution).stream()
                .filter(entry -> entry.get("phase").equals("create"))
                .map(entry -> List.of(entry.get("friends"), entry.get("income"), entry.get("outcome"))).toList(),
                is(List.of(List.of("3", "7", "5"))));
    }

    @Test
    void outcomeInvitationExistsDuringBodyAndDoesntRemainAfterBody() {
        EngineExecutionResults execution = execute(selectClass(OutcomeInvitationCase.class));
        assertThat("An outgoing invitation is missing during the test or survives fixture cleanup", List.of(
                        outcome(execution), reports(execution).stream().map(entry -> List.of(
                                new Friendship(entry.get("username"), entry.get("invitee"), PENDING).exist(),
                                new ApiProfile(entry.get("invitee")).exist())).toList()),
                is(List.of(List.of(1L, 0L, 0L, List.of("body")), List.of(List.of(false, false)))));
    }

    @Test
    void negativeOutcomeCountCannotReachFixtureFactory() {
        EngineExecutionResults execution = execute(selectClass(InvalidOutcomeCase.class));
        assertThat("A negative outcome count reaches user creation or throws the wrong exception",
                execution.testEvents().failed().map(event -> event.getRequiredPayload(TestExecutionResult.class)
                        .getThrowable().orElseThrow()).toList(), contains(instanceOf(IllegalArgumentException.class)));
    }

    @Test
    void multipleParametersReceiveIndependentUsers() {
        EngineExecutionResults execution = execute(selectMethod(Cases.class, "multiple", TestReporter.class,
                TestUser.class, TestUser.class));
        List<String> created = users(execution, "create");
        assertThat("Multiple parameters share or receive the wrong fixture", List.of(
                        outcome(execution), created.stream().distinct().count(),
                        users(execution, "resolve").equals(created), users(execution, "body").equals(created),
                        users(execution, "close").equals(created.reversed())),
                is(List.of(List.of(1L, 0L, 0L, List.of("create", "create", "resolve", "resolve", "body", "body", "close", "close")),
                        2L, true, true, true)));
    }

    @Test
    void methodStoresDontReuseUsers() {
        EngineExecutionResults execution = execute(selectClass(IsolatedCases.class));
        List<String> created = users(execution, "create");
        assertThat("Method stores reuse or retain another test fixture", List.of(
                        outcome(execution), created.stream().distinct().count(),
                        users(execution, "body").equals(created), users(execution, "close").equals(created)),
                is(List.of(List.of(2L, 0L, 0L, List.of("create", "resolve", "body", "close", "create", "resolve", "body", "close")),
                        2L, true, true)));
    }

    @Test
    void concurrentMethodStoresDontShareFixtures() {
        EngineExecutionResults execution = EngineTestKit.engine("junit-jupiter").selectors(selectClass(IsolatedCases.class))
                .configurationParameter("junit.jupiter.execution.parallel.enabled", "true")
                .configurationParameter("junit.jupiter.execution.parallel.mode.default", "concurrent")
                .configurationParameter("junit.jupiter.execution.parallel.config.strategy", "fixed")
                .configurationParameter("junit.jupiter.execution.parallel.config.fixed.parallelism", "2")
                .configurationParameter("junit.jupiter.execution.timeout.default", "5s").execute();
        var traces = execution.testEvents().reportingEntryPublished().stream().collect(Collectors.groupingBy(
                event -> event.getTestDescriptor().getUniqueId(), Collectors.mapping(
                        event -> event.getRequiredPayload(ReportEntry.class).getKeyValuePairs().get("phase"), Collectors.toList())));
        assertThat("Concurrent method stores interfere with fixture lifecycle", List.of(
                        execution.testEvents().succeeded().count(), execution.testEvents().failed().count(),
                        execution.containerEvents().failed().count(), users(execution, "create").stream().distinct().count(),
                        traces.values().stream().allMatch(trace -> trace.equals(List.of("create", "resolve", "body", "close")))),
                is(List.of(2L, 0L, 0L, 2L, true)));
    }

    @Test
    void bodyFailureDoesntPreventCleanup() {
        EngineExecutionResults execution = execute(selectMethod(Cases.class, "failing", TestReporter.class, TestUser.class));
        assertThat("A failed test body prevents fixture cleanup", List.of(outcome(execution), failures(execution)),
                is(List.of(List.of(0L, 1L, 0L, List.of("create", "resolve", "body", "close")), List.of("Test body failed"))));
    }

    @Test
    void fixtureIsClosedWhenItsCreationFails() {
        EngineExecutionResults execution = execute(selectClass(CreationFailureCase.class));
        assertThat("A fixture is not stored before its creation", List.of(outcome(execution), failures(execution)),
                is(List.of(List.of(0L, 1L, 0L, List.of("create", "close")), List.of("Fixture creation failed"))));
    }

    @Test
    void secondCreationFailureDoesntSkipEitherCleanup() {
        EngineExecutionResults execution = execute(selectClass(PartialCreationCase.class));
        assertThat("Partial preparation leaves a fixture unclosed", List.of(
                        outcome(execution), failures(execution),
                        users(execution, "close").equals(users(execution, "create").reversed())),
                is(List.of(List.of(0L, 1L, 0L, List.of("create", "create", "close", "close")),
                        List.of("Second fixture creation failed"), true)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"negativeFriends", "negativeIncome", "negativeOutcome"})
    void invalidAnnotationDoesntCreateFixture(String method) {
        EngineExecutionResults execution = execute(selectMethod(Cases.class, method, TestReporter.class, TestUser.class));
        assertThat("An invalid annotation creates a fixture or reaches the body", List.of(
                        outcome(execution), failures(execution).stream().anyMatch(message -> message.contains("must be non-negative"))),
                is(List.of(List.of(0L, 1L, 0L, List.of()), true)));
    }

    @Test
    void invalidSecondAnnotationDoesntLeaveFirstFixture() {
        EngineExecutionResults execution = execute(selectMethod(Cases.class, "invalidSecond", TestUser.class, TestUser.class));
        assertThat("An invalid second annotation leaves the first fixture unclosed", List.of(outcome(execution), failures(execution)),
                is(List.of(List.of(0L, 1L, 0L, List.of("create", "close")),
                        List.of("Income invitations count must be non-negative, got -5"))));
    }

    @Test
    void unannotatedUserCannotBeResolved() {
        EngineExecutionResults execution = execute(selectMethod(Cases.class, "unannotated", TestUser.class));
        assertThat("An unannotated user parameter is accepted", List.of(outcome(execution),
                        failures(execution).stream().anyMatch(message -> message.contains("No ParameterResolver registered"))),
                is(List.of(List.of(0L, 1L, 0L, List.of()), true)));
    }

    @Test
    void annotatedWrongTypeCannotBeResolved() {
        EngineExecutionResults execution = execute(selectMethod(Cases.class, "wrongType", String.class));
        assertThat("An annotated parameter of the wrong type is accepted", List.of(outcome(execution),
                        failures(execution).stream().anyMatch(message -> message.contains("No ParameterResolver registered"))),
                is(List.of(List.of(0L, 1L, 0L, List.of("create", "close")), true)));
    }

    @Test
    void lifecycleMethodCannotResolveUser() {
        EngineExecutionResults execution = execute(selectClass(LifecycleCase.class));
        assertThat("A lifecycle method receives a test method fixture", List.of(outcome(execution),
                        failures(execution).stream().anyMatch(message -> message.contains("No ParameterResolver registered"))),
                is(List.of(List.of(0L, 1L, 0L, List.of()), true)));
    }

    @Test
    void cleanupFailureCannotBeReportedAsSuccess() {
        EngineExecutionResults execution = execute(selectClass(CleanupFailureCase.class));
        assertThat("A failed cleanup is reported as a successful test", List.of(outcome(execution),
                        failures(execution).stream().anyMatch(message -> message.contains("Fixture cleanup failed"))),
                is(List.of(List.of(0L, 1L, 0L, List.of("create", "resolve", "body", "close")), true)));
    }

    @Test
    void testWithoutUserDoesntCreateFixture() {
        EngineExecutionResults execution = execute(selectMethod(Cases.class, "withoutUser", TestReporter.class));
        assertThat("A test without user parameters creates a fixture", outcome(execution),
                is(List.of(1L, 0L, 0L, List.of("body"))));
    }

    private EngineExecutionResults execute(DiscoverySelector selector) {
        return EngineTestKit.engine("junit-jupiter").selectors(selector)
                .configurationParameter("junit.jupiter.execution.timeout.default", "5s").execute();
    }

    private List<Object> outcome(EngineExecutionResults execution) {
        return List.of(execution.testEvents().succeeded().count(), execution.testEvents().failed().count(),
                execution.containerEvents().failed().count(), reports(execution).stream().map(entry -> entry.get("phase")).toList());
    }

    private List<Map<String, String>> reports(EngineExecutionResults execution) {
        return execution.testEvents().reportingEntryPublished()
                .map(event -> event.getRequiredPayload(ReportEntry.class).getKeyValuePairs()).toList();
    }

    private List<String> users(EngineExecutionResults execution, String phase) {
        return reports(execution).stream().filter(entry -> entry.get("phase").equals(phase))
                .map(entry -> entry.get("username")).toList();
    }

    private List<String> failures(EngineExecutionResults execution) {
        return execution.testEvents().failed().map(event -> failureMessage(event.getRequiredPayload(TestExecutionResult.class)
                .getThrowable().orElseThrow())).toList();
    }

    private String failureMessage(Throwable failure) {
        return Optional.ofNullable(failure.getCause()).map(this::failureMessage).orElseGet(failure::getMessage);
    }

    static final class OutcomeInvitationCase {
        @Test
        void scenario(TestReporter reporter, @User(outcomeInvitations = 1) TestUser user) {
            String invitee = user.outcomeInvitations().getFirst().username();
            reporter.publishEntry(Map.of("phase", "body", "username", user.username(), "invitee", invitee));
            assertThat("An outgoing invitation or invitee profile is missing during the test", List.of(
                            new Friendship(user.username(), invitee, PENDING).exist(), new ApiProfile(invitee).exist()),
                    is(List.of(true, true)));
        }
    }

    static final class InvalidOutcomeCase {
        @RegisterExtension
        static final UserExtension extension = new UserExtension((request, reporter) -> {
            throw new AssertionError("A negative outcome count reached the fixture factory");
        });

        @Test
        void scenario(@User(outcomeInvitations = -1) TestUser user) {
            throw new AssertionError("A negative outcome count reached the test body");
        }
    }

    static final class Cases {
        @RegisterExtension
        static final UserExtension extension = new UserExtension(ReportedFixture::new);

        @Test
        void prepared(TestReporter reporter, @User TestUser user) {
            reporter.publishEntry(Map.of("phase", "body", "username", user.username()));
        }

        @Test
        void requested(TestReporter reporter, @User(friends = 3, incomeInvitations = 7, outcomeInvitations = 5) TestUser user) {
            reporter.publishEntry(Map.of("phase", "body", "username", user.username()));
        }

        @Test
        void multiple(TestReporter reporter, @User(friends = 3) TestUser left, @User(incomeInvitations = 7) TestUser right) {
            reporter.publishEntry(Map.of("phase", "body", "username", left.username()));
            reporter.publishEntry(Map.of("phase", "body", "username", right.username()));
        }

        @Test
        void failing(TestReporter reporter, @User TestUser user) {
            reporter.publishEntry(Map.of("phase", "body", "username", user.username()));
            throw new IllegalStateException("Test body failed");
        }

        @Test
        void negativeFriends(TestReporter reporter, @User(friends = -3) TestUser user) {
            reporter.publishEntry(Map.of("phase", "body", "username", user.username()));
        }

        @Test
        void negativeIncome(TestReporter reporter, @User(incomeInvitations = -7) TestUser user) {
            reporter.publishEntry(Map.of("phase", "body", "username", user.username()));
        }

        @Test
        void negativeOutcome(TestReporter reporter, @User(outcomeInvitations = -5) TestUser user) {
            reporter.publishEntry(Map.of("phase", "body", "username", user.username()));
        }

        @Test
        void invalidSecond(@User TestUser first, @User(incomeInvitations = -5) TestUser second) {
            throw new AssertionError("Invalid preparation reached test body");
        }

        @Test
        void unannotated(TestUser user) {
            throw new AssertionError("Unannotated parameter reached test body");
        }

        @Test
        void wrongType(@User String user) {
            throw new AssertionError("Wrong parameter type reached test body");
        }

        @Test
        void withoutUser(TestReporter reporter) {
            reporter.publishEntry("phase", "body");
        }
    }

    @ExtendWith(ParallelBarrier.class)
    static final class IsolatedCases {
        @RegisterExtension
        static final UserExtension extension = new UserExtension(ReportedFixture::new);

        @Test
        void first(TestReporter reporter, @User TestUser user) {
            reporter.publishEntry(Map.of("phase", "body", "username", user.username()));
        }

        @Test
        void second(TestReporter reporter, @User TestUser user) {
            reporter.publishEntry(Map.of("phase", "body", "username", user.username()));
        }
    }

    private static final class ParallelBarrier implements BeforeTestExecutionCallback {
        @Override
        public void beforeTestExecution(ExtensionContext context) throws Exception {
            if (context.getConfigurationParameter("junit.jupiter.execution.parallel.enabled", Boolean::parseBoolean).orElse(false)) {
                context.getRoot().getStore(ExtensionContext.Namespace.create(ParallelBarrier.class))
                        .computeIfAbsent("barrier", key -> new CyclicBarrier(2), CyclicBarrier.class)
                        .await(5, TimeUnit.SECONDS);
            }
        }
    }

    static final class CreationFailureCase {
        @RegisterExtension
        static final UserExtension extension = new UserExtension((request, reporter) ->
                new ReportedFixture(request, reporter, () -> {
                    throw new IllegalStateException("Fixture creation failed");
                }, () -> {
                }));

        @Test
        void scenario(TestReporter reporter, @User TestUser user) {
            reporter.publishEntry(Map.of("phase", "body", "username", user.username()));
        }
    }

    static final class PartialCreationCase {
        @RegisterExtension
        static final UserExtension extension = new UserExtension((request, reporter) ->
                new ReportedFixture(request, reporter, () -> {
                    if (request.friends() == 7) {
                        throw new IllegalStateException("Second fixture creation failed");
                    }
                }, () -> {
                }));

        @Test
        void scenario(@User(friends = 3) TestUser first, @User(friends = 7) TestUser second) {
            throw new AssertionError("Partial preparation reached test body");
        }
    }

    static final class LifecycleCase {
        @RegisterExtension
        static final UserExtension extension = new UserExtension(ReportedFixture::new);

        @BeforeEach
        void prepare(@User TestUser user) {
            throw new AssertionError("Lifecycle parameter was resolved");
        }

        @Test
        void scenario(TestReporter reporter) {
            reporter.publishEntry("phase", "body");
        }
    }

    static final class CleanupFailureCase {
        @RegisterExtension
        static final UserExtension extension = new UserExtension((request, reporter) ->
                new ReportedFixture(request, reporter, () -> {
                }, () -> {
                    throw new IllegalStateException("Fixture cleanup failed");
                }));

        @Test
        void scenario(TestReporter reporter, @User TestUser user) {
            reporter.publishEntry(Map.of("phase", "body", "username", user.username()));
        }
    }

    private static final class ReportedFixture implements UserFixture {
        private final TestUser user;
        private final TestReporter reporter;
        private final Runnable creation;
        private final Runnable cleanup;

        private ReportedFixture(UserRequest request, TestReporter reporter) {
            this(request, reporter, () -> {
            }, () -> {
            });
        }

        private ReportedFixture(UserRequest request, TestReporter reporter, Runnable creation, Runnable cleanup) {
            this.user = TestUser.unique()
                    .withFriends(IntStream.range(0, request.friends()).mapToObj(index -> TestUser.unique()).toList())
                    .withIncomeInvitations(IntStream.range(0, request.incomeInvitations()).mapToObj(index -> TestUser.unique()).toList())
                    .withOutcomeInvitations(IntStream.range(0, request.outcomeInvitations()).mapToObj(index -> TestUser.unique()).toList());
            this.reporter = reporter;
            this.creation = creation;
            this.cleanup = cleanup;
        }

        @Override
        public TestUser user() {
            reporter.publishEntry(Map.of("phase", "resolve", "username", user.username()));
            return user;
        }

        @Override
        public void create() {
            reporter.publishEntry(Map.of("phase", "create", "username", user.username(),
                    "friends", Integer.toString(user.friends().size()), "income", Integer.toString(user.incomeInvitations().size()),
                    "outcome", Integer.toString(user.outcomeInvitations().size())));
            creation.run();
        }

        @Override
        public void close() {
            reporter.publishEntry(Map.of("phase", "close", "username", user.username()));
            cleanup.run();
        }
    }
}
