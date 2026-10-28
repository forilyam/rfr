# UI Rangiffler: роли и имена элементов (снимок для занятия 1)

Запасной источник на случай, если Playwright MCP недоступен на эфире. Снято с `rfr-gql-client` на `course-base`; перед записью сверить в браузере.

| Страница | URL | Элементы (role · name) |
|---|---|---|
| Landing | `/` | button «Login»; link «Register» |
| Registration (rfr-auth) | `:9001/register` | textbox placeholder «Type your username»; textbox placeholder «Type your password»; textbox placeholder «Repeat your password»; button «Sign up»; после успеха — text «Congratulations! You've registered!», link «Sign in» |
| Login (rfr-auth) | `:9001/login` | textbox placeholder «Type your username»; textbox placeholder «Type your password»; button «Sign in» |
| Main | `/my-travels` | heading «Travels map»; heading «Photos»; button «Only my travels» (`aria-pressed`); button «With friends»; button «Add photo»; banner: heading «Rangiffler», link «Profile», button «Logout» |
| Profile | `/profile` | textbox label «Username» (disabled); textbox label «First Name»; textbox label «Surname»; combobox «Location»; label «Update avatar» (file input); button «Save»; button «Reset» |

Регистрация и вход — страницы `rfr-auth` (Thymeleaf), остальное — React. Прямой переход на `/login` не запускает OAuth/PKCE: вход только с кнопки «Login» гостевой страницы.
