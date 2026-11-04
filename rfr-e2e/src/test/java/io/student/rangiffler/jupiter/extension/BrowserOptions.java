package io.student.rangiffler.jupiter.extension;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.junit.Options;
import com.microsoft.playwright.junit.OptionsFactory;
import io.student.rangiffler.config.TestConfig;

public final class BrowserOptions implements OptionsFactory {
    @Override
    public Options getOptions() {
        return new Options()
                .setHeadless(TestConfig.headless())
                .setTestIdAttribute("data-test-id")
                .setContextOptions(contextOptions());
    }

    private Browser.NewContextOptions contextOptions() {
        return new Browser.NewContextOptions()
                .setViewportSize(TestConfig.viewportWidth(), TestConfig.viewportHeight());
    }
}
