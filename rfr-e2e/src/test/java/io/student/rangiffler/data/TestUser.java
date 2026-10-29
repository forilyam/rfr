package io.student.rangiffler.data;

import io.student.rangiffler.utils.RandomDataUtils;

import java.util.List;
import java.util.stream.Stream;

public record TestUser(String username, String password, List<TestUser> friends, List<TestUser> incomeInvitations) {
    public TestUser {
        friends = List.copyOf(friends);
        incomeInvitations = List.copyOf(incomeInvitations);
    }

    public TestUser(String username, String password) {
        this(username, password, List.of(), List.of());
    }

    public static TestUser unique() {
        return new TestUser(RandomDataUtils.randomUsername(), RandomDataUtils.randomPassword());
    }

    public TestUser withFriends(List<TestUser> friends) {
        return new TestUser(username, password,
                Stream.concat(this.friends.stream(), friends.stream()).toList(), incomeInvitations);
    }

    public TestUser withIncomeInvitations(List<TestUser> invitations) {
        return new TestUser(username, password, friends,
                Stream.concat(incomeInvitations.stream(), invitations.stream()).toList());
    }

    @Override
    public String toString() {
        return "TestUser[username=" + username + ", password=<redacted>]";
    }
}
