package io.student.rangiffler.jupiter.probe;

import io.student.rangiffler.jupiter.annotation.ProbeTarget;
import io.student.rangiffler.jupiter.extension.ProbeTargetCondition;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.platform.testkit.engine.EngineExecutionResults;
import org.junit.platform.testkit.engine.EngineTestKit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

@Timeout(10)
final class ProbeTargetConditionTest {
    @ParameterizedTest
    @ValueSource(strings = {"SequentialCases", "FailureCases", "ConcurrentCases", "ExhaustionCases"})
    void queueTargetCannotRunDirectly(String target) {
        EngineExecutionResults execution = EngineTestKit.engine("junit-jupiter")
                .selectors(selectClass(UsersQueueExtensionTest.class.getName() + "$" + target))
                .configurationParameter("junit.jupiter.extensions.autodetection.enabled", "false").execute();
        assertThat("A users queue probe target runs directly", execution.containerEvents().skipped().count(), is(1L));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Cases", "OutcomeInvitationCase", "IsolatedCases", "InvalidSecondCase", "LifecycleCase"})
    void userExtensionTargetCannotRunDirectly(String target) {
        EngineExecutionResults execution = EngineTestKit.engine("junit-jupiter")
                .selectors(selectClass(UserExtensionTest.class.getName() + "$" + target))
                .configurationParameter("junit.jupiter.extensions.autodetection.enabled", "false").execute();
        assertThat("A UserExtension probe target runs directly", execution.containerEvents().skipped().count(), is(1L));
    }

    @Test
    void targetCannotRunWithoutFlag() {
        EngineExecutionResults execution = EngineTestKit.engine("junit-jupiter").selectors(selectClass(Target.class))
                .configurationParameter("junit.jupiter.extensions.autodetection.enabled", "false").execute();
        assertThat("A probe target runs without the flag", execution.containerEvents().skipped().count(), is(1L));
    }

    @ParameterizedTest
    @ValueSource(strings = {"false", "invalid-83", ""})
    void targetCannotRunWithoutTrueFlag(String flag) {
        EngineExecutionResults execution = EngineTestKit.engine("junit-jupiter").selectors(selectClass(Target.class))
                .configurationParameter("junit.jupiter.extensions.autodetection.enabled", "false")
                .configurationParameter(ProbeTargetCondition.PARAMETER, flag).execute();
        assertThat("A probe target runs with a disabled or invalid flag", execution.containerEvents().skipped().count(), is(1L));
    }

    @Test
    void targetRunsWithTrueFlag() {
        EngineExecutionResults execution = EngineTestKit.engine("junit-jupiter").selectors(selectClass(Target.class))
                .configurationParameter("junit.jupiter.extensions.autodetection.enabled", "false")
                .configurationParameter(ProbeTargetCondition.PARAMETER, "true").execute();
        assertThat("A probe target cannot run with the flag enabled", execution.testEvents().succeeded().count(), is(1L));
    }

    @ProbeTarget
    static final class Target {
        @Test
        void scenario() {
        }
    }
}
