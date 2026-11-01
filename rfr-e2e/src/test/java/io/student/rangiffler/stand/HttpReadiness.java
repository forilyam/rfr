package io.student.rangiffler.stand;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;
import org.apache.hc.core5.io.CloseMode;
import org.apache.hc.core5.util.Timeout;

public final class HttpReadiness implements StandService {
    private final String name;
    private final ClassicHttpRequest request;
    private final boolean verification;
    private final String command;

    public HttpReadiness(String name, ClassicHttpRequest request, boolean verification, String command) {
        this.name = name;
        this.request = ClassicRequestBuilder.copy(request).build();
        this.verification = verification;
        this.command = command;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String command() {
        return command;
    }

    @Override
    public boolean readiness(Duration timeout) {
        long deadline = System.nanoTime() + timeout.toNanos();
        try (Attempt attempt = new Attempt(
            HttpClients.custom().disableRedirectHandling().disableAutomaticRetries()
                .setConnectionManager(PoolingHttpClientConnectionManagerBuilder.create()
                    .setDefaultConnectionConfig(ConnectionConfig.custom()
                        .setConnectTimeout(Timeout.of(timeout)).setSocketTimeout(Timeout.of(timeout)).build())
                    .build())
                .setDefaultRequestConfig(RequestConfig.custom()
                    .setConnectionRequestTimeout(Timeout.of(timeout)).setResponseTimeout(Timeout.of(timeout)).build())
                .build(),
            Executors.newVirtualThreadPerTaskExecutor()
        )) {
            int status = attempt.status(request, Math.max(1L, deadline - System.nanoTime()));
            return !verification || status < 400;
        } catch (ExecutionException | TimeoutException exception) {
            return false;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for " + name, exception);
        }
    }

    private static final class Attempt implements AutoCloseable {
        private final CloseableHttpClient client;
        private final ExecutorService executor;

        private Attempt(CloseableHttpClient client, ExecutorService executor) {
            this.client = client;
            this.executor = executor;
        }

        private int status(ClassicHttpRequest request, long timeout)
            throws ExecutionException, InterruptedException, TimeoutException {
            Future<Integer> response = executor.submit(() ->
                client.execute(ClassicRequestBuilder.copy(request).build(), HttpResponse::getCode));
            try {
                return response.get(timeout, TimeUnit.NANOSECONDS);
            } finally {
                response.cancel(true);
            }
        }

        @Override
        public void close() {
            try {
                client.close(CloseMode.IMMEDIATE);
            } finally {
                executor.shutdownNow();
            }
        }
    }
}
