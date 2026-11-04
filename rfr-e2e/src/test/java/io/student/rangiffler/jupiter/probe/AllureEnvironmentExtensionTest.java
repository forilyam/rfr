package io.student.rangiffler.jupiter.probe;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

import io.student.rangiffler.jupiter.annotation.ProbeTarget;
import io.student.rangiffler.config.TestConfig;
import io.student.rangiffler.jupiter.extension.AllureEnvironmentExtension;
import io.student.rangiffler.jupiter.extension.SuiteExtension;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Properties;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestReporter;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.io.TempDir;
import org.junit.platform.testkit.engine.EngineExecutionResults;
import org.junit.platform.testkit.engine.EngineTestKit;

@NullMarked
@Timeout(20)
final class AllureEnvironmentExtensionTest {
    @Test
    void completedSuiteCannotOmitAnyEnvironmentKey(@TempDir(cleanup = org.junit.jupiter.api.io.CleanupMode.NEVER) Path directory) throws Exception {
        int port;
        try (ServerSocket socket = new ServerSocket(0, 17, InetAddress.getByName("127.0.0.1"))) {
            port = socket.getLocalPort();
        }
        int exit = execute(directory, port);
        assertThat("The results directory does not contain environment.properties with all required keys",
                List.of(exit, properties(directory).stringPropertyNames()),
                is(List.of(0, java.util.Set.of("frontUrl", "authUrl", "apiUrl", "browser", "headless", "commit",
                        "rangiffler-auth.version", "rangiffler-api.version"))));
    }

    @Test
    void stalledMysqlQueriesCannotPreventEnvironmentFile(@TempDir(cleanup = org.junit.jupiter.api.io.CleanupMode.NEVER) Path directory) throws Exception {
        CountDownLatch queries = new CountDownLatch(2);
        int exit;
        try (SilentMysql server = new SilentMysql(
                new ServerSocket(0, 23, InetAddress.getByName("127.0.0.1")),
                Executors.newVirtualThreadPerTaskExecutor(), queries, new ConcurrentLinkedQueue<>())) {
            server.start();
            exit = execute(directory, server.port());
        }
        Properties properties = Files.exists(directory.resolve("environment.properties")) ? properties(directory) : new Properties();
        assertThat("Stalled SQL queries do not finish with unavailable versions and the real environment values",
                List.of(exit, queries.getCount(), properties),
                is(List.of(0, 0L, Map.of(
                        "frontUrl", "http://frontend.invalid:3187",
                        "authUrl", "http://auth.invalid:9137",
                        "apiUrl", "http://api.invalid:8171",
                        "browser", "firefox",
                        "headless", "false",
                        "commit", "ac79b31",
                        "rangiffler-auth.version", "unavailable",
                        "rangiffler-api.version", "unavailable"))));
    }

    @Test
    void completedSuiteCannotOmitEnvironmentProperties(@TempDir(cleanup = org.junit.jupiter.api.io.CleanupMode.NEVER) Path directory) throws Exception {
        EngineExecutionResults execution = execute(directory, CompletedTest.class);
        assertThat("A completed suite does not write all environment values",
                List.of(execution.testEvents().succeeded().count(), properties(directory)),
                is(List.of(1L, environment())));
    }

    @Test
    void failedPreparationCannotPreventEnvironmentProperties(@TempDir(cleanup = org.junit.jupiter.api.io.CleanupMode.NEVER) Path directory) throws Exception {
        EngineExecutionResults execution = execute(directory, FailedTest.class);
        assertThat("A failed suite preparation executes the body or loses environment values",
                List.of(execution.testEvents().started().count(), execution.containerEvents().failed().count(), properties(directory)),
                is(List.of(0L, 1L, environment())));
    }

    @Test
    void failedBeforeAllCannotInvokeFollowingExtension(@TempDir(cleanup = org.junit.jupiter.api.io.CleanupMode.NEVER) Path directory) {
        EngineExecutionResults execution = execute(directory, InterruptedTest.class);
        assertThat("JUnit invokes a later beforeAll after an earlier callback fails",
                List.of(execution.containerEvents().failed().count(), Files.exists(directory.resolve("following"))),
                is(List.of(1L, false)));
    }

