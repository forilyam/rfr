---
name: rfr-page-objects
description: Создаёт и меняет Page Objects и компоненты UI Rangiffler на Playwright Java в rfr-e2e (пакеты ui/page и ui/component). Применять к страницам, локаторам, действиям и проверкам UI. Фикстуры, SQL и JUnit extensions — вне ответственности страниц.
---

# Page Objects Rangiffler

Страницы — `rfr-e2e/src/test/java/io/student/rangiffler/ui/page`, общие части — `ui/component`, адреса — `config/TestConfig`.
Доступные имена и роли элементов подтверждай в браузере через Playwright MCP (`http://localhost:3001`), не угадывай по исходникам фронта.

## Контракт

1. Класс `final`, поля `private final`: `Page`, `Locator`, компоненты. Исключение — `public static final String URL` с базой из `TestConfig`; он есть у каждой страницы, даже если на неё попадают только переходом.
2. Конструктор принимает только `Page` и только присваивает поля: без переходов, кликов, проверок, ожиданий и SQL.
3. Композиция вместо наследования: общие части (`CredentialsForm`, `HeaderComponent`) — компоненты с тем же `Page`, базовых классов нет.
4. Каждый публичный метод страницы и компонента — с `@Step`, и каждый параметр попадает в текст шага: `@Step("Заполнить форму входа: {username} / {password}")`.
5. Переход возвращает объект следующей страницы, действие и проверка на текущей — `this`.
6. Одно бизнес-действие — один метод, имя которого называет форму: `fillLoginForm(username, password)`, `fillRegistrationForm(...)`. Без безликих `fillForm`/`fill`/`submit` и цепочек `fillUsername().fillPassword()`; отдельный метод на поле — только если тесту нужно именно поле (валидация).
7. Действие отдельно от проверки: `fill…`/`click…` ничего не проверяют; `checkThat…` только проверяют через `PlaywrightAssertions.assertThat(locator)` и ничего не кликают. Без `locator.isVisible()` в JUnit-проверках и ручных ожиданий.
8. Без `checkThatOpened`, `checkPageLoaded`, `isLoaded`, `waitUntilLoaded`: `click`/`fill` ждут элемент, web-first assertions ждут состояние. URL и видимость страницы проверяем, только когда переход и есть предмет теста.
9. Локаторы: `getByRole` с точным `name`, `getByLabel`, `getByPlaceholder`, `getByText` с точным совпадением, `getByTestId`. Без `first()`/`nth()`, MUI-классов и XPath; неуникальный элемент сужай родителем: `page.getByRole(AriaRole.BANNER).getByRole(…)`.
10. Проверка не должна быть тавтологией локатора: `assertThat(heading("Travels map")).hasText("Travels map")` ничего не доказывает. Ожидаемое значение приходит из теста (`checkThatUsername(username)`).
11. Вход только через UI; `access_token`/`id_token` в `localStorage` не подставлять (до занятия 13).
12. Без комментариев и Javadoc: смысл передают имена методов и текст `@Step`.
13. MUI Select открывай через `getByRole(COMBOBOX)` с точным именем поля. Для `Location` выбирай `getByRole(OPTION)` с фильтром `setHas(getByRole(IMG))` по точному имени страны: доступное имя option может содержать повтор, например «France France». Страну передаёт тест; не выбирай случайный option или элемент по индексу.
14. Если действие запускает сохранение, от которого зависит следующий шаг, ожидай ответ внутри метода действия: `page.waitForResponse(predicate, save::click)`. Для GraphQL predicate должен различать операции по `operationName` (например, `UpdateUser`), а не только по URL `/graphql`. При необходимости дождись тела ответа через `.finished()`, прежде чем возвращать управление для `reload()`. Это синхронизация действия, а не проверка результата: значения после сохранения проверяй отдельно через web-first assertions, без `sleep`.

## Проверка результата

Прогон `./gradlew :rfr-e2e:test --tests '<Тест>' --rerun-tasks` — единственное подтверждение. Проход сценария через MCP запуском теста не считается.
В отчёте разделяй: что подтверждено в браузере через MCP, что взято из исходников.
