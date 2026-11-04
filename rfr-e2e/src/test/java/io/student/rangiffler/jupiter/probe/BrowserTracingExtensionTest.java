package io.student.rangiffler.jupiter.probe;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.junit.UsePlaywright;
import io.student.rangiffler.config.TestConfig;
import io.student.rangiffler.jupiter.annotation.ProbeTarget;
import io.student.rangiffler.jupiter.annotation.UiTest;
import io.student.rangiffler.jupiter.extension.BrowserOptions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipFile;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assumptions.abort;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

@Timeout(60)
@UsePlaywright(BrowserOptions.class)
final class BrowserTracingExtensionTest {
    @ParameterizedTest
    @ValueSource(strings = {"body", "setup", "teardown"})
    void failedTestsCannotLoseTraceOrAllureAttachment(String phase, @TempDir(cleanup = CleanupMode.NEVER) Path directory) throws Exception {
        execute(directory, phase);
        List<Path> traces = archives(directory.resolve("traces"));
        List<Path> attachments = archives(directory.resolve("allure"));
        try (ZipFile trace = new ZipFile(traces.getFirst().toFile())) {
            assertThat("A failed test does not retain screenshots, snapshots, sources and its Allure ZIP attachment",
                    List.of(traces.size(), attachments.size(),
                            trace.stream().anyMatch(entry -> entry.getName().endsWith(".trace")),
                            trace.stream().anyMatch(entry -> entry.getName().endsWith(".jpeg")),
                            trace.stream().anyMatch(entry -> entry.getName().startsWith("src/") && entry.getName().endsWith(".java")),
                            Files.mismatch(traces.getFirst(), attachments.getFirst())),
                    is(List.of(1, 1, true, true, true, -1L)));
        }
    }

    @Test
    void successfulTestsCannotRetainTrace(@TempDir(cleanup = CleanupMode.NEVER) Path directory) throws Exception {
        execute(directory, "success");
        assertThat("A successful test retains a trace archive or Allure ZIP attachment",
                List.of(archives(directory.resolve("traces")), archives(directory.resolve("allure"))),
                is(List.of(List.of(), List.of())));
    }

    @Test
    void abortedTestsCannotRetainTrace(@TempDir(cleanup = CleanupMode.NEVER) Path directory) throws Exception {
        execute(directory, "abort");
        assertThat("An aborted test retains a trace archive or Allure ZIP attachment",
                List.of(archives(directory.resolve("traces")), archives(directory.resolve("allure"))),
                is(List.of(List.of(), List.of())));
    }

    private void execute(Path directory, String phase) throws Exception {
        Process process = new ProcessBuilder(
                Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-cp", TestConfig.testClasspath(),
                "-Dallure.results.directory=" + directory.resolve("allure"),
                "-Drfr.traces=" + directory.resolve("traces"),
                "-Drfr.headless=true",
                BrowserTracingExtensionTest.class.getName(), phase
        ).redirectErrorStream(true).redirectOutput(directory.resolve("probe.log").toFile()).start();
        try {
            if (!process.waitFor(45, TimeUnit.SECONDS) || process.exitValue() != 0) {
                throw new IllegalStateException("Tracing probe did not complete: " + Files.readString(directory.resolve("probe.log")));
            }
        } finally {
            if (process.isAlive()) {
                process.destroy();
                if (!process.waitFor(5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Tracing probe did not stop after termination");
                }
            }
        }
    }

    private List<Path> archives(Path directory) throws Exception {
        if (!Files.exists(directory)) {
            return List.of();
        }
        try (var files = Files.walk(directory)) {
            return files.filter(path -> path.toString().endsWith(".zip")).toList();
        }
    }

    public static void main(String[] arguments) {
        Class<?> target = switch (arguments[0]) {
            case "body" -> FailedBody.class;
            case "setup" -> FailedSetup.class;
            case "teardown" -> FailedTeardown.class;
            case "abort" -> Aborted.class;
            case "success" -> Successful.class;
            default -> throw new IllegalArgumentException("Unknown tracing probe phase: " + arguments[0]);
        };
        SummaryGeneratingListener listener = new SummaryGeneratingListener();
        LauncherFactory.create().execute(LauncherDiscoveryRequestBuilder.request()
                .selectors(selectClass(target))
                .configurationParameter("junit.jupiter.extensions.autodetection.enabled", "false")
                .configurationParameter("junit.jupiter.execution.parallel.enabled", "false")
                .build(), listener);
        long failures = switch (arguments[0]) {
            case "body", "setup", "teardown" -> 1L;
            default -> 0L;
        };
        if (listener.getSummary().getTestsStartedCount() != 1
                || listener.getSummary().getTestsFailedCount() != failures
                || listener.getSummary().getFailures().stream().anyMatch(failure ->
                !failure.getException().getMessage().equals("Tracing probe " + arguments[0] + " failed"))
                || listener.getSummary().getTestsAbortedCount() != (arguments[0].equals("abort") ? 1 : 0)) {
            listener.getSummary().printFailuresTo(new java.io.PrintWriter(System.err, true));
            throw new IllegalStateException("Tracing probe reported unexpected test outcomes");
        }
    }

    @ProbeTarget
    @UiTest
    static final class Successful {
        @Test
        void scenario(Page page) {
            page.setContent("<title>Tracing 7291</title>");
        }
    }

    @ProbeTarget
    @UiTest
    static final class FailedBody {
        @Test
        void scenario(Page page) {
            page.setContent("<title>Tracing 3187</title>");
            page.screenshot();
            throw new AssertionError("Tracing probe body failed");
        }
    }

    @ProbeTarget
    @UiTest
    static final class FailedSetup {
        @BeforeEach
        void prepare(Page page) {
            page.setContent("<title>Tracing 5179</title>");
            page.screenshot();
            throw new IllegalStateException("Tracing probe setup failed");
        }

        @Test
        void scenario(Page page) {
            page.title();
        }
    }

    @ProbeTarget
    @UiTest
    static final class FailedTeardown {
        @Test
        void scenario(Page page) {
            page.setContent("<title>Tracing 6833</title>");
        }

        @AfterEach
        void finish(Page page) {
            page.screenshot();
            throw new IllegalStateException("Tracing probe teardown failed");
        }
    }

    @ProbeTarget
    @UiTest
    static final class Aborted {
        @Test
        void scenario(Page page) {
            page.setContent("<title>Tracing 9311</title>");
            abort("Tracing probe was aborted");
        }
    }
}
