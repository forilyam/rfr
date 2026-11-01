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
Перед первым классом с тегом `ui` расширение `StandReadinessExtension` ожидает готовности стенда, по умолчанию до 180 секунд на все сервисы. Для MySQL проверяется TCP-подключение, для авторизации и фронтенда — HTTP-ответ с кодом ниже 400, для GraphQL API — получение HTTP-ответа на POST `/graphql` с запросом `{__typename}`, включая ответ с ошибкой. При истечении тайм-аута диагностика перечисляет недоступные сервисы и команды их запуска; UI-сценарии не выполняются. Классы без тега `ui` эту проверку не запускают.
Для изменения времени ожидания передай `-Drfr.standTimeout=240`, для отключения проверки — `-Drfr.standCheck=false`.
Расширения зарегистрированы глобально через `META-INF/services/org.junit.jupiter.api.extension.Extension`: `AllureEnvironmentExtension`, `StandReadinessExtension`, `StandSchemaExtension`. Автообнаружение включено в `junit-platform.properties`, поэтому регистрация действует и при запуске UI-класса из IDE. В classpath-запуске [`ServiceLoader` сохраняет порядок строк внутри одного service-файла](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/ServiceLoader.html#load(java.lang.Class,java.lang.ClassLoader)); JUnit Jupiter 6.1.3 регистрирует обнаруженные расширения в этом порядке, а `BeforeAllCallback` вызывается в порядке регистрации. Поэтому проверка готовности завершается до сверки миграций; при её ошибке сверка не запускается. Эта гарантия опирается на совместную регистрацию расширений в одном файле.
Задача `e2e` зависит только от тестовой задачи модуля и не запускает `waitForStand`: ожиданием управляет JUnit внутри тестовой JVM.
Корневая задача `e2e` вызывает `:rfr-e2e:e2e`; задача модуля поддерживает фильтр `--tests` и использует те же классы и настройки, что `:rfr-e2e:test`. Тесты выполняются при каждом запуске: для задач настроено `outputs.upToDateWhen { false }`.
HTML-отчёт `e2e` — `rfr-e2e/build/reports/tests/e2e/index.html`, отчёт `test` — `rfr-e2e/build/reports/tests/test/index.html`.

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
| `rfr.dbTimeout` | `RFR_DB_TIMEOUT` | `5` (положительное число секунд; получение и проверка соединения, подключение и чтение JDBC) |
| `rfr.browser` | `RFR_BROWSER` | `chromium` |
| `rfr.headless` | `RFR_HEADLESS` | `true` |
| `rfr.standCheck` | `RFR_STAND_CHECK` | `true` |
| `rfr.standTimeout` | `RFR_STAND_TIMEOUT` | `180` (положительное число секунд) |
| `rfr.commit` | `RFR_COMMIT` | Gradle передаёт результат `git rev-parse --short HEAD`; вне Gradle — `unknown` |

## Окружение в Allure

`AllureEnvironmentExtension.afterSuite()` записывает `environment.properties` в каталог из системного свойства `allure.results.directory`. По умолчанию Gradle использует `rfr-e2e/build/allure-results`; при прямом запуске JUnit из корня проекта используется тот же относительный путь. Переопределение `-Dallure.results.directory=<каталог>` поддерживает и Gradle.

Файл содержит `frontUrl`, `authUrl`, `apiUrl`, `browser`, `headless`, `commit`, `rangiffler-auth.version` и `rangiffler-api.version`. Версии читаются через `Migrations.currentVersion()` из текущего состояния Flyway после прогона; при недоступной схеме записывается `unavailable`, при отсутствии текущей версии — `0`. Само расширение Git не запускает.

Расширение окружения стоит первым, потому что JUnit прекращает цепочку `beforeAll` после первой ошибки. Его вызов заранее регистрирует `SuiteRun` в корневом `ExtensionContext.Store`; при закрытии Store вызывается `afterSuite`, даже если последующее расширение не смогло подготовить стенд. Если поставить расширение окружения после упавшего, ресурс не зарегистрируется и файл не появится. Это поведение проверяет `AllureEnvironmentExtensionTest` через EngineTestKit.

Мишень с настоящим `AllureEnvironmentExtension` запускается в отдельной JVM и проверяет все восемь рабочих ключей. Проба зависшего SQL использует TCP-сервер на эфемерном порту: он завершает рукопожатие MySQL и не отвечает на запрос. При `rfr.dbTimeout=1` расширение должно завершить чтение обеих схем и записать `unavailable`; дочерняя JVM, соединения и задачи сервера принадлежат пробе и освобождаются при её завершении.

Для просмотра отчёта из корня проекта выполни `./gradlew allureServe`.

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