    private EngineExecutionResults execute(Path directory, Class<?> target) {
        return EngineTestKit.engine("junit-jupiter").selectors(selectClass(target))
                .configurationParameter("junit.jupiter.extensions.autodetection.enabled", "false")
                .configurationParameter("junit.jupiter.execution.parallel.enabled", "false")
                .configurationParameter("probe.results", directory.toString())
                .execute();
    }

    private int execute(Path directory, int port) throws Exception {
        Process process = new ProcessBuilder(
                Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-cp", TestConfig.testClasspath(),
                "-Dallure.results.directory=" + directory,
                "-Drfr.dbUrl=jdbc:mysql://127.0.0.1:" + port,
                "-Drfr.dbTimeout=1",
                "-Drfr.frontUrl=http://frontend.invalid:3187",
                "-Drfr.authUrl=http://auth.invalid:9137",
                "-Drfr.apiUrl=http://api.invalid:8171",
                "-Drfr.browser=firefox", "-Drfr.headless=false", "-Drfr.commit=ac79b31",
                "-Drfr.authMigrations=" + Path.of(TestConfig.authMigrations()).toAbsolutePath(),
                "-Drfr.apiMigrations=" + Path.of(TestConfig.apiMigrations()).toAbsolutePath(),
                "io.student.rangiffler.jupiter.probe.AllureEnvironmentExtensionTest$RealEnvironmentTest"
        ).redirectErrorStream(true).redirectOutput(directory.resolve("probe.log").toFile()).start();
        try (ProbeJvm probe = new ProbeJvm(process)) {
            return probe.exit();
        }
    }

