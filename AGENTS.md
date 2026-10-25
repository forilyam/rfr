# Rangiffler (rfr)

Учебный проект курса Java Advanced 3.0. Монолит с моками: часть функций настоящая (регистрация, вход, профиль, дружба), часть возвращается из JSON-моков (фото, лайки, лента, статистика).

## Модули и порты

| Модуль | Что это | Порт |
|---|---|---|
| `rfr-auth` | Spring Authorization Server, схема `rangiffler-auth` | 9001 |
| `rfr-api` | GraphQL API `/graphql`, схема `rangiffler-api`; фото и лента — моки из `src/main/resources/mock/*.json` | 8081 |
| `rfr-gql-client` | React + Apollo, собирается отдельно от Gradle (`npm run dev`) | 3001 |
| `rfr-e2e` | E2E-тесты: JUnit 6, Playwright Java, Allure 3 | — |

MySQL — порт 3306, пользователь `root`, пароль `secret`.

## Окружение

- Не запускай `localenv.sh`: он останавливает и удаляет **все** Docker-контейнеры машины. Поднимай только MySQL (`docker run --name rfr-mysql -p 3306:3306 -e MYSQL_ROOT_PASSWORD=secret -d mysql:8.4.7`), сервисы и фронт.
- Нужны JDK 25, Node 22+, Docker.

## Команды

```bash
./gradlew :rfr-e2e:installPlaywright
./gradlew :rfr-e2e:test --tests '*RegistrationTest' --rerun-tasks
./gradlew :rfr-e2e:test --tests '*MainPageTest' --rerun-tasks
./gradlew :rfr-e2e:allureReport
```

Настройки тестов — `TestConfig`: `-Drfr.<ключ>=…` или переменная `RFR_<КЛЮЧ>`, иначе значение по умолчанию. Подробности — `rfr-e2e/README.md`.

## Правила для rfr-e2e

- Структура пакетов — `rfr-e2e/README.md`, раздел «Архитектура»: `config`, `jupiter`, `data`, `api`, `ui`, `assertions`, `test`. Новые классы кладутся в существующие пакеты.
- Page Objects и компоненты — по skill `rfr-page-objects` (`.agents`).
- Без комментариев и Javadoc в коде `rfr-e2e` (Java, Gradle, SQL, properties): смысл передают имена и `@Step`.
- Вход в тестах только через UI; токены в localStorage не подставлять.
- Проход сценария через Playwright MCP — не запуск теста. Успех подтверждается JUnit-прогоном через Gradle.
- Тестовые данные создаются и удаляются фикстурами (`@User` → `UserExtension` → `UserFixture`), без широких `DELETE`/`TRUNCATE`.
