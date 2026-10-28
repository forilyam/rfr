# Rangiffler (rfr)

Учебный проект курса Java Advanced 3.0. Монолит с моками: часть функций настоящая (регистрация, вход, профиль, дружба), часть возвращается из JSON-моков (фото, лайки, лента, статистика).

## Модули и порты

| Модуль | Что это | Порт | Инструкции |
|---|---|---|---|
| `rfr-auth` | Spring Authorization Server, схема `rangiffler-auth` | 9001 | [AGENTS.md](rfr-auth/AGENTS.md) |
| `rfr-api` | GraphQL API `/graphql`, схема `rangiffler-api`; фото и лента — моки из `src/main/resources/mock/*.json` | 8081 | [AGENTS.md](rfr-api/AGENTS.md) |
| `rfr-gql-client` | React + Apollo, собирается отдельно от Gradle (`npm run dev`) | 3001 | [AGENTS.md](rfr-gql-client/AGENTS.md) |
| `rfr-e2e` | E2E-тесты: JUnit 6, Playwright Java, Allure 3 | — | [AGENTS.md](rfr-e2e/AGENTS.md) |

MySQL — порт 3306, пользователь `root`, пароль `secret`.

## Окружение

- Из корня проекта поднимай MySQL командой `docker compose up -d mysql`, затем оба бэкенда командой `./gradlew runAll`. Фронт запускай командой `npm run dev` из `rfr-gql-client`.
- `runAll` запускает `rfr-auth:bootRun` и `rfr-api:bootRun` параллельно; параллельность Gradle включена в `gradle.properties`. Для одновременного запуска нужны минимум два Gradle worker; не используй `--no-parallel` или `--max-workers=1`.
- Нужны JDK 25, Node 22+, Docker.
