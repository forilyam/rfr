# rfr-e2e

E2E-тесты Rangiffler: JUnit 6.1.3, Playwright Java 1.63 (`@UsePlaywright`), Allure 3, `JdbcClient` для фикстур.

## Запуск

```bash
./gradlew :rfr-e2e:installPlaywright # подготовка Chromium перед первым прогоном
./gradlew e2e -Drfr.migrationCheck=true # все тесты через suite со сверкой миграций
./gradlew :rfr-e2e:e2e --tests '*TabTest'
./gradlew :rfr-e2e:test --tests '*RegistrationTest'
./gradlew :rfr-e2e:test --tests '*MainPageTest'
./gradlew :rfr-e2e:test --tests '*LoginNegativeTest'
./gradlew :rfr-e2e:test --tests '*ProfileTest'
./gradlew :rfr-e2e:allureReport      # rfr-e2e/build/reports/allure-report/allureReport/index.html
```

Стенд: MySQL `:3306`, `rfr-auth :9001`, `rfr-api :8081`, фронт `:3001` (см. корневой README).
Команды выполняются из корня проекта; тесты сами стенд не поднимают.
Корневая задача `e2e` вызывает `:rfr-e2e:e2e`. Задача модуля включает только движок `junit-platform-suite` и запускает `io.student.rangiffler.test.RangifflerSuite`; suite выбирает тесты пакета `io.student.rangiffler` и выполняет их через Jupiter. В `@BeforeSuite` выполняется сверка миграций при `rfr.migrationCheck=true`. Глобальная регистрация `StandSchemaExtension` убрана. Задача `:rfr-e2e:test` оставлена для прямого запуска классов и методов; полный запуск этой задачи после добавления suite может дублировать тесты.

Фильтр `--tests` передавай задаче модуля: `./gradlew :rfr-e2e:e2e --tests '*TabTest'`. Корневая задача `e2e` не является задачей типа `Test` и не принимает `--tests`.
Тесты выполняются при каждом запуске: для задач настроено `outputs.upToDateWhen { false }`.
HTML-отчёт `e2e` — `rfr-e2e/build/reports/tests/e2e/index.html`, отчёт `test` — `rfr-e2e/build/reports/tests/test/index.html`.

## Проверка запуска через suite

Результаты от 01.11.2026: Gradle 9.7.1, JUnit 6.1.3, Allure Java 3.0.0, Allure Report 3.17.0. MySQL, оба бэкенда и фронтенд работали. Во всех проверках сверки явно передавалось `-Drfr.migrationCheck=true`.

| Запуск | Результат | `@BeforeSuite` |
|---|---|---|
| `./gradlew e2e -Drfr.migrationCheck=true` | 67 тестов прошли, каждый выполнен один раз | Выполняется |
| Тот же запуск со снятым ограничением движков | 134 исполнения 67 уникальных тестов, каждый ровно дважды; 21 ошибка во втором проходе UI: `Playwright connection closed` | Выполняется для suite; прямой проход Jupiter идёт отдельно |
| `./gradlew e2e --tests '*TabTest'` | Ошибка настройки `Unknown command-line option '--tests'`, тесты не запускаются | Не выполняется |
| `./gradlew :rfr-e2e:e2e --tests '*TabTest' -Drfr.migrationCheck=true` | 9 тестов из трёх классов `*TabTest` прошли | Выполняется; отдельно подтверждено отказом при неверном пути миграций |
| Один класс или метод напрямую из IDE | Фактический запуск не проверен: инструменты IDE не вернули результат | По контракту JUnit не выполняется: прямой запуск Jupiter не запускает suite |

Ограничение движков снималось только временным init-скриптом; итоговая конфигурация `e2e` оставляет только `junit-platform-suite`. Артефакты прогонов сохранены локально в `build/suite-checks/`: `summary.json`, `suite-only/`, `all-engines/`, `module-filter/`, `module-filter-success/`; команды и описание изоляции Allure — в `README.txt`.

### Падение `@BeforeSuite`

Для проверки сделана временная копия auth-миграций с изменённым checksum `V1__schema_init.sql`. Путь копии передан через `-Drfr.authMigrations=...`. Исходные миграции и схема БД не менялись; Flyway выполнял только validation.

Gradle завершился с ошибкой и показал:

```text
RangifflerSuite > initializationError FAILED
    java.lang.IllegalStateException at RangifflerSuite.java:34

1 test completed, 1 failed
```

Это синтетический результат инициализации suite; тела 67 тестов не выполнялись. XML содержит `Migration checksum mismatch for migration version 1` и стек вызова `RangifflerSuite.checkMigrations`.

Allure сохранил **0 результатов тестов и 1 глобальную ошибку**: `Container RangifflerSuite failed, tests inside it did not run: ...`. Ошибка присутствует в сырых `*-globals.json` и в `widgets/globals.json` собранного отчёта. В `summary.json` отчёта при этом `stats.total=0` и `status="passed"`: этот статус сам по себе не доказывает успешность запуска, нужно учитывать глобальные ошибки и код завершения Gradle.

Изолированный HTML Allure: `build/suite-checks/failure/allure-report/index.html`; XML, HTML Gradle и сырые результаты — в соседних `xml/`, `html/`, `allure-results/`. Содержимое отчёта проверено по JSON; визуальная проверка в браузере не выполнена. Обычные отчёты после последнего успешного прогона показывают девять `TabTest`; для анализа отказа используй сохранённую копию `failure/`.

Семантика suite и причина двойного запуска описаны в [документации JUnit 6.1.3](https://docs.junit.org/6.1.3/advanced-topics/junit-platform-suite-engine.html). Чтобы проверить обход сверки в IDE, запусти отдельно класс `FriendsTabTest` и метод `friendsTabShowsEmptyStateWithoutFriends` через JUnit runner с `-Drfr.migrationCheck=true -Drfr.authMigrations=/private/tmp/rfr-missing-migrations`. При прямом запуске они должны пройти без ошибки сверки; запуск самого `RangifflerSuite` с теми же параметрами должен завершиться ошибкой до тестов.

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
