# rfr-gql-client - Frontend приложение Rangiffler

## Описание

`rfr-gql-client` - SPA на React для Rangiffler: карта путешествий, лента фотографий, люди и дружба, профиль.
С бэкендом общается через GraphQL (`rfr-api`), аутентифицируется в `rfr-auth` по OAuth 2.1 Authorization Code + PKCE.

Лента, фото и лайки на бэкенде пока mock: интерфейс показывает успешные операции, но после перечитывания ленты
данные возвращаются к содержимому JSON (подробнее - README `rfr-api`).

## Технологический стек

- **React 19**, **TypeScript 6**
- **Vite 8** - сборщик и dev-сервер
- **Apollo Client 4** - GraphQL клиент (`@apollo/client/react` для хуков)
- **MUI 9** - компоненты, светлая и темная тема (CSS variables)
- **React Router 8** - маршрутизация
- **crypto-js** - PKCE (`code_verifier`, `code_challenge`)
- **react-svg-worldmap** - карта мира
- **ESLint 10** (flat config, `eslint.config.js`), `typescript-eslint`

Для установленных зависимостей нужен Node.js 22 (не ниже 22.22.0) либо 24+.
Ограничение учитывает `engines` React Router и ESLint из `package-lock.json`, а не только Vite.

## Структура проекта

```
rfr-gql-client/src/
├── api/
│   ├── apolloClient.ts     # Apollo: Bearer access token, обработка 401, кэш
│   ├── authClient.ts       # POST /oauth2/token и /oauth2/revoke
│   ├── authUtils.ts        # PKCE, ссылки authorize/register/logout, токены в localStorage
│   └── graphqlError.ts     # Текст ошибки для пользователя по extensions.classification
├── components/
│   ├── AppContent/         # Маршруты
│   ├── PrivateRoute/       # Защищенные маршруты: GetUser + провайдеры контекстов
│   ├── MenuAppBar/         # Верхняя панель: профиль, переключение темы, Logout
│   ├── Drawer/, Sidebar/   # Боковое меню
│   ├── CollapsibleMapCard/ # Сворачиваемая карточка карты
│   ├── WorldMap/           # Карта со статистикой, клик по стране = фильтр ленты
│   ├── VisitedCountries/   # Всплывающий список "N countries visited"
│   ├── Toggle/             # "Only my" / "With friends"
│   ├── PhotoContainer/     # Сетка фото, скелетоны, бесконечная прокрутка, пустые состояния
│   ├── PhotoCard/          # Карточка: фото 4:3, страна, лайк, меню владельца (Edit / Delete)
│   ├── AddPhotoFab/        # Плавающая кнопка "Add photo"
│   ├── PhotoModal/         # Компонент PhotoModal и formValidate.ts; активный диалог — в context/DialogContext.tsx
│   ├── ImageUpload/, CountrySelect/
│   ├── ProfileForm/        # Форма профиля
│   ├── PeopleTable/        # All / Friends / Income / Outcome, карточки на узких экранах
│   ├── Table/              # ActionButtons, TableToolbar (поиск), Pagination, TableHead
│   ├── QueryErrorAlert/    # Ошибка запроса с кнопкой Retry
│   ├── Loader/, TabPanel/
├── context/                # SessionContext, CountriesContext, DialogContext (форма фото), SnackBarContext + хуки use*
├── hooks/                  # По хуку на GraphQL-операцию (см. ниже)
├── pages/                  # LandingPage, Redirect, Authorized, Logout, MyTravelsPage, PeoplePage, ProfilePage
├── types/                  # Connection, Country, Likes, Order, Photo, Stat, User
├── utils/                  # arrays, comparator
├── theme.tsx               # Тема MUI (light / dark)
├── App.tsx, main.tsx
```

## Маршруты

| Путь | Назначение |
|---|---|
| `/` | Landing page: Login и Register; если пользователь уже вошел - переход в приложение |
| `/redirect` | Генерирует PKCE и уводит на `rfr-auth` `/oauth2/authorize` |
| `/authorized` | OAuth callback: обмен `code` на токены |
| `/logout` | Post-logout redirect: удаление токенов и PKCE из localStorage |
| `/my-travels` | Карта и лента (защищенный) |
| `/people` | Люди и дружба (защищенный) |
| `/profile` | Профиль (защищенный) |

Защищенные маршруты оборачивает `PrivateRoute`: он выполняет `GetUser` (этот запрос же создает API-профиль при первом входе)
при ошибке `UNAUTHORIZED`/`FORBIDDEN` уводит на `/`, при других ошибках показывает `QueryErrorAlert` с Retry.

