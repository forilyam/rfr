package io.student.rangiffler.ui.page;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;
import io.student.rangiffler.config.TestConfig;
import io.student.rangiffler.ui.component.CredentialsForm;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public final class RegistrationPage {
  public static final String URL = TestConfig.authUrl() + "/register";
  private final Page page;
  private final CredentialsForm credentials;
  private final Locator confirmation;
  private final Locator signUp;
  private final Locator success;
  private final Locator signIn;

  public RegistrationPage(Page page) {
    this.page = page;
    this.credentials = new CredentialsForm(page);
    this.confirmation = page.getByPlaceholder("Repeat your password");
    this.signUp = page.getByRole(AriaRole.BUTTON,
        new Page.GetByRoleOptions().setName("Sign up").setExact(true));
    this.success = page.getByText("Congratulations! You've registered!",
        new Page.GetByTextOptions().setExact(true));
    this.signIn = page.getByRole(AriaRole.LINK,
        new Page.GetByRoleOptions().setName("Sign in").setExact(true));
  }

  @Step("Заполнить форму регистрации: {username} / {password} / {passwordConfirmation}")
  public RegistrationPage fillRegistrationForm(String username, String password, String passwordConfirmation) {
    credentials.fillCredentials(username, password);
    confirmation.fill(passwordConfirmation);
    return this;
  }

  @Step("Нажать Sign up")
  public RegistrationPage clickSignUp() {
    signUp.click();
    return this;
  }

  @Step("Проверить успешную регистрацию")
  public RegistrationPage checkThatRegistrationSucceeded() {
    assertThat(success).isVisible();
    assertThat(signIn).isVisible();
    return this;
  }

  @Step("Нажать Sign in после регистрации")
  public LandingPage clickSignIn() {
    signIn.click();
    return new LandingPage(page);
  }
}
