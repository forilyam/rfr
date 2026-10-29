package io.student.rangiffler.data;

public record UserRequest(int friends, int incomeInvitations) {
    public UserRequest {
        if (friends < 0) {
            throw new IllegalArgumentException("Friends count must be non-negative, got " + friends);
        }
        if (incomeInvitations < 0) {
            throw new IllegalArgumentException("Income invitations count must be non-negative, got " + incomeInvitations);
        }
    }

    public UserFixture fixture() {
        UserFixture fixture = new SingleUserFixture(TestUser.unique());
        if (friends > 0) {
            fixture = new UserWithFriendsFixture(fixture, friends);
        }
        if (incomeInvitations > 0) {
            fixture = new UserWithIncomeInvitations(fixture, incomeInvitations);
        }
        return fixture;
    }
}
