# Домашнее задание к занятию 01. Сложный старт

Объём — около 6 часов. Результат сдаётся пулл-реквестом в ваш форк `rfr`.

## Часть 1. Свой форк и локальный запуск

1. Сделайте форк [qa-guru/rfr](https://github.com/qa-guru/rfr) в свой GitHub-аккаунт и склонируйте его.
2. Подтяните код занятия из бандла `rfr-l01.bundle` (лежит в материалах занятия) и создайте от него рабочую ветку:

   ```bash
   git fetch ./rfr-l01.bundle v3/l01-first-e2e:v3/l01-first-e2e
   git switch -c hw/01-profile-tests v3/l01-first-e2e
   ```

3. Проверьте окружение: JDK 25, Node.js 22+, Docker.
4. Поднимите стенд. **Не запускайте `localenv.sh`**: он останавливает и удаляет все Docker-контейнеры на машине.

   ```bash
   docker run --name rfr-mysql -p 3306:3306 -e MYSQL_ROOT_PASSWORD=secret -d mysql:8.4.7
   (cd rfr-auth && ../gradlew bootRun)            # http://localhost:9001, запускать первым
   (cd rfr-api && ../gradlew bootRun)             # http://localhost:8081/graphiql
   (cd rfr-gql-client && npm ci && npm run dev)   # http://localhost:3001
   ```

5. Убедитесь, что тесты работают:

   ```bash
   ./gradlew :rfr-e2e:installPlaywright
   ./gradlew :rfr-e2e:test --tests '*RegistrationTest' --tests '*MainPageTest' --rerun-tasks
   ./gradlew :rfr-e2e:allureReport
   ```

   Оба теста зелёные два прогона подряд, в отчёте Allure видны шаги `@Step`. После прогона тестовые пользователи удалены:

   ```sql
   SELECT COUNT(*) FROM `rangiffler-auth`.`user` WHERE username LIKE 'rfr\_%';
   SELECT COUNT(*) FROM `rangiffler-api`.`user`  WHERE username LIKE 'rfr\_%';
   ```

## Часть 2. Агент с нашим skill

Настройте **один** агент на выбор — Claude Code, Cursor, Codex или GigaCode — так, чтобы он видел skill `rfr-page-objects` и Playwright MCP.

Skill — это папка с файлом `SKILL.md`: YAML-заголовок с полями `name` и `description` и инструкции в Markdown. Агент читает `description` и сам решает, когда применить skill, или вы вызываете его явно. Поле `name` должно совпадать с именем папки. Исходник skill в проекте один — `../../../../.agents`; если агент ищет skills в другой папке, сделайте символическую ссылку, а не копию, чтобы правила не разъехались.

### Claude Code

- Установка: `curl -fsSL https://claude.ai/install.sh | bash` (или `npm install -g @anthropic-ai/claude-code`), затем `claude` в корне проекта и вход в аккаунт.
- Skills: проектные — `../../../../.agents`, личные — `~/.claude/skills/<name>/SKILL.md`. Наш skill уже на месте.
- Вызов: автоматически по `description` или явно — `/rfr-page-objects`.
- MCP: `.mcp.json` в корне проекта подхватывается сам, при первом запуске подтвердите сервер `playwright`.
- Документация: <https://code.claude.com/docs/en/skills>.

### Cursor

- Установка: скачайте Cursor с <https://cursor.com/download> и откройте папку проекта.
- Skills: `.cursor/skills/`, `.agents/skills/` в проекте и `~/.cursor/skills/`, `~/.agents/skills/` у пользователя. Для совместимости Cursor читает и `../../../../.agents`, так что наш skill подхватится без изменений.
- Вызов: автоматически по `description` или в чате агента — `/` и имя skill.
- MCP: `.cursor/mcp.json` в том же формате, что `.mcp.json`.
- Документация: <https://cursor.com/docs/skills>.

### Codex

- Установка: `npm install -g @openai/codex`, затем `codex` в корне проекта и вход через аккаунт ChatGPT.
- Skills: `.agents/skills/` от текущей папки до корня репозитория и `~/.agents/skills/` у пользователя. Подключите наш skill ссылкой:

  ```bash
  mkdir -p .agents/skills
  ln -s ../../.claude/skills/rfr-page-objects .agents/skills/rfr-page-objects
  ```

- Вызов: автоматически по `description`, через `/skills` или упоминанием `$rfr-page-objects` в запросе.
- MCP: в `~/.codex/config.toml`

  ```toml
  [mcp_servers.playwright]
  command = "npx"
  args = ["@playwright/mcp@0.0.83", "--isolated"]
  ```

- Документация: <https://developers.openai.com/codex/skills>.

### GigaCode

- Установка: плагин GigaCode для VS Code или IDE JetBrains (IntelliJ IDEA, GigaIDE) из маркетплейса IDE, вход через GitVerse; включите агентный режим.
- Skills (VS Code): `.gigacode/skills/<name>/SKILL.md` в проекте. Подключите наш skill ссылкой:

  ```bash
  mkdir -p .gigacode/skills
  ln -s ../../.claude/skills/rfr-page-objects .gigacode/skills/rfr-page-objects
  ```

- Вызов: автоматически по `description`; список и отключение skills — в настройках GigaCode, раздел Skills. Если skill не применился, начните новую сессию.
- Для JetBrains и MCP сверьтесь с актуальной документацией: <https://gitverse.ru/docs/ai/ai-assistant-gigacode/gigacode-for-vscode/skills>.

### Проверка настройки

Спросите агента: «Какие правила skill `rfr-page-objects` относятся к конструктору и проверкам?» и попросите через Playwright MCP перечислить роли и доступные имена элементов на `http://localhost:3001`. Приложите оба ответа к PR.

## Часть 3. Наращиваем покрытие с агентом

Код пишет агент по skill, вы ставите задачу, ревьюите результат по чек-листу skill и подтверждаете прогоном Gradle. Проход сценария через MCP — не запуск теста.

1. **`ui/page/ProfilePage` целиком**: First Name, Surname, Location, аватар (`setInputFiles`), Save, Reset. Подсказки:
   - Location — MUI `Select`, то есть `combobox` с именем `Location`. Option ищите по флагу — `getByRole(OPTION)` с фильтром по `IMG` с точным именем страны: имя самого option выглядит как «France France».
   - `clickSave()` должен дождаться ответа GraphQL-операции `UpdateUser` через `page.waitForResponse(..., save::click)`, иначе `reload()` обгонит мутацию (почему — на занятии 6).
   - Аватар после сохранения — `img` в `main` с `src` вида `data:image/...`.
2. **`test/profile/ProfileTest`** на пользователе из `@User`: сохранение каждого поля и чтение после reload, Reset возвращает сохранённые значения, смена страны (страна выбирается детерминированно, без случайности).
3. **`test/auth/LoginNegativeTest`**: неверный пароль и несуществующий пользователь — в обоих случаях `Bad credentials` (Spring Security скрывает `UsernameNotFoundException`). Локатор ошибки — `.form__error` шаблона `rfr-auth`: у `<p>` нет роли.
4. **1–2 новых правила в `rfr-page-objects`**, выведенные из практики, например про `combobox`/option или про ожидание ответа в действии.

Постановку задачи агенту стройте так: цель, контекст (skill, пакеты, что mock), критерии приёмки, «сначала предложи публичные методы, код — после моего ОК». В PR зафиксируйте, какое замечание ревью превратилось в правило skill.

