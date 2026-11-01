package io.student.rangiffler.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public final class TearDown implements AutoCloseable {
    private final List<Runnable> steps;

    public TearDown() {
        this(List.of());
    }

    private TearDown(List<Runnable> steps) {
        this.steps = steps;
    }

    public TearDown with(Runnable step) {
        return new TearDown(Stream.concat(steps.stream(),
                Stream.of(Objects.requireNonNull(step, "Cleanup step must not be null"))).toList());
    }

    public <T> TearDown withEach(Iterable<T> items, Consumer<? super T> action) {
        Objects.requireNonNull(action, "Cleanup action must not be null");
        return new TearDown(Stream.concat(steps.stream(),
                StreamSupport.stream(items.spliterator(), false)
                        .<Runnable>map(item -> () -> action.accept(item))).toList());
    }

    @Override
    public void close() {
        List<RuntimeException> failures = new ArrayList<>();
        for (Runnable step : steps) {
            try {
                step.run();
            } catch (RuntimeException failure) {
                failures.add(failure);
            }
        }
        if (!failures.isEmpty()) {
            RuntimeException first = failures.getFirst();
            failures.stream().skip(1).filter(failure -> failure != first).forEach(first::addSuppressed);
            throw first;
        }
    }
}
