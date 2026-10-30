package io.student.rangiffler.data;

import io.student.rangiffler.data.db.AuthAccount;
import io.student.rangiffler.data.db.Friendships;
import io.student.rangiffler.data.db.ApiProfile;

public final class SingleUserFixture implements UserFixture {
    private final TestUser user;
    private final AuthAccount account;
    private final ApiProfile profile;
    private final Friendships friendships;

    public SingleUserFixture(TestUser user) {
        this.user = user;
        this.account = new AuthAccount(user);
        this.profile = new ApiProfile(user.username());
        this.friendships = new Friendships(user.username());
    }

    @Override
    public TestUser user() {
        return user;
    }

    @Override
    public void create() {
        account.create();
        profile.create();
    }

    @Override
    public void close() {
        new TearDown().with(friendships::delete).with(profile::delete).with(account::delete).close();
    }
}
