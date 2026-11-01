package io.student.rangiffler.data.db;

import io.student.rangiffler.data.SingleUserFixture;
import io.student.rangiffler.data.TestUser;
import io.student.rangiffler.data.UserFixture;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static io.student.rangiffler.data.db.FriendshipStatus.ACCEPTED;
import static io.student.rangiffler.data.db.FriendshipStatus.PENDING;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@Timeout(20)
final class FriendshipTest {
    @ParameterizedTest
    @EnumSource(FriendshipStatus.class)
    void relationshipRetainsItsStatusAndDirection(FriendshipStatus status) {
        try (UserFixture origin = new SingleUserFixture(TestUser.unique());
             UserFixture invitee = new SingleUserFixture(TestUser.unique())) {
            origin.create();
            invitee.create();
            new Friendship(origin.user().username(), invitee.user().username(), status).create();
            assertThat("A relationship has the wrong status or direction", List.of(
                            new Friendship(origin.user().username(), invitee.user().username(), status).exist(),
                            new Friendship(invitee.user().username(), origin.user().username(), status).exist(),
                            new Friendship(origin.user().username(), invitee.user().username(),
                                    status == ACCEPTED ? PENDING : ACCEPTED).exist()),
                    is(List.of(true, status == ACCEPTED, false)));
        }
    }

    @Test
    void deletingOutgoingInvitationDoesntDeleteIncomingOrUnrelatedInvitations() {
        try (UserFixture origin = new SingleUserFixture(TestUser.unique());
             UserFixture invitee = new SingleUserFixture(TestUser.unique());
             UserFixture stranger = new SingleUserFixture(TestUser.unique())) {
            origin.create();
            invitee.create();
            stranger.create();
            Friendship invitation = new Friendship(origin.user().username(), invitee.user().username(), PENDING);
            invitation.create();
            new Friendship(stranger.user().username(), origin.user().username(), PENDING).create();
            new Friendship(stranger.user().username(), invitee.user().username(), PENDING).create();
            invitation.delete();
            assertThat("Deleting an outgoing invitation leaves it or deletes another invitation", List.of(
                            invitation.exist(),
                            new Friendship(stranger.user().username(), origin.user().username(), PENDING).exist(),
                            new Friendship(stranger.user().username(), invitee.user().username(), PENDING).exist()),
                    is(List.of(false, true, true)));
        }
    }
}
