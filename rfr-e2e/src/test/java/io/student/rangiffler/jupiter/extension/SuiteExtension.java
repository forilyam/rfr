package io.student.rangiffler.jupiter.extension;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

@NullMarked
public interface SuiteExtension extends BeforeAllCallback {

    @Override
    default void beforeAll(ExtensionContext context) {
        if (!supports(context)) {
            return;
        }
        final ExtensionContext root = context.getRoot();
        root.getStore(ExtensionContext.Namespace.GLOBAL)
                .computeIfAbsent(getClass(), _ -> new SuiteRun(this, root), SuiteRun.class)
                .start();
    }

    default boolean supports(ExtensionContext context) {
        return true;
    }

    default void beforeSuite(ExtensionContext context) {
    }

    default void afterSuite(ExtensionContext context) {
    }
}
