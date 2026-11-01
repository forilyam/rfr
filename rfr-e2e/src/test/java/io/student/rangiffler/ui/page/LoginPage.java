package io.student.rangiffler.ui.page;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;
import io.student.rangiffler.config.TestConfig;
import io.student.rangiffler.ui.component.CredentialsForm;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public final class LoginPage {
    public static final String URL = TestConfig.authUrl() + "/login";
    private final Page page;
    private final CredentialsForm credentials;
    private final Locator signIn;
    private final Locator error;

    public LoginPage(Page page) {
        this.page = page;
        this.credentials = new CredentialsForm(page);
        this.signIn = page.getByRole(AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("Sign in").setExact(true));
        this.error = page.locator(".form__error");
    }

    @Step("Заполнить форму входа: {username} / {password}")
    public LoginPage fillLoginForm(String username, String password) {
        credentials.fillCredentials(username, password);
        return this;
    }

    @Step("Нажать Sign in")
    public MainPage clickSignIn() {
        signIn.click();
        page.waitForURL(MainPage.URL);
        return new MainPage(page);
    }

    @Step("Нажать Sign in с неверными учётными данными")
    public LoginPage clickSignInWithInvalidCredentials() {
        signIn.click();
        return this;
    }

    @Step("Проверить ошибку входа: {message}")
    public LoginPage checkError(String message) {
        assertThat(error).hasText(message);
        return this;
    }
}