    private Properties properties(Path directory) throws Exception {
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(directory.resolve("environment.properties"))) {
            properties.load(input);
        }
        return properties;
    }

    private Map<String, String> environment() {
        return Map.of(
                "frontUrl", "http://frontend.invalid:3187",
                "authUrl", "http://auth.invalid:9137",
                "apiUrl", "http://api.invalid:8171",
                "browser", "firefox",
                "headless", "false",
                "commit", "ac79b31",
                "rangiffler-auth.version", "unavailable",
                "rangiffler-api.version", "17.3"
        );
    }

    @ProbeTarget
    @ExtendWith(AllureEnvironmentExtension.class)
    static final class RealEnvironmentTest {
        static void main() {
            EngineTestKit.engine("junit-jupiter").selectors(selectClass(RealEnvironmentTest.class))
                    .configurationParameter("junit.jupiter.extensions.autodetection.enabled", "false")
                    .execute().testEvents().assertStatistics(statistics -> statistics.succeeded(1));
        }

        @Test
        void scenario(TestReporter reporter) {
            reporter.publishEntry("phase", "body");
        }
    }

    @ProbeTarget
    @ExtendWith(Environment.class)
    static final class CompletedTest {
        @Test
        void scenario(TestReporter reporter) {
            reporter.publishEntry("phase", "body");
        }
    }

    @ProbeTarget
    @ExtendWith({Environment.class, Failure.class})
    static final class FailedTest {
        @Test
        void scenario(TestReporter reporter) {
            reporter.publishEntry("phase", "body");
        }
    }

    @ProbeTarget
    @ExtendWith({Failure.class, Following.class})
    static final class InterruptedTest {
        @Test
        void scenario(TestReporter reporter) {
            reporter.publishEntry("phase", "body");
        }
    }

    static final class Environment implements BeforeAllCallback {
        @Override
        public void beforeAll(ExtensionContext context) {
            new AllureEnvironmentExtension(
                    Path.of(context.getConfigurationParameter("probe.results").orElseThrow()),
                    () -> new AllureEnvironmentExtensionTest().environment()
            ).beforeAll(context);
        }
    }

    static final class Failure implements SuiteExtension {
        @Override
        public void beforeSuite(ExtensionContext context) {
            throw new IllegalStateException("Probe suite preparation failed");
        }
    }

    static final class Following implements BeforeAllCallback {
        @Override
        public void beforeAll(ExtensionContext context) throws Exception {
            Files.createFile(Path.of(context.getConfigurationParameter("probe.results").orElseThrow()).resolve("following"));
        }
    }

    private record ProbeJvm(Process process) implements AutoCloseable {
        private int exit() throws InterruptedException {
            return process.waitFor(9, TimeUnit.SECONDS) ? process.exitValue() : -1;
        }

        @Override
        public void close() throws InterruptedException {
            if (process.isAlive()) {
                process.destroy();
                if (!process.waitFor(3, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Environment probe did not stop after termination");
                }
            }
        }
    }

    private record SilentMysql(
            ServerSocket server, ExecutorService executor, CountDownLatch queries, Queue<Socket> sockets
    ) implements AutoCloseable {
        private int port() {
            return server.getLocalPort();
        }

        private void start() throws IOException {
            server.setSoTimeout(317);
            executor.submit(() -> {
                while (!server.isClosed()) {
                    try {
                        Socket socket = server.accept();
                        synchronized (sockets) {
                            if (server.isClosed()) {
                                socket.close();
                            } else {
                                sockets.add(socket);
                                executor.submit(() -> respond(socket));
                            }
                        }
                    } catch (SocketTimeoutException exception) {
                        if (Thread.currentThread().isInterrupted()) {
                            return;
                        }
                    } catch (IOException exception) {
                        if (!server.isClosed()) {
                            throw new UncheckedIOException("Cannot accept a MySQL probe connection", exception);
                        }
                    }
                }
            });
        }

        private void respond(Socket socket) {
            try {
                socket.setSoTimeout(15000);
                ByteArrayOutputStream greeting = new ByteArrayOutputStream();
                greeting.write(10);
                greeting.writeBytes("8.0.37\0".getBytes(StandardCharsets.US_ASCII));
                greeting.writeBytes(new byte[]{23, 0, 0, 0});
                greeting.writeBytes("sAlT7xQ9\0".getBytes(StandardCharsets.US_ASCII));
                greeting.writeBytes(new byte[]{9, -94, 45, 2, 0, 10, 0, 21});
                greeting.writeBytes(new byte[10]);
                greeting.writeBytes("bCdE2fGhJkLm\0mysql_native_password\0".getBytes(StandardCharsets.US_ASCII));
                send(socket, greeting.toByteArray(), 0);
                receive(socket.getInputStream());
                send(socket, new byte[]{0, 0, 0, 2, 0, 0, 0}, 2);
                receive(socket.getInputStream());
                queries.countDown();
            } catch (IOException exception) {
                if (!socket.isClosed()) {
                    throw new UncheckedIOException("MySQL probe connection failed", exception);
                }
            }
        }

        private void receive(InputStream input) throws IOException {
            byte[] header = input.readNBytes(4);
            if (header.length != 4) {
                throw new IOException("MySQL probe did not receive a packet header");
            }
            int length = (header[0] & 255) | (header[1] & 255) << 8 | (header[2] & 255) << 16;
            if (input.readNBytes(length).length != length) {
                throw new IOException("MySQL probe received an incomplete packet");
            }
        }

        private void send(Socket socket, byte[] payload, int sequence) throws IOException {
            socket.getOutputStream().write(payload.length);
            socket.getOutputStream().write(payload.length >> 8);
            socket.getOutputStream().write(payload.length >> 16);
            socket.getOutputStream().write(sequence);
            socket.getOutputStream().write(payload);
            socket.getOutputStream().flush();
        }

        @Override
        public void close() throws Exception {
            List<IOException> failures = new ArrayList<>();
            try {
                server.close();
            } catch (IOException exception) {
                failures.add(exception);
            }
            synchronized (sockets) {
                for (Socket socket : sockets) {
                    try {
                        socket.close();
                    } catch (IOException exception) {
                        failures.add(exception);
                    }
                }
            }
            executor.shutdownNow();
            if (!executor.awaitTermination(3, TimeUnit.SECONDS)) {
                throw new IllegalStateException("MySQL probe tasks did not stop after closing their sockets");
            }
            if (!failures.isEmpty()) {
                IOException failure = failures.getFirst();
                failures.stream().skip(1).forEach(failure::addSuppressed);
                throw failure;
            }
        }
    }
}
