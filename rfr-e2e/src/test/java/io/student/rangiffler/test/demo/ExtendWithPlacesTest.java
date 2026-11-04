package io.student.rangiffler.test.demo;

import io.student.rangiffler.jupiter.annotation.Demo;
import io.student.rangiffler.jupiter.extension.DemoExtension;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("demo")
@TestMethodOrder(MethodOrderer.MethodName.class)
@DisplayName("Места установки @ExtendWith")
class ExtendWithPlacesTest {
    @ExtendWith(DemoExtension.class)
    interface Extended {
    }

    @Nested
    @ExtendWith(DemoExtension.class)
    @DisplayName("На классе")
    class OnClass {
        @Test
        @DisplayName("Первый тест получает параметр от расширения класса")
        void firstTestResolvesParameter(String value) {
            assertEquals("resolved by DemoExtension", value, "DemoExtension did not resolve the parameter");
        }

        @Test
        @DisplayName("Второй тест получает параметр от того же расширения")
        void secondTestResolvesParameter(String value) {
            assertEquals("resolved by DemoExtension", value, "DemoExtension did not resolve the parameter");
        }
    }

    @Nested
    @DisplayName("На интерфейсе, который реализует класс")
    class OnInterface implements Extended {
        @Test
        @DisplayName("Первый тест получает параметр от расширения интерфейса")
        void firstTestResolvesParameter(String value) {
            assertEquals("resolved by DemoExtension", value, "DemoExtension did not resolve the parameter");
        }

        @Test
        @DisplayName("Второй тест получает параметр от того же расширения")
        void secondTestResolvesParameter(String value) {
            assertEquals("resolved by DemoExtension", value, "DemoExtension did not resolve the parameter");
        }
    }

    @Nested
    @Demo
    @DisplayName("В составной аннотации")
    class OnMetaAnnotation {
        @Test
        @DisplayName("Первый тест получает параметр от расширения из @Demo")
        void firstTestResolvesParameter(String value) {
            assertEquals("resolved by DemoExtension", value, "DemoExtension did not resolve the parameter");
        }

        @Test
        @DisplayName("Второй тест получает параметр от того же расширения")
        void secondTestResolvesParameter(String value) {
            assertEquals("resolved by DemoExtension", value, "DemoExtension did not resolve the parameter");
        }
    }

    @Nested
    @DisplayName("На тестовом методе")
    class OnMethod {
        @Test
        @ExtendWith(DemoExtension.class)
        @DisplayName("Первый тест получает параметр от своего расширения")
        void firstTestResolvesParameter(String value) {
            assertEquals("resolved by DemoExtension", value, "DemoExtension did not resolve the parameter");
        }

        @Test
        @ExtendWith(DemoExtension.class)
        @DisplayName("Второй тест получает параметр от своего расширения")
        void secondTestResolvesParameter(String value) {
            assertEquals("resolved by DemoExtension", value, "DemoExtension did not resolve the parameter");
        }
    }

    @Nested
    @DisplayName("На статическом поле")
    class OnStaticField {
        @ExtendWith(DemoExtension.class)
        static String field;

        @Test
        @DisplayName("Первый тест получает параметр от расширения статического поля")
        void firstTestResolvesParameter(String value) {
            assertEquals("resolved by DemoExtension", value, "DemoExtension did not resolve the parameter");
        }

        @Test
        @DisplayName("Второй тест получает параметр от того же расширения")
        void secondTestResolvesParameter(String value) {
            assertEquals("resolved by DemoExtension", value, "DemoExtension did not resolve the parameter");
        }
    }

    @Nested
    @DisplayName("На поле экземпляра")
    class OnInstanceField {
        @ExtendWith(DemoExtension.class)
        String field;

        @Test
        @DisplayName("Первый тест получает параметр от расширения поля экземпляра")
        void firstTestResolvesParameter(String value) {
            assertEquals("resolved by DemoExtension", value, "DemoExtension did not resolve the parameter");
        }

        @Test
        @DisplayName("Второй тест получает параметр от расширения своего экземпляра")
        void secondTestResolvesParameter(String value) {
            assertEquals("resolved by DemoExtension", value, "DemoExtension did not resolve the parameter");
        }
    }

    @Nested
    @DisplayName("На параметре конструктора")
    class OnConstructorParameter {
        private final String value;

        OnConstructorParameter(@ExtendWith(DemoExtension.class) String value) {
            this.value = value;
        }

        @Test
        @DisplayName("Первый тест видит значение из конструктора")
        void firstTestSeesConstructorValue() {
            assertEquals("resolved by DemoExtension", value, "DemoExtension did not resolve the constructor parameter");
        }

        @Test
        @DisplayName("Второй тест видит значение из конструктора")
        void secondTestSeesConstructorValue() {
            assertEquals("resolved by DemoExtension", value, "DemoExtension did not resolve the constructor parameter");
        }
    }

    @Nested
    @DisplayName("На параметре тестового метода")
    class OnTestParameter {
        @Test
        @DisplayName("Первый тест получает параметр от своего расширения")
        void firstTestResolvesParameter(@ExtendWith(DemoExtension.class) String value) {
            assertEquals("resolved by DemoExtension", value, "DemoExtension did not resolve the parameter");
        }

        @Test
        @DisplayName("Второй тест получает параметр от своего расширения")
        void secondTestResolvesParameter(@ExtendWith(DemoExtension.class) String value) {
            assertEquals("resolved by DemoExtension", value, "DemoExtension did not resolve the parameter");
        }
    }

    @Nested
    @DisplayName("На параметре @BeforeEach")
    class OnBeforeEachParameter {
        private String value;

        @BeforeEach
        void remember(@ExtendWith(DemoExtension.class) String value) {
            this.value = value;
        }

        @Test
        @DisplayName("Первый тест видит значение из @BeforeEach")
        void firstTestSeesBeforeEachValue() {
            assertEquals("resolved by DemoExtension", value, "DemoExtension did not resolve the @BeforeEach parameter");
        }

        @Test
        @DisplayName("Второй тест видит значение из @BeforeEach")
        void secondTestSeesBeforeEachValue() {
            assertEquals("resolved by DemoExtension", value, "DemoExtension did not resolve the @BeforeEach parameter");
        }
    }

    @Nested
    @DisplayName("На параметре @BeforeAll и @AfterAll")
    class OnBeforeAllParameter {
        static String value;

        @BeforeAll
        static void remember(@ExtendWith(DemoExtension.class) String resolved) {
            value = resolved;
        }

        @AfterAll
        static void forget(@ExtendWith(DemoExtension.class) String resolved) {
            value = null;
        }

        @Test
        @DisplayName("Первый тест видит значение из @BeforeAll")
        void firstTestSeesBeforeAllValue() {
            assertEquals("resolved by DemoExtension", value, "DemoExtension did not resolve the @BeforeAll parameter");
        }

        @Test
        @DisplayName("Второй тест видит значение из @BeforeAll")
        void secondTestSeesBeforeAllValue() {
            assertEquals("resolved by DemoExtension", value, "DemoExtension did not resolve the @BeforeAll parameter");
        }
    }
}