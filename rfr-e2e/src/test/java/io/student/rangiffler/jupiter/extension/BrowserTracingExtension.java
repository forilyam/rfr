package io.student.rangiffler.jupiter.extension;

import com.microsoft.playwright.Tracing;
import com.microsoft.playwright.impl.junit.BrowserContextExtension;
import io.qameta.allure.Allure;
import io.qameta.allure.AttachmentOptions;
import io.student.rangiffler.config.TestConfig;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.opentest4j.TestAbortedException;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public final class BrowserTracingExtension implements BeforeEachCallback, AfterEachCallback {
    @Override
    public void beforeEach(ExtensionContext context) {
        Tracing tracing = BrowserContextExtension.getOrCreateBrowserContext(context).tracing();
        tracing.start(new Tracing.StartOptions().setScreenshots(true).setSnapshots(true).setSources(true));
        context.getStore(ExtensionContext.Namespace.create(BrowserTracingExtension.class)).put("tracing", tracing);
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        Optional<Tracing> tracing = Optional.ofNullable(context
                .getStore(ExtensionContext.Namespace.create(BrowserTracingExtension.class))
                .remove("tracing", Tracing.class));
        if (tracing.isEmpty()) {
            return;
        }
        if (context.getExecutionException().filter(this::failed).isEmpty()) {
            tracing.get().stop();
            return;
        }
        Path archive = Files.createTempDirectory(Files.createDirectories(TestConfig.traces()),
                context.getRequiredTestMethod().getName() + "-").resolve("trace.zip");
        tracing.get().stop(new Tracing.StopOptions().setPath(archive));
        try (InputStream input = Files.newInputStream(archive)) {
            Allure.getLifecycle().addAttachment("trace.zip", "application/zip", input,
                    AttachmentOptions.withFileExtension("zip"));
        }
    }

    private boolean failed(Throwable failure) {
        try {
            throw failure;
        } catch (TestAbortedException exception) {
            return false;
        } catch (Throwable exception) {
            return true;
        }
    }
}
