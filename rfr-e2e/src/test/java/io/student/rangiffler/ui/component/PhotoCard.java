package io.student.rangiffler.ui.component;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;

import java.util.List;
import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public final class PhotoCard {
    private final Locator card;

    public PhotoCard(Locator cards, String description) {
        this.card = cards.filter(new Locator.FilterOptions().setHasText(description));
    }

    @Step("Открыть действия с фотографией")
    public PhotoCard clickActions() {
        card.getByRole(AriaRole.BUTTON,
                new Locator.GetByRoleOptions().setName("Photo actions").setExact(true)).click();
        return this;
    }

    @Step("Поставить лайк фотографии")
    public PhotoCard clickLike() {
        card.getByRole(AriaRole.BUTTON,
                new Locator.GetByRoleOptions().setName("like").setExact(true)).click();
        return this;
    }

    @Step("Проверить доступное имя фотокарточки: {name}")
    public PhotoCard checkName(String name) {
        assertThat(card).hasAccessibleName(name);
        return this;
    }

    @Step("Проверить число лайков фотографии: {count}")
    public PhotoCard checkLikes(int count) {
        assertThat(card.getByText(Pattern.compile("^\\d+ likes?$")))
                .hasText(count + (count == 1 ? " like" : " likes"));
        return this;
    }

    @Step("Проверить, что у фотографии друга доступен только лайк")
    public PhotoCard checkOnlyLikeButton() {
        assertThat(card.getByRole(AriaRole.BUTTON)).hasAccessibleName("like");
        return this;
    }

    @Step("Проверить действия с фотографией: {actions}")
    public PhotoCard checkActions(List<String> actions) {
        assertThat(card.page().getByRole(AriaRole.MENU).getByRole(AriaRole.MENUITEM))
                .hasText(actions.toArray(String[]::new));
        return this;
    }
}
