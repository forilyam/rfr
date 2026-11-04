package io.student.rangiffler.stand;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class SocketReadiness implements StandService {
    private final String name;
    private final InetSocketAddress address;
    private final String command;

    public SocketReadiness(String name, InetSocketAddress address, String command) {
        this.name = name;
        this.address = address;
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
        try (Attempt attempt = new Attempt(new Socket(), Executors.newVirtualThreadPerTaskExecutor())) {
            return attempt.response(address, timeout);
        } catch (IOException | ExecutionException | TimeoutException exception) {
            return false;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for " + name, exception);
        }
    }

    private static final class Attempt implements AutoCloseable {
        private final Socket socket;
        private final ExecutorService executor;

        private Attempt(Socket socket, ExecutorService executor) {
            this.socket = socket;
            this.executor = executor;
        }

        private boolean response(InetSocketAddress address, Duration timeout)
                throws ExecutionException, InterruptedException, TimeoutException {
            long deadline = System.nanoTime() + timeout.toNanos();
            Future<Boolean> response = executor.submit(() -> {
                socket.connect(
                        new InetSocketAddress(address.getHostString(), address.getPort()),
                        Math.clamp(timeout.toMillis(), 1, Integer.MAX_VALUE)
                );
                return true;
            });
            try {
                return response.get(Math.max(1L, deadline - System.nanoTime()), TimeUnit.NANOSECONDS);
            } finally {
                response.cancel(true);
            }
        }

        @Override
        public void close() throws IOException {
            try {
                socket.close();
            } finally {
                executor.shutdownNow();
            }
        }
    }
}
