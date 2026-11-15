package io.student.rangiffler.jupiter.annotation;

import io.student.rangiffler.jupiter.extension.ProbeTargetCondition;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Tag("probe-target")
@ExtendWith(ProbeTargetCondition.class)
public @interface ProbeTarget {
}
