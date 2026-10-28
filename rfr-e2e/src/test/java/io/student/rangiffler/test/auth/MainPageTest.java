package io.student.rangiffler.test.auth;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.junit.UsePlaywright;
import io.student.rangiffler.data.TestUser;
import io.student.rangiffler.jupiter.annotation.User;
import io.student.rangiffler.ui.page.LandingPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@UsePlaywright
@DisplayName("Главная страница с SQL-предусловием")
class MainPageTest {
    @Test
    @DisplayName("Подготовленный пользователь входит через UI и видит главную")
    void preparedUserCanLogin(Page page, @User TestUser user) {
        new LandingPage(page).open()
                .clickLogin()
                .fillLoginForm(user.username(), user.password())
                .clickSignIn()
                .checkThatMainElementsVisible()
                .clickProfile()
                .checkUsername(user.username());
    }
}
