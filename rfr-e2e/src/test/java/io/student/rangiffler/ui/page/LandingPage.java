package io.student.rangiffler.ui.page;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;
import io.student.rangiffler.config.TestConfig;

public final class LandingPage {
  public static final String URL = TestConfig.frontUrl() + "/";
  private final Page page;
  private final Locator login;
  private final Locator register;

  public LandingPage(Page page) {
    this.page = page;
    this.login = page.getByRole(AriaRole.BUTTON,
        new Page.GetByRoleOptions().setName("Login").setExact(true));
    this.register = page.getByRole(AriaRole.LINK,
        new Page.GetByRoleOptions().setName("Register").setExact(true));
  }

  @Step("Открыть гостевую страницу")
  public LandingPage open() {
    page.navigate(URL);
    return this;
  }

  @Step("Нажать Register")
  public RegistrationPage clickRegister() {
    register.click();
    return new RegistrationPage(page);
  }

  @Step("Нажать Login")
  public LoginPage clickLogin() {
    login.click();
    return new LoginPage(page);
  }
}
