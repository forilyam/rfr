package io.student.rangiffler.data;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public final class StaticUserFixture implements UserFixture {
    private static final LinkedBlockingQueue<TestUser> USERS = new LinkedBlockingQueue<>(3);
    private final List<TestUser> lease;

    static {
        USERS.add(new TestUser("rfr_static_iris", "StaticIris73!"));
        USERS.add(new TestUser("rfr_static_leon", "StaticLeon29!"));
        USERS.add(new TestUser("rfr_static_nora", "StaticNora61!"));
    }

    public StaticUserFixture() {
        this.lease = new ArrayList<>(1);
    }

    @Override
    public synchronized TestUser user() {
        if (lease.isEmpty()) {
            throw new IllegalStateException("No static user is leased by this fixture");
        }
        return lease.getFirst();
    }

    @Override
    public synchronized void create() {
        if (!lease.isEmpty()) {
            throw new IllegalStateException("This fixture already leases a static user");
        }
        try {
            TestUser user = USERS.poll(5, TimeUnit.SECONDS);
            if (user == null) {
                throw new IllegalStateException("Timed out after 5 seconds waiting for a static user from the pool of 3");
            }
            lease.add(user);
        } catch (InterruptedException failure) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for a static user from the pool of 3", failure);
        }
    }

    @Override
    public synchronized void close() {
        if (!lease.isEmpty()) {
            if (!USERS.offer(lease.getFirst())) {
                throw new IllegalStateException("Cannot return the static user because its pool is full");
            }
            lease.clear();
        }
    }
}
