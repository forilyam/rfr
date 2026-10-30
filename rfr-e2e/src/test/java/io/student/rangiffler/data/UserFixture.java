package io.student.rangiffler.data;

public interface UserFixture extends AutoCloseable {
    TestUser user();

    void create();

    @Override
    void close();
}
