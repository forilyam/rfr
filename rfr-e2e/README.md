# rfr-e2e

E2E-тесты Rangiffler: JUnit 6.1.3, Playwright Java 1.63 (`@UsePlaywright`), Allure 3, `JdbcClient` для фикстур.

## Запуск

```bash
./gradlew :rfr-e2e:installPlaywright # подготовка Chromium перед первым прогоном
./gradlew e2e                       # все тесты модуля
./gradlew :rfr-e2e:test --tests '*RegistrationTest'
./gradlew :rfr-e2e:test --tests '*MainPageTest'
./gradlew :rfr-e2e:test --tests '*LoginNegativeTest'
./gradlew :rfr-e2e:test --tests '*ProfileTest'
./gradlew :rfr-e2e:allureReport      # rfr-e2e/build/reports/allure-report/allureReport/index.html
```

Стенд: MySQL `:3306`, `rfr-auth :9001`, `rfr-api :8081`, фронт `:3001` (см. корневой README).
Команды выполняются из корня проекта; тесты сами стенд не поднимают.
Задача `e2e` вызывает `:rfr-e2e:test`. Тесты выполняются при каждом запуске: для задачи настроено `outputs.upToDateWhen { false }`.
HTML-отчёт Gradle — `rfr-e2e/build/reports/tests/test/index.html`.

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
