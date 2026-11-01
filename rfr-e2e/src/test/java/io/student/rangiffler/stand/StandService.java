package io.student.rangiffler.stand;

import java.time.Duration;

public interface StandService {
    String name();

    String command();

    boolean readiness(Duration timeout);
}
