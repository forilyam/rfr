package io.student.rangiffler.ui.component;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;
import io.student.rangiffler.ui.page.ProfilePage;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public final class HeaderComponent {
  private final Page page;
  private final Locator header;
  private final Locator logo;
  private final Locator profile;
  private final Locator logout;

  public HeaderComponent(Page page) {
    this.page = page;
    this.header = page.getByRole(AriaRole.BANNER);
    this.logo = header.getByRole(AriaRole.HEADING,
        new Locator.GetByRoleOptions().setName("Rangiffler").setExact(true));
    this.profile = header.getByRole(AriaRole.LINK,
        new Locator.GetByRoleOptions().setName("Profile").setExact(true));
    this.logout = header.getByRole(AriaRole.BUTTON,
        new Locator.GetByRoleOptions().setName("Logout").setExact(true));
  }

  @Step("Проверить элементы верхней панели")
  public HeaderComponent checkThatVisible() {
    assertThat(logo).isVisible();
    assertThat(profile).isVisible();
    assertThat(logout).isVisible();
    return this;
  }

  @Step("Нажать Profile")
  public ProfilePage clickProfile() {
    profile.click();
    return new ProfilePage(page);
  }
}
