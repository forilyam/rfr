package io.student.rangiffler.jupiter.extension;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.impl.junit.BrowserContextExtension;
import io.student.rangiffler.config.TestConfig;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public final class BrowserTimeoutExtension implements BeforeEachCallback {
    @Override
    public void beforeEach(ExtensionContext context) {
        BrowserContext browser = BrowserContextExtension.getOrCreateBrowserContext(context);
        browser.setDefaultTimeout(TestConfig.actionTimeout().toMillis());
        browser.setDefaultNavigationTimeout(TestConfig.navigationTimeout().toMillis());
    }
}
