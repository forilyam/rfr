package io.student.rangiffler.data;

import io.student.rangiffler.data.db.Friendship;

import java.util.List;
import java.util.stream.IntStream;

public final class UserWithRelationsFixture implements UserFixture {
    private final UserFixture origin;
    private final Relation relation;
    private final List<UserFixture> partners;
    private final List<Friendship> friendships;

    public UserWithRelationsFixture(UserFixture origin, Relation relation, int count) {
        this.origin = origin;
        this.relation = relation;
        this.partners = IntStream.range(0, count)
                .<UserFixture>mapToObj(index -> new SingleUserFixture(TestUser.unique())).toList();
        this.friendships = partners.stream()
                .map(partner -> relation.friendship(origin.user(), partner.user())).toList();
    }

    @Override
    public TestUser user() {
        return relation.user(origin.user(), partners.stream().map(UserFixture::user).toList());
    }

    @Override
    public void create() {
        origin.create();
        for (int index = 0; index < partners.size(); index++) {
            partners.get(index).create();
            friendships.get(index).create();
        }
    }

    @Override
    public void close() {
        new TearDown().withEach(friendships, Friendship::delete)
                .withEach(partners, UserFixture::close).with(origin::close).close();
    }
}
