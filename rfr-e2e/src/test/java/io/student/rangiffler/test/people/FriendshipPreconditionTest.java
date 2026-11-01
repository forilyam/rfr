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
@DisplayName("Дружба и приглашения с SQL-предусловиями")
final class FriendshipPreconditionTest {
    @Test
    @DisplayName("Подготовленные друзья отображаются в интерфейсе")
    void requestedFriendsAppearInUi(Page page, @User(friends = 3) TestUser user) {
        login(page, user).checkPeopleCount(3)
                .checkUsers(user.friends().stream().map(TestUser::username).toList());
    }

    @Test
    @DisplayName("Подготовленные входящие приглашения отображаются в интерфейсе")
    void requestedIncomeInvitationsAppearInUi(Page page, @User(incomeInvitations = 2) TestUser user) {
        login(page, user).clickIncomeInvitations().checkPeopleCount(2)
                .checkUsers(user.incomeInvitations().stream().map(TestUser::username).toList());
    }

    @Test
    @DisplayName("Друзья и входящие приглашения создаются одновременно")
    void requestedFriendsAndInvitationsAppearInUi(Page page,
                                                  @User(friends = 2, incomeInvitations = 3) TestUser user) {
        login(page, user).checkPeopleCount(2)
                .checkUsers(user.friends().stream().map(TestUser::username).toList())
                .clickIncomeInvitations().checkPeopleCount(3)
                .checkUsers(user.incomeInvitations().stream().map(TestUser::username).toList());
    }

    @Test
    @DisplayName("Пользователь без связей отправляет приглашение через интерфейс")
    void emptyUserCanSendInvitationInUi(Page page, @User TestUser user, @User TestUser peer) {
        invite(login(page, user).checkNoPeople().clickIncomeInvitations().checkNoPeople(), peer.username());
    }

    @Test
    @DisplayName("Пользователи без связей становятся друзьями через интерфейс")
    void emptyUsersCanBecomeFriendsInUi(Page page, @User TestUser user, @User TestUser peer) {
        invite(login(page, user).checkNoPeople(), peer.username()).clickLogout();
        login(page, peer).clickIncomeInvitations().checkUser(user.username())
                .clickAccept(user.username()).clickFriends().checkUser(user.username());
    }

    private PeoplePage login(Page page, TestUser user) {
        new LandingPage(page).open().clickLogin()
                .fillLoginForm(user.username(), user.password()).clickSignIn();
        return new PeoplePage(page).open();
    }

    private PeoplePage invite(PeoplePage people, String username) {
        return people.clickAllPeople().searchPeople(username).clickAdd(username)
                .clickOutcomeInvitations().checkUser(username);
    }
}
