package io.student.rangiffler.test.travels;

import com.microsoft.playwright.Page;
import io.student.rangiffler.data.TestUser;
import io.student.rangiffler.jupiter.annotation.UiTest;
import io.student.rangiffler.jupiter.annotation.User;
import io.student.rangiffler.ui.page.LandingPage;
import io.student.rangiffler.ui.page.MyTravelsPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@UiTest
@DisplayName("Мои путешествия")
final class MyTravelsPageTest {
    @Test
    @DisplayName("Лента моих путешествий содержит четыре фотографии из моков")
    void ownFeedShowsFourPhotos(Page page, @User TestUser user) {
        login(page, user).checkPhotoCount(4);
    }

    @Test
    @DisplayName("Лента с друзьями содержит семь фотографий из моков")
    void friendsFeedShowsSevenPhotos(Page page, @User TestUser user) {
        login(page, user).clickWithFriends().checkPhotoCount(7);
    }

    @Test
    @DisplayName("Возврат к своим путешествиям исключает фотографии друзей")
    void ownFeedDoesntKeepFriendsPhotos(Page page, @User TestUser user) {
        login(page, user).clickWithFriends().clickOnlyMine().checkPhotoCount(4);
    }

    @Test
    @DisplayName("Счётчик показывает четыре посещённые страны")
    void visitedCounterShowsFourCountries(Page page, @User TestUser user) {
        login(page, user).checkVisitedCountriesCount(4);
    }

    @Test
    @DisplayName("Выбор страны в списке оставляет только её фотографию")
    void countrySelectionDoesntKeepOtherCountriesPhotos(Page page, @User TestUser user) {
        login(page, user).clickVisitedCountries().clickCountry("Netherlands").checkPhotoCount(1);
    }

    @Test
    @DisplayName("Чип показывает страну, выбранную в списке")
    void countryFilterShowsSelectedCountry(Page page, @User TestUser user) {
        login(page, user).clickVisitedCountries().clickCountry("Netherlands").checkCountryFilter("Netherlands");
    }

    @Test
    @DisplayName("Сброс фильтра возвращает все фотографии")
    void resetCountryFilterDoesntHideOtherCountriesPhotos(Page page, @User TestUser user) {
        login(page, user).clickVisitedCountries().clickCountry("Netherlands")
                .clickResetCountry("Netherlands").checkPhotoCount(4);
    }

    private MyTravelsPage login(Page page, TestUser user) {
        new LandingPage(page).open().clickLogin()
                .fillLoginForm(user.username(), user.password()).clickSignIn();
        return new MyTravelsPage(page);
    }
}
