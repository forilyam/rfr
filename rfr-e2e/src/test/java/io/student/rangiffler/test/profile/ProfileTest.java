package io.student.rangiffler.test.profile;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.junit.UsePlaywright;
import com.microsoft.playwright.options.FilePayload;
import io.student.rangiffler.data.TestUser;
import io.student.rangiffler.jupiter.annotation.User;
import io.student.rangiffler.ui.page.LandingPage;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import javax.imageio.ImageIO;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@UsePlaywright
@DisplayName("Профиль пользователя")
class ProfileTest {
    @Test
    @DisplayName("Сохранённые имя, фамилия, страна и аватар остаются после перезагрузки")
    void savedProfileSurvivesReload(Page page, @User TestUser user) throws IOException {
        ByteArrayOutputStream image = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(13, 19, BufferedImage.TYPE_INT_RGB), "png", image);
        String avatar = "data:image/png;base64," + Base64.getEncoder().encodeToString(image.toByteArray());
        new LandingPage(page).open()
                .clickLogin()
                .fillLoginForm(user.username(), user.password())
                .clickSignIn()
                .clickProfile()
                .fillProfileForm("Élodie", "Moreau-Lefèvre", "France")
                .uploadAvatar(new FilePayload("portrait.png", "image/png", image.toByteArray()))
                .checkAvatar(avatar)
                .clickSave()
                .reload()
                .checkProfileForm("Élodie", "Moreau-Lefèvre", "France", avatar);
    }

    @Test
    @DisplayName("Смена страны с France на Germany сохраняется после перезагрузки")
    void changedCountrySurvivesReload(Page page, @User TestUser user) {
        new LandingPage(page).open()
                .clickLogin()
                .fillLoginForm(user.username(), user.password())
                .clickSignIn()
                .clickProfile()
                .fillProfileForm("Inès", "Dubois", "France")
                .clickSave()
                .reload()
                .checkLocation("France")
                .fillProfileForm("Inès", "Dubois", "Germany")
                .clickSave()
                .reload()
                .checkLocation("Germany");
    }

    @Test
    @DisplayName("Reset восстанавливает сохранённые имя, фамилию, страну и аватар")
    void resetRestoresSavedProfile(Page page, @User TestUser user) throws IOException {
        ByteArrayOutputStream image = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(17, 23, BufferedImage.TYPE_INT_RGB), "png", image);
        ByteArrayOutputStream replacement = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(29, 11, BufferedImage.TYPE_INT_RGB), "png", replacement);
        String avatar = "data:image/png;base64," + Base64.getEncoder().encodeToString(image.toByteArray());
        new LandingPage(page).open()
                .clickLogin()
                .fillLoginForm(user.username(), user.password())
                .clickSignIn()
                .clickProfile()
                .fillProfileForm("Maëlle", "D'Arcy", "France")
                .uploadAvatar(new FilePayload("saved.png", "image/png", image.toByteArray()))
                .checkAvatar(avatar)
                .clickSave()
                .reload()
                .fillProfileForm("Björn", "Lindström", "Germany")
                .uploadAvatar(new FilePayload("replacement.png", "image/png", replacement.toByteArray()))
                .checkAvatar("data:image/png;base64," + Base64.getEncoder().encodeToString(replacement.toByteArray()))
                .clickReset()
                .checkProfileForm("Maëlle", "D'Arcy", "France", avatar);
    }
}
