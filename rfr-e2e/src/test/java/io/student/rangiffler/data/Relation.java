package io.student.rangiffler.data;

import io.student.rangiffler.data.db.Friendship;
import io.student.rangiffler.data.db.FriendshipStatus;

import java.util.List;
import java.util.function.BiFunction;

import static io.student.rangiffler.data.db.FriendshipStatus.ACCEPTED;
import static io.student.rangiffler.data.db.FriendshipStatus.PENDING;

public enum Relation {
    FRIEND(ACCEPTED, false, TestUser::withFriends),
    INCOME_INVITATION(PENDING, true, TestUser::withIncomeInvitations),
    OUTCOME_INVITATION(PENDING, false, TestUser::withOutcomeInvitations);

    private final FriendshipStatus status;
    private final boolean incoming;
    private final BiFunction<TestUser, List<TestUser>, TestUser> projection;

    Relation(FriendshipStatus status, boolean incoming, BiFunction<TestUser, List<TestUser>, TestUser> projection) {
        this.status = status;
        this.incoming = incoming;
        this.projection = projection;
    }

    TestUser user(TestUser user, List<TestUser> partners) {
        return projection.apply(user, partners);
    }

    Friendship friendship(TestUser user, TestUser partner) {
        return new Friendship(incoming ? partner.username() : user.username(),
                incoming ? user.username() : partner.username(), status);
    }
}
