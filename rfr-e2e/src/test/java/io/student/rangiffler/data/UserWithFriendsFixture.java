package io.student.rangiffler.data;

import io.student.rangiffler.data.db.Friendship;
import io.student.rangiffler.data.db.Friendships;

import java.util.List;
import java.util.stream.IntStream;

public final class UserWithFriendsFixture implements UserFixture {
    private final UserFixture origin;
    private final List<UserFixture> friends;
    private final Friendships friendships;

    public UserWithFriendsFixture(UserFixture origin, int count) {
        this.origin = origin;
        this.friends = IntStream.range(0, count)
                .<UserFixture>mapToObj(index -> new SingleUserFixture(TestUser.unique())).toList();
        this.friendships = new Friendships(origin.user().username());
    }

    @Override
    public TestUser user() {
        return origin.user().withFriends(friends.stream().map(UserFixture::user).toList());
    }

    @Override
    public void create() {
        origin.create();
        for (UserFixture friend : friends) {
            friend.create();
            new Friendship(user().username(), friend.user().username()).create();
        }
    }

    @Override
    public void close() {
        new TearDown().with(friendships::delete).withEach(friends, UserFixture::close).with(origin::close).close();
    }
}
