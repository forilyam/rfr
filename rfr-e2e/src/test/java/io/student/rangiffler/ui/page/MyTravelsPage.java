package io.student.rangiffler.ui.page;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;
import io.student.rangiffler.config.TestConfig;
import io.student.rangiffler.ui.component.PhotoCard;

import java.util.regex.Pattern;
import java.util.Objects;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public final class MyTravelsPage {
    public static final String URL = TestConfig.frontUrl() + "/my-travels";
    private final Page page;
    private final Locator photos;
    private final Locator countries;

    public MyTravelsPage(Page page) {
        this.page = page;
        this.photos = page.getByRole(AriaRole.ARTICLE);
        this.countries = page.getByRole(AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName(Pattern.compile("^\\d+ countr(?:y|ies) visited$")));
    }

    @Step("Открыть мои путешествия")
    public MyTravelsPage open() {
        page.navigate(URL);
        return this;
    }

    @Step("Выбрать фотографию по описанию: {description}")
    public PhotoCard photo(String description) {
        return new PhotoCard(photos, description);
    }

    @Step("Показать только мои путешествия")
    public MyTravelsPage clickOnlyMine() {
        clickFeed("Only my travels");
        return this;
    }

    @Step("Показать путешествия с друзьями")
    public MyTravelsPage clickWithFriends() {
        clickFeed("With friends");
        return this;
    }

    @Step("Открыть список посещённых стран")
    public MyTravelsPage clickVisitedCountries() {
        countries.click();
        return this;
    }

    @Step("Выбрать страну в списке посещённых стран: {country}")
    public MyTravelsPage clickCountry(String country) {
        page.getByRole(AriaRole.TOOLTIP).getByRole(AriaRole.BUTTON,
                new Locator.GetByRoleOptions().setName(country).setExact(true)).click();
        return this;
    }

    @Step("Сбросить фильтр по стране: {country}")
    public MyTravelsPage clickResetCountry(String country) {
        page.getByRole(AriaRole.BUTTON,
                        new Page.GetByRoleOptions().setName("Country filter: " + country).setExact(true))
                .getByRole(AriaRole.BUTTON,
                        new Locator.GetByRoleOptions().setName("Reset country filter").setExact(true)).click();
        return this;
    }

    @Step("Проверить число фотографий: {count}")
    public MyTravelsPage checkPhotoCount(int count) {
        assertThat(photos).hasCount(count);
        return this;
    }

    @Step("Проверить число посещённых стран при закрытом списке: {count}")
    public MyTravelsPage checkVisitedCountriesCount(int count) {
        assertThat(countries).hasText(count + (count == 1 ? " country visited" : " countries visited"));
        return this;
    }

    @Step("Проверить фильтр по стране: {country}")
    public MyTravelsPage checkCountryFilter(String country) {
        assertThat(page.getByRole(AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("Country filter: " + country).setExact(true))).hasText(country);
        return this;
    }

    private void clickFeed(String name) {
        Locator button = page.getByRole(AriaRole.GROUP,
                        new Page.GetByRoleOptions().setName("Travels filter").setExact(true))
                .getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName(name).setExact(true));
        if ("true".equals(button.getAttribute("aria-pressed"))) {
            button.click();
        } else {
            page.waitForResponse(response -> response.url().endsWith("/graphql")
                            && response.request().method().equals("POST")
                            && Pattern.compile("\"operationName\"\\s*:\\s*\"GetFeed\"")
                            .matcher(Objects.toString(response.request().postData(), "")).find(), button::click).finished();
        }
    }
}
