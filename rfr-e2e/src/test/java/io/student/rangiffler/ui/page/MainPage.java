package io.student.rangiffler.ui.page;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;
import io.student.rangiffler.config.TestConfig;
import io.student.rangiffler.ui.component.HeaderComponent;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public final class MainPage {
  public static final String URL = TestConfig.frontUrl() + "/my-travels";
  private final HeaderComponent header;
  private final Locator travelsMap;
  private final Locator photos;
  private final Locator onlyMine;
  private final Locator withFriends;
  private final Locator addPhoto;

  public MainPage(Page page) {
    this.header = new HeaderComponent(page);
    this.travelsMap = page.getByRole(AriaRole.HEADING,
        new Page.GetByRoleOptions().setName("Travels map").setExact(true));
    this.photos = page.getByRole(AriaRole.HEADING,
        new Page.GetByRoleOptions().setName("Photos").setExact(true));
    this.onlyMine = page.getByRole(AriaRole.BUTTON,
        new Page.GetByRoleOptions().setName("Only my travels").setExact(true));
    this.withFriends = page.getByRole(AriaRole.BUTTON,
        new Page.GetByRoleOptions().setName("With friends").setExact(true));
    this.addPhoto = page.getByRole(AriaRole.BUTTON,
        new Page.GetByRoleOptions().setName("Add photo").setExact(true));
  }

  @Step("Проверить основные элементы главной страницы")
  public MainPage checkThatMainElementsVisible() {
    assertThat(travelsMap).isVisible();
    assertThat(photos).isVisible();
    assertThat(onlyMine).isVisible();
    assertThat(onlyMine).hasAttribute("aria-pressed", "true");
    assertThat(withFriends).isVisible();
    assertThat(addPhoto).isVisible();
    header.checkThatVisible();
    return this;
  }

  @Step("Открыть профиль")
  public ProfilePage clickProfile() {
    return header.clickProfile();
  }
}
