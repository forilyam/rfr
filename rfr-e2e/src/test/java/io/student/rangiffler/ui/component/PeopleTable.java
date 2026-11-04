package io.student.rangiffler.ui.component;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;

import java.util.List;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public final class PeopleTable {
    private final Page page;
    private final Locator panel;
    private final Locator table;

    public PeopleTable(Page page) {
        this.page = page;
        this.panel = page.getByRole(AriaRole.TABPANEL);
        this.table = panel.getByRole(AriaRole.TABLE);
    }

    public PeopleRow row(String username) {
        return new PeopleRow(page, table.getByRole(AriaRole.ROW).filter(new Locator.FilterOptions().setHas(
                page.getByRole(AriaRole.CELL, new Page.GetByRoleOptions().setName(username).setExact(true)))));
    }

    @Step("Проверить число людей в таблице: {count}")
    public PeopleTable checkPeopleCount(int count) {
        assertThat(table.getByRole(AriaRole.ROW).filter(
                new Locator.FilterOptions().setHas(page.getByRole(AriaRole.CELL)))).hasCount(count);
        return this;
    }

    @Step("Проверить пустую таблицу людей")
    public PeopleTable checkNoPeople() {
        assertThat(panel.getByText("There are no users yet", new Locator.GetByTextOptions().setExact(true))).isVisible();
        return this;
    }

    @Step("Проверить пользователей в таблице: {usernames}")
    public PeopleTable checkUsers(List<String> usernames) {
        checkPeopleCount(usernames.size());
        usernames.forEach(username -> row(username).checkThatVisible());
        return this;
    }
}
