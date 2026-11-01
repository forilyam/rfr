package io.student.rangiffler.jupiter.probe;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

import io.student.rangiffler.jupiter.extension.SuiteExtension;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestReporter;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.engine.reporting.ReportEntry;
import org.junit.platform.testkit.engine.EngineExecutionResults;
import org.junit.platform.testkit.engine.EngineTestKit;

@Timeout(15)
final class SuiteExtensionTest {
    @Test
    void suiteCallbacksCannotRepeatForClassesInTheSameEngine() {
        EngineExecutionResults execution = EngineTestKit.engine("junit-jupiter")
                .selectors(selectClass(FirstTest.class), selectClass(SecondTest.class))
                .configurationParameter("junit.jupiter.execution.parallel.enabled", "false")
                .configurationParameter("junit.jupiter.execution.timeout.default", "5s")
                .execute();
        assertThat("Suite callbacks dont surround both classes exactly once",
                execution.allEvents().reportingEntryPublished()
                        .map(event -> event.getRequiredPayload(ReportEntry.class).getKeyValuePairs().get("phase"))
                        .toList(),
                is(List.of("before", "body", "body", "after")));
    }

    @ExtendWith(Observed.class)
    static final class FirstTest {
        @Test
        void scenario(TestReporter reporter) {
            reporter.publishEntry("phase", "body");
        }
    }

    @ExtendWith(Observed.class)
    static final class SecondTest {
        @Test
        void scenario(TestReporter reporter) {
            reporter.publishEntry("phase", "body");
        }
    }

    static final class Observed implements SuiteExtension {
        @Override
        public void beforeSuite(ExtensionContext context) {
            context.publishReportEntry("phase", "before");
        }

        @Override
        public void afterSuite(ExtensionContext context) {
            context.publishReportEntry("phase", "after");
        }
    }
}
