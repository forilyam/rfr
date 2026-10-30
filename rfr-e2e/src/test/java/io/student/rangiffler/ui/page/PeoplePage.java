package io.student.rangiffler.ui.page;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;
import io.student.rangiffler.config.TestConfig;
import io.student.rangiffler.ui.component.HeaderComponent;

import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public final class PeoplePage {
    public static final String URL = TestConfig.frontUrl() + "/people";
    private final Page page;
    private final Locator panel;
    private final Locator tabs;
    private final HeaderComponent header;

    public PeoplePage(Page page) {
        this.page = page;
        this.panel = page.getByRole(AriaRole.TABPANEL);
        this.tabs = page.getByRole(AriaRole.TABLIST,
                new Page.GetByRoleOptions().setName("People tabs").setExact(true));
        this.header = new HeaderComponent(page);
    }

    @Step("Открыть список людей")
    public PeoplePage open() {
        page.waitForResponse(response -> operation(response, "GetFriends"), () -> page.navigate(URL)).finished();
        return this;
    }

    @Step("Открыть список друзей")
    public PeoplePage clickFriends() {
        clickTab("Friends", "GetFriends");
        return this;
    }

    @Step("Открыть список всех людей")
    public PeoplePage clickAllPeople() {
        tabs.getByRole(AriaRole.TAB, new Locator.GetByRoleOptions().setName("All People").setExact(true)).click();
        return this;
    }

    @Step("Открыть входящие приглашения")
    public PeoplePage clickIncomeInvitations() {
        clickTab("Income invitations", "GetInvitations");
        return this;
    }

    @Step("Открыть исходящие приглашения")
    public PeoplePage clickOutcomeInvitations() {
        clickTab("Outcome invitations", "GetOutcomeInvitations");
        return this;
    }

    @Step("Найти пользователя: {username}")
    public PeoplePage searchPeople(String username) {
        panel.getByRole(AriaRole.TEXTBOX,
                new Locator.GetByRoleOptions().setName("search people").setExact(true)).fill(username);
        panel.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("search").setExact(true)).click();
        return this;
    }

    @Step("Отправить приглашение пользователю: {username}")
    public PeoplePage clickAdd(String username) {
        page.waitForResponse(response -> response.url().endsWith("/graphql")
                        && response.request().method().equals("POST")
                        && Pattern.compile("\"operationName\"\\s*:\\s*\"FriendshipAction\"")
                        .matcher(Objects.toString(response.request().postData(), "")).find(),
                () -> row(username).getByRole(AriaRole.BUTTON,
                        new Locator.GetByRoleOptions().setName("Add").setExact(true)).click()).finished();
        return this;
    }

    @Step("Принять приглашение пользователя: {username}")
    public PeoplePage clickAccept(String username) {
        page.waitForResponse(response -> response.url().endsWith("/graphql")
                        && response.request().method().equals("POST")
                        && Pattern.compile("\"operationName\"\\s*:\\s*\"FriendshipAction\"")
                        .matcher(Objects.toString(response.request().postData(), "")).find(),
                () -> row(username).getByRole(AriaRole.BUTTON,
                        new Locator.GetByRoleOptions().setName("Accept").setExact(true)).click()).finished();
        return this;
    }

    @Step("Проверить число людей в списке: {count}")
    public PeoplePage checkPeopleCount(int count) {
        assertThat(panel.getByRole(AriaRole.ROW).filter(
                new Locator.FilterOptions().setHas(page.getByRole(AriaRole.CELL)))).hasCount(count);
        return this;
    }

    @Step("Проверить пустой список людей")
    public PeoplePage checkNoPeople() {
        assertThat(panel.getByText("There are no users yet", new Locator.GetByTextOptions().setExact(true))).isVisible();
        return this;
    }

    @Step("Проверить наличие пользователя в списке: {username}")
    public PeoplePage checkUser(String username) {
        assertThat(row(username)).isVisible();
        return this;
    }

    @Step("Проверить отсутствие пользователя в списке: {username}")
    public PeoplePage checkNoUser(String username) {
        assertThat(row(username)).hasCount(0);
        return this;
    }

    @Step("Проверить кнопку Remove у друга: {username}")
    public PeoplePage checkFriendActions(String username) {
        assertThat(row(username).getByRole(AriaRole.BUTTON,
                new Locator.GetByRoleOptions().setName("Remove").setExact(true))).isVisible();
        return this;
    }

    @Step("Проверить кнопки Accept и Decline у отправителя приглашения: {username}")
    public PeoplePage checkIncomeInvitationActions(String username) {
        assertThat(row(username).getByRole(AriaRole.BUTTON,
                new Locator.GetByRoleOptions().setName("Accept").setExact(true))).isVisible();
        assertThat(row(username).getByRole(AriaRole.BUTTON,
                new Locator.GetByRoleOptions().setName("Decline").setExact(true))).isVisible();
        return this;
    }

    @Step("Проверить статус Waiting и кнопку Cancel у приглашённого: {username}")
    public PeoplePage checkOutcomeInvitationActions(String username) {
        assertThat(row(username).getByText("Waiting...", new Locator.GetByTextOptions().setExact(true))).isVisible();
        assertThat(row(username).getByRole(AriaRole.BUTTON,
                new Locator.GetByRoleOptions().setName("Cancel").setExact(true))).isVisible();
        return this;
    }

    @Step("Проверить пользователей в списке: {usernames}")
    public PeoplePage checkUsers(List<String> usernames) {
        checkPeopleCount(usernames.size());
        usernames.forEach(this::checkUser);
        return this;
    }

    @Step("Выйти из учётной записи")
    public LandingPage clickLogout() {
        return header.clickLogout();
    }

    private Locator row(String username) {
        return panel.getByRole(AriaRole.ROW).filter(new Locator.FilterOptions().setHas(
                page.getByRole(AriaRole.CELL, new Page.GetByRoleOptions().setName(username).setExact(true))));
    }

    private void clickTab(String name, String query) {
        Locator tab = tabs.getByRole(AriaRole.TAB, new Locator.GetByRoleOptions().setName(name).setExact(true));
        if ("true".equals(tab.getAttribute("aria-selected"))) {
            tab.click();
        } else {
            page.waitForResponse(response -> operation(response, query), tab::click).finished();
        }
    }

    private boolean operation(Response response, String name) {
        return response.url().endsWith("/graphql") && response.request().method().equals("POST")
                && Pattern.compile("\"operationName\"\\s*:\\s*\"" + Pattern.quote(name) + "\"")
                .matcher(Objects.toString(response.request().postData(), "")).find();
    }
}
