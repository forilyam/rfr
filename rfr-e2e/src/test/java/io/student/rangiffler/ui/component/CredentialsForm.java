package io.student.rangiffler.ui.component;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import io.qameta.allure.Step;

public final class CredentialsForm {
  private final Locator username;
  private final Locator password;

  public CredentialsForm(Page page) {
    this.username = page.getByPlaceholder("Type your username");
    this.password = page.getByPlaceholder("Type your password");
  }

  @Step("Ввести логин и пароль: {username} / {password}")
  public CredentialsForm fillCredentials(String username, String password) {
    this.username.fill(username);
    this.password.fill(password);
    return this;
  }
}
