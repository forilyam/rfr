# rfr-e2e

E2E-тесты Rangiffler: JUnit 6.1.3, Playwright Java 1.63 (`@UsePlaywright`), Allure 3, `JdbcClient` для фикстур.

## Запуск

```bash
./gradlew :rfr-e2e:installPlaywright # подготовка Chromium перед первым прогоном
./gradlew e2e                       # все тесты модуля
./gradlew :rfr-e2e:e2e --tests '*TabTest'
./gradlew :rfr-e2e:test --tests '*RegistrationTest'
./gradlew :rfr-e2e:test --tests '*MainPageTest'
./gradlew :rfr-e2e:test --tests '*LoginNegativeTest'
./gradlew :rfr-e2e:test --tests '*ProfileTest'
./gradlew :rfr-e2e:allureReport      # rfr-e2e/build/reports/allure-report/allureReport/index.html
```

Стенд: MySQL `:3306`, `rfr-auth :9001`, `rfr-api :8081`, фронт `:3001` (см. корневой README).
Команды выполняются из корня проекта; тесты сами стенд не поднимают.
Корневая задача `e2e` вызывает `:rfr-e2e:e2e`; задача модуля поддерживает фильтр `--tests` и использует те же классы и настройки, что `:rfr-e2e:test`. Тесты выполняются при каждом запуске: для задач настроено `outputs.upToDateWhen { false }`.
HTML-отчёт `e2e` — `rfr-e2e/build/reports/tests/e2e/index.html`, отчёт `test` — `rfr-e2e/build/reports/tests/test/index.html`.

## Проверка схемы при открытии launcher-сессии

`StandSchemaExtension` реализует `LauncherSessionListener` и зарегистрирован через `META-INF/services/org.junit.platform.launcher.LauncherSessionListener`. Из регистрации Jupiter он исключён. Зависимость `junit-platform-launcher` объявлена как `testImplementation`, поскольку интерфейс нужен при компиляции. В `launcherSessionOpened` при `rfr.migrationCheck=true` выполняется прежняя сверка обеих схем через Flyway validation; миграции не применяются. При значении `false` слушатель также вызывается, но сверка пропускается.

