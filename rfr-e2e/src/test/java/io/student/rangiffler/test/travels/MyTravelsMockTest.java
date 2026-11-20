package io.student.rangiffler.test.travels;

import com.microsoft.playwright.Page;
import io.student.rangiffler.data.TestUser;
import io.student.rangiffler.jupiter.annotation.UiTest;
import io.student.rangiffler.jupiter.annotation.User;
import io.student.rangiffler.ui.page.LandingPage;
import io.student.rangiffler.ui.page.MyTravelsPage;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@UiTest
@DisplayName("Мои путешествия на mock-ленте")
final class MyTravelsMockTest {
    @Test
    @DisplayName("Переключение ленты меняет число карточек и посещённых стран на mock-ленте")
    void switchingFeedDoesntKeepPreviousPhotoAndCountryCounts(Page page, @User TestUser user) {
        login(page, user).checkPhotoCount(4).checkVisitedCountriesCount(4)
                .clickWithFriends().checkPhotoCount(7).checkVisitedCountriesCount(7)
                .clickOnlyMine().checkPhotoCount(4).checkVisitedCountriesCount(4);
    }

    @Test
    @DisplayName("Фильтр страны показывает чип и одну карточку, сброс возвращает все карточки на mock-ленте")
    void resettingCountryFilterDoesntKeepPhotosHidden(Page page, @User TestUser user) {
        login(page, user).clickWithFriends().checkPhotoCount(7)
                .clickVisitedCountries().clickCountry("Netherlands")
                .checkCountryFilter("Netherlands").checkPhotoCount(1)
                .clickResetCountry("Netherlands").checkPhotoCount(7);
    }

    @Test
    @DisplayName("Меню Photo actions доступно только у своих фотографий на mock-ленте")
    void friendsPhotosDontOfferOwnerActions(Page page, @User TestUser user) {
        MyTravelsPage travels = login(page, user).clickWithFriends().checkPhotoCount(7);
        travels.photo("Старая Прага завораживает!").checkOnlyLikeButton();
        travels.photo("Мюнхен - мой любимый город!").checkOnlyLikeButton();
        travels.photo("Обожаю Таиланд!").checkOnlyLikeButton();
        travels.photo("Париж!").clickActions().checkActions(List.of("Edit", "Delete"));
    }

    private MyTravelsPage login(Page page, TestUser user) {
        new LandingPage(page).open().clickLogin()
                .fillLoginForm(user.username(), user.password()).clickSignIn();
        return new MyTravelsPage(page);
    }
}
