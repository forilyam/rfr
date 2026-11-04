package io.student.rangiffler.jupiter.annotation;

import com.microsoft.playwright.junit.UsePlaywright;
import io.student.rangiffler.jupiter.extension.BrowserOptions;
import org.junit.jupiter.api.Tag;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Tag("ui")
@UsePlaywright(BrowserOptions.class)
public @interface UiTest {
}
