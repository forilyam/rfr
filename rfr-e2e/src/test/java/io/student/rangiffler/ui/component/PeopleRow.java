package io.student.rangiffler.ui.component;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;

import java.util.Objects;
import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public final class PeopleRow {
    private final Page page;
    private final Locator row;

    public PeopleRow(Page page, Locator row) {
        this.page = page;
        this.row = row;
    }

    @Step("Отправить приглашение пользователю из строки")
    public PeopleRow clickAdd() {
        click("Add");
        return this;
    }

    @Step("Принять приглашение пользователя из строки")
    public PeopleRow clickAccept() {
        click("Accept");
        return this;
    }

    @Step("Проверить наличие строки пользователя")
    public PeopleRow checkThatVisible() {
        assertThat(row).isVisible();
        return this;
    }

    @Step("Проверить отсутствие строки пользователя")
    public PeopleRow checkThatAbsent() {
        assertThat(row).hasCount(0);
        return this;
    }

    @Step("Проверить кнопку Remove в строке друга")
    public PeopleRow checkFriendActions() {
        assertThat(row.getByRole(AriaRole.BUTTON,
                new Locator.GetByRoleOptions().setName("Remove").setExact(true))).isVisible();
        return this;
    }

    @Step("Проверить кнопки Accept и Decline в строке отправителя приглашения")
    public PeopleRow checkIncomeInvitationActions() {
        assertThat(row.getByRole(AriaRole.BUTTON,
                new Locator.GetByRoleOptions().setName("Accept").setExact(true))).isVisible();
        assertThat(row.getByRole(AriaRole.BUTTON,
                new Locator.GetByRoleOptions().setName("Decline").setExact(true))).isVisible();
        return this;
    }

    @Step("Проверить статус Waiting и кнопку Cancel в строке приглашённого")
    public PeopleRow checkOutcomeInvitationActions() {
        assertThat(row.getByText("Waiting...", new Locator.GetByTextOptions().setExact(true))).isVisible();
        assertThat(row.getByRole(AriaRole.BUTTON,
                new Locator.GetByRoleOptions().setName("Cancel").setExact(true))).isVisible();
        return this;
    }

    private void click(String name) {
        page.waitForResponse(response -> response.url().endsWith("/graphql")
                        && response.request().method().equals("POST")
                        && Pattern.compile("\"operationName\"\\s*:\\s*\"FriendshipAction\"")
                        .matcher(Objects.toString(response.request().postData(), "")).find(),
                () -> row.getByRole(AriaRole.BUTTON,
                        new Locator.GetByRoleOptions().setName(name).setExact(true)).click()).finished();
    }
}