Проверено 01.11.2026: Gradle 9.7.1, JUnit 6.1.3, JDK 25. Количество UI-тестов не определяет вызов слушателя: он относится к launcher-сессии, а не к аннотации `@UsePlaywright`. Это регистрация уровня [JUnit Platform Launcher](https://docs.junit.org/6.1.3/api/org.junit.platform.launcher/org/junit/platform/launcher/LauncherSessionListener.html), отдельная от Jupiter extensions.

### Запуск без UI и два fork-процесса

В текущей конфигурации и `test`, и `e2e` включают UI. Только для эксперимента init-скрипт исключал `io/student/rangiffler/test/**` из задачи `test`; постоянный состав задач сохранён. В XML успешных прогонов присутствуют ровно четыре класса: `FriendshipTest` (3 теста), `UserExtensionTest` (19), `UsersQueueExtensionTest` (4) и добавленный `SuiteExtensionTest` (1). Классов UI в XML нет.

Для подсчёта на время проверки включён уровень JUL `FINE` только для `io.student.rangiffler.jupiter.extension.StandSchemaExtension`; остальные JUL-логи отключены. Диагностическая запись `Launcher session opened in JVM <PID>, migration check enabled: <true|false>` находится в начале callback, до проверки флага. По умолчанию этот DEBUG-лог не выводится. Поэтому запись доказывает вызов слушателя даже при выключенной сверке; фактическая сверка отдельно подтверждена отказом при неверном пути миграций и включённом флаге.

| Запуск `:rfr-e2e:test` с временным исключением UI | Результат | Вызовы `launcherSessionOpened` |
|---|---|---|
| `rfr.migrationCheck=true`, `maxParallelForks=1` | 27 тестов прошли | 1, PID `7715` |
| `rfr.migrationCheck=true`, `maxParallelForks=2` | Те же 27 тестов прошли, без повторных исполнений | 2, по одному в PID `14297` и `14298` |
| Только `SuiteExtensionTest`, `rfr.migrationCheck=false` | 1 внешний тест прошёл; внутри EngineTestKit выполнены две пробы | 1, PID `5705`, только внешняя launcher-сессия |
| Только `UserExtensionTest`, сверка включена, путь auth-миграций отсутствует | Не удалось запустить тестовый процесс, тела тестов не выполнялись | 1, PID `1718`, до обнаружения тестов |

`maxParallelForks=2` задаёт верхнюю границу числа процессов, а не число вызовов для любого набора. В проверенном наборе были заняты оба процесса; `forkEvery` не менялся. Слушатель вызывается для каждой открытой launcher-сессии, общего состояния между JVM у него нет.

Итоговый повтор после форматирования и исправления предупреждений IDE подтвердил 27 уникальных исполнений без ошибок и ровно два вызова слушателя, в PID `41853` и `41854`. Его журнал и XML сохранены как `final-two.log` и `final-two-xml/`.

### EngineTestKit в SuiteExtensionTest

`SuiteExtensionTest.suiteCallbacksCannotRepeatForClassesInTheSameEngine` запускает Jupiter через `EngineTestKit` с двумя классами-пробами. Проверка отчётных событий подтверждает порядок `before`, `body`, `body`, `after`: подставной `SuiteExtension` оборачивает оба класса ровно один раз. Внутренний запуск движка не добавляет launcher-сессии и не вызывает `StandSchemaExtension`: в журнале остаётся ровно одна запись о внешней сессии. Запись выполняется до проверки `rfr.migrationCheck`, поэтому выключенный флаг не скрывает возможный внутренний вызов. В полном наборе со включённой сверкой вложенные EngineTestKit-запуски также не добавили вызовов к одному или двум внешним процессам.

Вложенные классы `SuiteExtensionTest$*.class` исключены из самостоятельного обнаружения Gradle, как вложенные пробы существующих extension-тестов. Они выполняются только внутри EngineTestKit.

### Исключение из launcherSessionOpened

Для воспроизведения передавались `-Drfr.migrationCheck=true -Drfr.authMigrations=/private/tmp/rfr-launcher-hvasbc4c/missing-auth`. Такой каталог не создавался. Проверка останавливается до обращения к БД; исходные миграции и схема БД не менялись.

В Gradle `:rfr-e2e:test` завершается с кодом `1`:

```text
Gradle Test Executor 13 FAILED
    org.gradle.api.internal.tasks.testing.TestSuiteExecutionException at SuiteTestDefinitionProcessor.java:45
        Caused by: java.lang.IllegalStateException at Migrations.java:48

> Test process encountered an unexpected problem.
   > Could not start Gradle Test Executor 13.
      > Migrations directory /private/tmp/rfr-launcher-hvasbc4c/missing-auth of schema rangiffler-auth does not exist
```

Это ошибка старта Gradle executor, а не `initializationError` отдельного класса. Свежих `TEST-*.xml` после этого запуска нет. До переноса та же проверка давала `UserExtensionTest > initializationError`; сохранённая проверка ожидаемого отказа при открытии сессии не проходит на старом журнале и проходит на новом.

В IntelliJ IDEA проверена именно нативная конфигурация **JUnit**, с классом `io.student.rangiffler.jupiter.probe.SuiteExtensionTest` и classpath модуля `rfr.rfr-e2e.test`. В VM options передавались те же флаг и неверный путь. Дерево результатов показывает `No tests were found`, консоль — `Internal Error occurred.`, `IllegalStateException` с указанным путём и стек через `StandSchemaExtension.launcherSessionOpened`, `DefaultLauncherSession`, `SessionPerRequestLauncher` и `JUnit6TestRunnerHelper`. Процесс завершился с кодом `254`. Это отказ до выполнения тестов; надпись об отсутствии тестов сама по себе не объясняет причину, её видно в консоли.

После эксперимента параметры намеренного отказа убраны, VM options восстановлены до `-ea`, а локальная конфигурация названа `SuiteExtensionTest - JUnit`. Контрольный нативный запуск показал `1 test passed` и код завершения `0`. Инспекции обоих изменённых Java-файлов не обнаружили ошибок и предупреждений.

Журналы, копии XML каждого прогона, выдержка из консоли IDE и `summary.json` сохранены локально в `build/launcher-checks/`. Там же находятся временные `verify.gradle`, `logging.properties` и команды в `README.txt`. Например, из корня проекта:

```bash
./gradlew :rfr-e2e:test -I rfr-e2e/build/launcher-checks/verify.gradle -Drfr.migrationCheck=true
./gradlew :rfr-e2e:test -I rfr-e2e/build/launcher-checks/verify.gradle -Dverification.forks=2 -Drfr.migrationCheck=true --info
```

## Конфигурация

`TestConfig` выбирает непустое системное свойство `-Drfr.<ключ>`, затем переменную окружения из таблицы, затем значение по умолчанию.
Gradle передаёт все `-Drfr.*` в тестовую JVM.

| Ключ | Переменная окружения | По умолчанию |
|---|---|---|
| `rfr.frontUrl` | `RFR_FRONT_URL` | `http://localhost:3001` |
| `rfr.authUrl` | `RFR_AUTH_URL` | `http://localhost:9001` |
| `rfr.apiUrl` | `RFR_API_URL` | `http://localhost:8081` |
| `rfr.dbUrl` | `RFR_DB_URL` | `jdbc:mysql://localhost:3306` (схема добавляется кодом) |
| `rfr.dbUser` | `RFR_DB_USER` | `root` |
| `rfr.dbPassword` | `RFR_DB_PASSWORD` | `secret` |
| `rfr.migrationCheck` | `RFR_MIGRATION_CHECK` | `false` |
| `rfr.authMigrations` | `RFR_AUTH_MIGRATIONS` | `../rfr-auth/src/main/resources/db/migration/rangiffler-auth` |
| `rfr.apiMigrations` | `RFR_API_MIGRATIONS` | `../rfr-api/src/main/resources/db/migration/rangiffler-api` |
| `rfr.browser` | `RFR_BROWSER` | `chromium` |
| `rfr.headless` | `RFR_HEADLESS` | `true` |

## Архитектура

Один source set `src/test`, пакет `io.student.rangiffler`:

| Пакет | Что лежит |
|---|---|
| `config` | `TestConfig` — единственная точка чтения настроек |
| `jupiter` | всё про JUnit: `annotation` (`@User`), `extension` (`UserExtension`), позже `condition`, `probe` |
| `data` | тестовые данные и их владельцы: `TestUser`, `UserFixture`; `db` — `Databases`, `Jdbc` |
| `utils` | статические утилиты без состояния: `RandomDataUtils` — случайные username/password; сюда же будущие генераторы (описания фото и т.п.) |
| `ui` | `page` — Page Objects, `component` — общие части страниц |
| `test` | сценарии регистрации, позитивного и негативного входа в `auth`; сохранения профиля, смены страны и Reset в `profile` |

Правила: в `jupiter` нет SQL и локаторов; в `data` нет JUnit; в `test` нет локаторов и SQL; `ThreadLocal` для состояния теста не используется — состояние живёт в Store расширения.

## Агент

Page Objects пишет Codex по skill `rfr-page-objects`; UI исследуется через Playwright MCP.
Настройки MCP находятся в `.codex/config.toml` и `.mcp.json` в корне проекта.

Доказательство корректности теста - запуск gradle таски.
