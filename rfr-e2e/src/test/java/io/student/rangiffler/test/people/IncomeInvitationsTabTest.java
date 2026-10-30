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
@DisplayName("Вкладка Income invitations")
final class IncomeInvitationsTabTest {
    @Test
    @DisplayName("Показан отправитель с кнопками Accept и Decline, друзья и приглашённые не показаны")
    void incomeTabShowsPreparedSenderWithAcceptAndDecline(Page page,
            @User(friends = 1, incomeInvitations = 1, outcomeInvitations = 1) TestUser user) {
        login(page, user).clickIncomeInvitations()
                .checkUsers(user.incomeInvitations().stream().map(TestUser::username).toList())
                .checkIncomeInvitationActions(user.incomeInvitations().getFirst().username())
                .checkNoUser(user.friends().getFirst().username())
                .checkNoUser(user.outcomeInvitations().getFirst().username());
    }

    @Test
    @DisplayName("Чужое входящее приглашение не попадает в список")
    void incomeTabDoesntShowOtherUsersInvitation(Page page, @User TestUser user,
                                                @User(incomeInvitations = 1) TestUser stranger) {
        login(page, user).clickIncomeInvitations().checkNoPeople()
                .checkNoUser(stranger.username()).checkNoUser(stranger.incomeInvitations().getFirst().username());
    }

    @Test
    @DisplayName("Без входящих приглашений показано пустое состояние")
    void incomeTabShowsEmptyStateWithoutInvitations(Page page, @User TestUser user) {
        login(page, user).clickIncomeInvitations().checkNoPeople();
    }

    private PeoplePage login(Page page, TestUser user) {
        new LandingPage(page).open().clickLogin()
                .fillLoginForm(user.username(), user.password()).clickSignIn();
        return new PeoplePage(page).open();
    }
}
