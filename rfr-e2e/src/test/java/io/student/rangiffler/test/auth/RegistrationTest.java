package io.student.rangiffler.test.auth;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.junit.UsePlaywright;
import io.student.rangiffler.data.TestUser;
import io.student.rangiffler.data.UserFixture;
import io.student.rangiffler.ui.page.LandingPage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@UsePlaywright
@DisplayName("Регистрация через UI")
class RegistrationTest {
    private final TestUser user = TestUser.unique();

    @AfterEach
    void deleteRegisteredUser() {
        new UserFixture(user).close();
    }

    @Test
    @DisplayName("Новый пользователь регистрируется, входит и видит главную")
    void registeredUserCanLogin(Page page) {
        new LandingPage(page).open()
                .clickRegister()
                .fillRegistrationForm(user.username(), user.password(), user.password())
                .clickSignUp()
                .checkThatRegistrationSucceeded()
                .clickSignIn()
                .clickLogin()
                .fillLoginForm(user.username(), user.password())
                .clickSignIn()
                .checkThatMainElementsVisible()
                .clickProfile()
                .checkUsername(user.username());
    }
}
