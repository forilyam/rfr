package io.student.rangiffler.ui.page;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;
import io.student.rangiffler.config.TestConfig;
import io.student.rangiffler.ui.component.HeaderComponent;
import io.student.rangiffler.ui.component.PeopleTable;

import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

public final class PeoplePage {
    public static final String URL = TestConfig.frontUrl() + "/people";
    private final Page page;
    private final Locator tabs;
    private final HeaderComponent header;
    private final PeopleTable table;

    public PeoplePage(Page page) {
        this.page = page;
        this.tabs = page.getByRole(AriaRole.TABLIST,
                new Page.GetByRoleOptions().setName("People tabs").setExact(true));
        this.header = new HeaderComponent(page);
        this.table = new PeopleTable(page);
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
        page.getByRole(AriaRole.TABPANEL).getByRole(AriaRole.TEXTBOX,
                new Locator.GetByRoleOptions().setName("search people").setExact(true)).fill(username);
        page.getByRole(AriaRole.TABPANEL).getByRole(AriaRole.BUTTON,
                new Locator.GetByRoleOptions().setName("search").setExact(true)).click();
        return this;
    }

    @Step("Отправить приглашение пользователю: {username}")
    public PeoplePage clickAdd(String username) {
        table.row(username).clickAdd();
        return this;
    }

    @Step("Принять приглашение пользователя: {username}")
    public PeoplePage clickAccept(String username) {
        table.row(username).clickAccept();
        return this;
    }

    @Step("Проверить число людей в списке: {count}")
    public PeoplePage checkPeopleCount(int count) {
        table.checkPeopleCount(count);
        return this;
    }

    @Step("Проверить пустой список людей")
    public PeoplePage checkNoPeople() {
        table.checkNoPeople();
        return this;
    }

    @Step("Проверить наличие пользователя в списке: {username}")
    public PeoplePage checkUser(String username) {
        table.row(username).checkThatVisible();
        return this;
    }

    @Step("Проверить отсутствие пользователя в списке: {username}")
    public PeoplePage checkNoUser(String username) {
        table.row(username).checkThatAbsent();
        return this;
    }

    @Step("Проверить кнопку Remove у друга: {username}")
    public PeoplePage checkFriendActions(String username) {
        table.row(username).checkFriendActions();
        return this;
    }

    @Step("Проверить кнопки Accept и Decline у отправителя приглашения: {username}")
    public PeoplePage checkIncomeInvitationActions(String username) {
        table.row(username).checkIncomeInvitationActions();
        return this;
    }

    @Step("Проверить статус Waiting и кнопку Cancel у приглашённого: {username}")
    public PeoplePage checkOutcomeInvitationActions(String username) {
        table.row(username).checkOutcomeInvitationActions();
        return this;
    }

    @Step("Проверить пользователей в списке: {usernames}")
    public PeoplePage checkUsers(List<String> usernames) {
        table.checkUsers(usernames);
        return this;
    }

    @Step("Выйти из учётной записи")
    public LandingPage clickLogout() {
        return header.clickLogout();
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
