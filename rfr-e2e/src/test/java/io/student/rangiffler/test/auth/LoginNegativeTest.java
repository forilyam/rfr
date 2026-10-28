package io.student.rangiffler.test.auth;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.junit.UsePlaywright;
import io.student.rangiffler.data.TestUser;
import io.student.rangiffler.jupiter.annotation.User;
import io.student.rangiffler.ui.page.LandingPage;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@UsePlaywright
@DisplayName("Отказ во входе")
class LoginNegativeTest {
  @Test
  @DisplayName("Неверный пароль приводит к Bad credentials")
  void userCannotLoginWithWrongPassword(Page page, @User TestUser user) {
    page.setExtraHTTPHeaders(Map.of("Accept-Language", "en-US"));
    new LandingPage(page).open()
        .clickLogin()
        .fillLoginForm(user.username(), user.password() + "_incorrect")
        .clickSignInWithInvalidCredentials()
        .checkError("Bad credentials");
  }

  @Test
  @DisplayName("Несуществующий пользователь получает Bad credentials")
  void nonexistentUserCannotLogin(Page page) {
    page.setExtraHTTPHeaders(Map.of("Accept-Language", "en-US"));
    new LandingPage(page).open()
        .clickLogin()
        .fillLoginForm("missing_" + UUID.randomUUID(), "Unknown_73!password")
        .clickSignInWithInvalidCredentials()
        .checkError("Bad credentials");
  }
}
