package io.student.rangiffler.data;

import io.student.rangiffler.data.db.Friendships;
import io.student.rangiffler.data.db.Invitation;

import java.util.List;
import java.util.stream.IntStream;

public final class UserWithIncomeInvitations implements UserFixture {
    private final UserFixture origin;
    private final List<UserFixture> senders;
    private final Friendships friendships;

    public UserWithIncomeInvitations(UserFixture origin, int count) {
        this.origin = origin;
        this.senders = IntStream.range(0, count)
                .<UserFixture>mapToObj(index -> new SingleUserFixture(TestUser.unique())).toList();
        this.friendships = new Friendships(origin.user().username());
    }

    @Override
    public TestUser user() {
        return origin.user().withIncomeInvitations(senders.stream().map(UserFixture::user).toList());
    }

    @Override
    public void create() {
        origin.create();
        for (UserFixture sender : senders) {
            sender.create();
            new Invitation(sender.user().username(), user().username()).create();
        }
    }

    @Override
    public void close() {
        new TearDown().with(friendships::delete).withEach(senders, UserFixture::close).with(origin::close).close();
    }
}
