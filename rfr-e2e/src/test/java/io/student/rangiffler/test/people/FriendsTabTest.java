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
@DisplayName("Вкладка Friends")
final class FriendsTabTest {
    @Test
    @DisplayName("Показан друг с кнопкой Remove, пользователи с приглашениями не показаны")
    void friendsTabShowsPreparedFriendWithRemove(Page page,
            @User(friends = 1, incomeInvitations = 1, outcomeInvitations = 1) TestUser user) {
        login(page, user).clickFriends()
                .checkUsers(user.friends().stream().map(TestUser::username).toList())
                .checkFriendActions(user.friends().getFirst().username())
                .checkNoUser(user.incomeInvitations().getFirst().username())
                .checkNoUser(user.outcomeInvitations().getFirst().username());
    }

    @Test
    @DisplayName("Чужая дружба не попадает в список друзей")
    void friendsTabDoesntShowOtherUsersFriendship(Page page, @User TestUser user,
                                                 @User(friends = 1) TestUser stranger) {
        login(page, user).clickFriends().checkNoPeople()
                .checkNoUser(stranger.username()).checkNoUser(stranger.friends().getFirst().username());
    }

    @Test
    @DisplayName("Без друзей показано пустое состояние")
    void friendsTabShowsEmptyStateWithoutFriends(Page page, @User TestUser user) {
        login(page, user).clickFriends().checkNoPeople();
    }

    private PeoplePage login(Page page, TestUser user) {
        new LandingPage(page).open().clickLogin()
                .fillLoginForm(user.username(), user.password()).clickSignIn();
        return new PeoplePage(page).open();
    }
}
