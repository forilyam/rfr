package io.student.rangiffler.ui.page;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.FilePayload;
import io.qameta.allure.Step;
import io.student.rangiffler.config.TestConfig;

import java.util.Objects;
import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public final class ProfilePage {
    public static final String URL = TestConfig.frontUrl() + "/profile";
    private final Page page;
    private final Locator usernameInput;
    private final Locator firstname;
    private final Locator surname;
    private final Locator location;
    private final Locator upload;
    private final Locator avatar;
    private final Locator save;
    private final Locator reset;

    public ProfilePage(Page page) {
        this.page = page;
        this.usernameInput = page.getByLabel("Username", new Page.GetByLabelOptions().setExact(true));
        this.firstname = page.getByLabel("First Name", new Page.GetByLabelOptions().setExact(true));
        this.surname = page.getByLabel("Surname", new Page.GetByLabelOptions().setExact(true));
        this.location = page.getByRole(AriaRole.COMBOBOX,
                new Page.GetByRoleOptions().setName("Location").setExact(true));
        this.upload = page.getByLabel("Update avatar", new Page.GetByLabelOptions().setExact(true));
        this.avatar = page.locator("main img[src^='data:image/']:not([alt])");
        this.save = page.getByRole(AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("Save").setExact(true));
        this.reset = page.getByRole(AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("Reset").setExact(true));
    }

    @Step("Открыть профиль")
    public ProfilePage open() {
        page.navigate(URL);
        return this;
    }

    @Step("Перезагрузить страницу профиля")
    public ProfilePage reload() {
        page.reload();
        return this;
    }

    @Step("Заполнить профиль: {firstname}, {surname}, {country}")
    public ProfilePage fillProfileForm(String firstname, String surname, String country) {
        this.firstname.fill(firstname);
        this.surname.fill(surname);
        location.click();
        page.getByRole(AriaRole.OPTION).filter(new Locator.FilterOptions().setHas(
                page.getByRole(AriaRole.IMG, new Page.GetByRoleOptions().setName(country).setExact(true))
        )).click();
        return this;
    }

    @Step("Загрузить аватар: {file}")
    public ProfilePage uploadAvatar(FilePayload file) {
        upload.setInputFiles(file);
        return this;
    }

    @Step("Сохранить профиль")
    public ProfilePage clickSave() {
        page.waitForResponse(response -> response.url().endsWith("/graphql")
                && response.request().method().equals("POST")
                && Pattern.compile("\"operationName\"\\s*:\\s*\"UpdateUser\"")
                .matcher(Objects.toString(response.request().postData(), "")).find(), save::click).finished();
        return this;
    }

    @Step("Сбросить изменения профиля")
    public ProfilePage clickReset() {
        reset.click();
        return this;
    }

    @Step("Проверить аватар профиля: {source}")
    public ProfilePage checkAvatar(String source) {
        assertThat(avatar).isVisible();
        assertThat(avatar).hasAttribute("src", source);
        return this;
    }

    @Step("Проверить профиль: {firstname}, {surname}, {country}, {source}")
    public ProfilePage checkProfileForm(String firstname, String surname, String country, String source) {
        assertThat(this.firstname).hasValue(firstname);
        assertThat(this.surname).hasValue(surname);
        checkLocation(country);
        checkAvatar(source);
        return this;
    }

    @Step("Проверить имя пользователя в профиле: {username}")
    public ProfilePage checkUsername(String username) {
        assertThat(usernameInput).hasValue(username);
        return this;
    }

    @Step("Проверить страну профиля: {country}")
    public ProfilePage checkLocation(String country) {
        assertThat(location).hasText(country);
        return this;
    }
}
