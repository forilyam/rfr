package io.student.rangiffler.test.people;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.junit.UsePlaywright;
import io.student.rangiffler.data.TestUser;
import io.student.rangiffler.jupiter.annotation.User;
import io.student.rangiffler.ui.page.LandingPage;
import io.student.rangiffler.ui.page.PeoplePage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@UsePlaywright
@Timeout(90)
@DisplayName("Вкладка Outcome invitations")
final class OutcomeInvitationsTabTest {
    @Test
    @DisplayName("Показан приглашённый со статусом Waiting и кнопкой Cancel, друзья и отправители не показаны")
    void outcomeTabShowsPreparedInviteeWithWaitingAndCancel(Page page,
            @User(friends = 1, incomeInvitations = 1, outcomeInvitations = 1) TestUser user) {
        login(page, user).clickOutcomeInvitations()
                .checkUsers(user.outcomeInvitations().stream().map(TestUser::username).toList())
                .checkOutcomeInvitationActions(user.outcomeInvitations().getFirst().username())
                .checkNoUser(user.friends().getFirst().username())
                .checkNoUser(user.incomeInvitations().getFirst().username());
    }

    @Test
    @DisplayName("Чужое исходящее приглашение не попадает в список")
    void outcomeTabDoesntShowOtherUsersInvitation(Page page, @User TestUser user,
                                                 @User(outcomeInvitations = 1) TestUser stranger) {
        login(page, user).clickOutcomeInvitations().checkNoPeople()
                .checkNoUser(stranger.username()).checkNoUser(stranger.outcomeInvitations().getFirst().username());
    }

    @Test
    @DisplayName("Без исходящих приглашений показано пустое состояние")
    void outcomeTabShowsEmptyStateWithoutInvitations(Page page, @User TestUser user) {
        login(page, user).clickOutcomeInvitations().checkNoPeople();
    }

    private PeoplePage login(Page page, TestUser user) {
        new LandingPage(page).open().clickLogin()
                .fillLoginForm(user.username(), user.password()).clickSignIn();
        return new PeoplePage(page).open();
    }
}
