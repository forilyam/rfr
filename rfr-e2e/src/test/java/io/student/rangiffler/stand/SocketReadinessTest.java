package io.student.rangiffler.stand;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@Timeout(5)
final class SocketReadinessTest {
    @Test
    void openPortCannotRequireMysqlAuthentication() throws Exception {
        boolean readiness;
        try (ServerSocket server = new ServerSocket()) {
            server.bind(new InetSocketAddress("127.0.0.1", 0), 13);
            readiness = new SocketReadiness(
                    "MySQL", InetSocketAddress.createUnresolved("127.0.0.1", server.getLocalPort()),
                    "docker compose up -d --wait"
            ).readiness(Duration.ofMillis(857));
        }
        assertThat("A listening TCP port requires a MySQL handshake that the script does not perform", readiness, is(true));
    }

    @Test
    void closedPortCannotBeConsideredReadyOrWaitIndefinitely() throws Exception {
        InetSocketAddress address;
        try (ServerSocket server = new ServerSocket()) {
            server.bind(new InetSocketAddress("127.0.0.1", 0), 17);
            address = InetSocketAddress.createUnresolved("127.0.0.1", server.getLocalPort());
        }
        long start = System.nanoTime();
        boolean readiness = new SocketReadiness("MySQL", address, "docker compose up -d --wait")
                .readiness(Duration.ofMillis(463));
        assertThat("A closed TCP port is accepted or exceeds the connection deadline",
                List.of(readiness, System.nanoTime() - start < Duration.ofSeconds(2).toNanos()), is(List.of(false, true)));
    }
}
