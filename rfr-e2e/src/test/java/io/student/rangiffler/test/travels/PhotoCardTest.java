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
@DisplayName("Карточка фотографии")
final class PhotoCardTest {
    @Test
    @DisplayName("Доступное имя карточки включает её страну и описание")
    void cardNameDoesntUseAnotherPhotosLabels(Page page, @User TestUser user) {
        login(page, user).photo("Париж!").checkName("France Париж!");
    }

    @Test
    @DisplayName("Лайк относится к выбранной карточке среди нескольких фотографий")
    void likeDoesntTargetAnotherPhoto(Page page, @User TestUser user) {
        login(page, user).photo("Амстердам!").clickLike().checkLikes(1);
    }

    @Test
    @DisplayName("Действия выбранной фотографии открывают меню Edit и Delete")
    void actionsDontTargetAnotherPhoto(Page page, @User TestUser user) {
        login(page, user).photo("Париж!").clickActions().checkActions(List.of("Edit", "Delete"));
    }

    private MyTravelsPage login(Page page, TestUser user) {
        new LandingPage(page).open().clickLogin()
                .fillLoginForm(user.username(), user.password()).clickSignIn();
        return new MyTravelsPage(page);
    }
}