## Аутентификация

1. Кнопка Login на `/` (или маршрут `/redirect`) генерирует `code_verifier` и `code_challenge` (S256), сохраняет их в localStorage и открывает
   `/oauth2/authorize?response_type=code&client_id=client&scope=openid&redirect_uri=.../authorized&code_challenge=...&theme=...`.
2. После логина `rfr-auth` возвращает на `/authorized?code=...`.
3. `AuthorizedPage` меняет `code` + `code_verifier` на токены (`POST /oauth2/token`) и сохраняет
   `access_token` и `id_token`.
4. Apollo передает в `rfr-api` **только access token**: `Authorization: Bearer <access_token>`.
5. HTTP 401 от `rfr-api` (истекший или невалидный токен) - `ErrorLink` очищает сессию и уводит на `/`.
6. Logout (`MenuAppBar`): `POST /oauth2/revoke` для access token -> очистка Apollo store ->
   `/connect/logout?id_token_hint=<id_token>&post_logout_redirect_uri=.../logout` -> `/logout` удаляет токены и PKCE из localStorage.

Access token живет 30 минут (настройка `rfr-auth`), refresh token публичному клиенту не выдается: после истечения пользователь входит заново
(пока жива сессия `rfr-auth`, без ввода пароля).

### localStorage

| Ключ | Значение |
|---|---|
| `access_token` | Токен для `rfr-api` |
| `id_token` | Только для `id_token_hint` при logout |
| `codeVerifier`, `codeChallenge` | PKCE текущего входа |
| `mui-mode` | `light` / `dark` / `system`; передается в `rfr-auth` параметром `theme` |
| `rangiffler-map-collapsed` | Свернута ли карта |

## GraphQL-операции

Каждая операция инкапсулирована в хук в `src/hooks`. Имена операций удобны для перехвата в тестах (`operationName`):

| Хук | Операция | Назначение |
|---|---|---|
| `useGetUser` | `query GetUser` | Текущий пользователь |
| `useGetCountries` | `query GetCountries` | Справочник стран |
| `useGetFeed` | `query GetFeed` | Лента (`withFriends`, `country`, `page`, `size=12`), статистика; `fetchMore` для бесконечной прокрутки |
| `useQueryPeople` | `query GetPeople` | All People с поиском и пагинацией |
| `useGetFriends` | `query GetFriends` | Друзья |
| `useGetInvitations` | `query GetInvitations` | Входящие заявки |
| `useGetOutcomeInvitations` | `query GetOutcomeInvitations` | Исходящие заявки |
| `useUpdateUser` | `mutation UpdateUser` | `user(input)` - профиль |
| `useUpdateFriendshipStatus` | `mutation FriendshipAction` | `friendship(input)` - ADD / ACCEPT / REJECT / DELETE; перечитывает 4 people-запроса |
| `useCreatePhoto` | `mutation CreatePhoto` | `photo(input)` без id; перечитывает `GetFeed` |
| `useUpdatePhoto` | `mutation UpdatePhoto` | `photo(input)` с id; перечитывает `GetFeed` |
| `useLikePhoto` | `mutation LikePhoto` | `photo(input)` с `like` |
| `useDeletePhoto` | `mutation DeletePhoto` | `deletePhoto(id)`; перечитывает `GetFeed` |

Ошибки с `extensions.classification` `BAD_REQUEST` / `NOT_FOUND` показываются пользователю текстом с сервера
(snackbar или `QueryErrorAlert`), остальные - общим сообщением.

Кэш Apollo нормализует сущности по `id`, `Feed` сливается (`merge: true`). Ответ мутации сразу обновляет карточку,
поэтому успешный snackbar или обновленная карточка не доказывают сохранение - для проверки нужно независимое чтение.

## Конфигурация

`.env`:

```env
VITE_AUTH_URL=http://localhost:9001
VITE_API_URL=http://localhost:8081
VITE_FRONT_HOST=localhost
VITE_FRONT_URL=http://localhost:3001
VITE_CLIENT_ID=client
```

Dev-сервер слушает `VITE_FRONT_HOST:3001` (`vite.config.ts`). Порт должен совпадать с redirect URI клиента в `rfr-auth`.

## Запуск

```bash
cd rfr-gql-client
npm ci          # или npm install
npm run dev     # http://localhost:3001
```

Для работы нужны запущенные `rfr-auth` (9001) и `rfr-api` (8081).

Сборка и проверки:

```bash
npm run build   # tsc + vite build -> dist/
npm run lint    # eslint, --max-warnings 0
npm run preview # раздать dist/
```
