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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

@UiTest
@DisplayName("Карточка фотографии")
final class PhotoCardTest {
    @ParameterizedTest
    @CsvSource({
            "Амстердам!, Netherlands Амстердам!",
            "Париж!, France Париж!",
            "Петербург!, Russian Federation Петербург!",
            "Нью-Йорк!, United States Нью-Йорк!",
            "Старая Прага завораживает!, Czech Republic Старая Прага завораживает!",
            "Мюнхен - мой любимый город!, Germany Мюнхен - мой любимый город!",
            "Обожаю Таиланд!, Thailand Обожаю Таиланд!"
    })
    @DisplayName("Каждая из семи карточек имеет собственные страну и описание в доступном имени")
    void cardNameDoesntUseAnotherPhotosLabels(String description, String name, Page page, @User TestUser user) {
        login(page, user).clickWithFriends().photo(description).checkName(name);
    }

    @Test
    @DisplayName("Лайк относится к выбранной карточке среди нескольких фотографий")
    void likeDoesntTargetAnotherPhoto(Page page, @User TestUser user) {
        login(page, user).clickWithFriends().photo("Амстердам!").clickLike().checkLikes(1);
    }

    @Test
    @DisplayName("Действия выбранной фотографии открывают меню Edit и Delete")
    void actionsDontTargetAnotherPhoto(Page page, @User TestUser user) {
        login(page, user).clickWithFriends().photo("Париж!").clickActions().checkActions(List.of("Edit", "Delete"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Старая Прага завораживает!", "Мюнхен - мой любимый город!", "Обожаю Таиланд!"})
    @DisplayName("На карточках друзей нет действий владельца")
    void friendsPhotosDontOfferOwnerActions(String description, Page page, @User TestUser user) {
        login(page, user).clickWithFriends().photo(description).checkOnlyLikeButton();
    }

    private MyTravelsPage login(Page page, TestUser user) {
        new LandingPage(page).open().clickLogin()
                .fillLoginForm(user.username(), user.password()).clickSignIn();
        return new MyTravelsPage(page);
    }
}
