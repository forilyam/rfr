package io.student.rangiffler.stand;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.sun.net.httpserver.HttpServer;

import java.net.InetSocketAddress;
import java.net.InetAddress;
import java.net.URI;

import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;

import java.time.Duration;
import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@Timeout(5)
final class HttpReadinessTest {
    @Test
    void localhostCannotIgnoreIpv6WhenIpv4IsUnavailable() throws Exception {
        assumeTrue(Arrays.stream(InetAddress.getAllByName("localhost"))
                .anyMatch(address -> address.getHostAddress().contains(":")), "localhost has no IPv6 address");
        HttpServer server = HttpServer.create(new InetSocketAddress("::1", 0), 13);
        server.createContext("/", exchange -> {
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
        });
        server.start();
        boolean readiness;
        try {
            readiness = new HttpReadiness(
                    "rfr-gql-client",
                    ClassicRequestBuilder.get(URI.create("http://localhost:" + server.getAddress().getPort())).build(),
                    true,
                    "npm run dev"
            ).readiness(Duration.ofMillis(1837));
        } finally {
            server.stop(0);
        }
        assertThat("A localhost service on IPv6 is ignored after IPv4 connection refusal", readiness, is(true));
    }

    @Test
    void graphqlResponseCannotRequireSuccessfulStatusWhenCurlDoesnt() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 7);
        server.createContext("/graphql", exchange -> {
            exchange.sendResponseHeaders(503, -1);
            exchange.close();
        });
        server.start();
        boolean readiness;
        try {
            readiness = new HttpReadiness(
                    "rfr-api",
                    ClassicRequestBuilder.post(URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/graphql"))
                            .setEntity("{\"query\":\"{__typename}\"}", ContentType.APPLICATION_JSON).build(),
                    false,
                    "./gradlew runAll"
            ).readiness(Duration.ofMillis(1739));
        } finally {
            server.stop(0);
        }
        assertThat("An HTTP error response is rejected despite the script using curl without -f", readiness, is(true));
    }

    @Test
    void frontendErrorResponseCannotBeConsideredReady() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 11);
        server.createContext("/", exchange -> {
            exchange.sendResponseHeaders(418, -1);
            exchange.close();
        });
        server.start();
        boolean readiness;
        try {
            readiness = new HttpReadiness(
                    "rfr-gql-client",
                    ClassicRequestBuilder.get(URI.create("http://127.0.0.1:" + server.getAddress().getPort())).build(),
                    true,
                    "npm run dev"
            ).readiness(Duration.ofMillis(1289));
        } finally {
            server.stop(0);
        }
        assertThat("An HTTP error response is accepted despite the script using curl -f", readiness, is(false));
    }
}
