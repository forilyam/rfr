# RFR-AUTH - Authorization Server

## Описание

`rfr-auth` - сервис регистрации и аутентификации Rangiffler на базе Spring Authorization Server (OAuth 2.1 / OpenID Connect 1.0).
Он хранит учетные записи, показывает страницы логина и регистрации и выдает токены фронтенду по
Authorization Code + PKCE. `rfr-api` проверяет выданные access token по JWKS этого сервиса.

## Технологический стек

- **Java 25**, Gradle 9.7.1 (wrapper в корне проекта)
- **Spring Boot 4.1.1**
- **Spring Security 7** + **Spring Authorization Server** - OAuth 2.1 / OIDC
- **Spring Data JPA**, **Flyway**, **MySQL 8.4** (схема `rangiffler-auth`)
- **Thymeleaf** - страницы login / register / error

## Структура модуля

```
rfr-auth/src/main/
├── java/io/student/rangiffler/
│   ├── RangifflerAuthApplication.java
│   ├── config/
│   │   ├── RangifflerAuthServiceConfig.java                  # Два SecurityFilterChain, JWKS, PasswordEncoder
│   │   ├── AccessTokenCustomizer.java                        # aud и authorities в access token
│   │   ├── PublicClientRevocationAuthenticationConverter.java # /oauth2/revoke для публичного клиента
│   │   ├── PublicClientRevocationAuthenticationProvider.java
│   │   └── ThemeCookieFilter.java                            # ?theme=light|dark -> cookie rangiffler-theme
│   ├── controller/
│   │   ├── LoginController.java                              # GET /login, GET /
│   │   ├── RegisterController.java                           # GET/POST /register
│   │   └── CustomErrorController.java                        # /error
│   ├── data/                                                 # UserEntity, AuthorityEntity, Authority (read, write)
│   │   └── repository/UserRepository.java
│   ├── model/RegistrationForm.java                           # Форма регистрации + валидация
│   ├── service/
│   │   ├── api/UserService.java, impl/UserServiceImpl.java   # Регистрация
│   │   ├── impl/DatabaseUserDetailsService.java              # UserDetails из БД
│   │   ├── OidcClearCookiesLogoutHandler.java                # Очистка JSESSIONID и XSRF-TOKEN при OIDC logout
│   │   ├── GlobalExceptionHandler.java
│   │   └── cors/CorsCustomizer.java                          # CORS для фронтенда (3001)
│   └── validation/                                           # @EqualPasswords, @NoWhitespace
└── resources/
    ├── application.yaml                                      # Порт 9001, OAuth-клиент, TTL токенов
    ├── db/migration/rangiffler-auth/V1__schema_init.sql
    ├── templates/                                            # login.html, register.html, error.html
    └── static/                                               # styles, images, fonts
```

## OAuth-клиент

Клиент фронтенда регистрируется свойствами Spring Boot в `application.yaml`
(`spring.security.oauth2.authorizationserver.client.oidc-client`):

| Параметр | Значение |
|---|---|
| `client-id` | `client` |
| Аутентификация клиента | `none` - публичный клиент (SPA), секрета нет |
| Grant type | `authorization_code`, PKCE обязателен (`require-proof-key: true`) |
| Redirect URI | `http://localhost:3001/authorized` |
| Post-logout redirect URI | `http://localhost:3001/logout` |
| Scopes | `openid`, `profile` |
| Consent | `require-authorization-consent: true`, но фронтенд запрашивает только `scope=openid`, а для него Spring Authorization Server согласие не запрашивает - экрана согласия нет |
| Access token TTL | `30m` |
| Authorization code TTL | `5m` |

Refresh token публичному клиенту не выдается. После истечения access token фронтенд получает 401 от `rfr-api`
и пользователь заново проходит авторизацию; пока жива сессия на `rfr-auth` (30 минут), пароль вводить не нужно.

## Токены

| Токен | Кому предназначен | Содержимое | Время жизни |
|---|---|---|---|
| **access token** (JWT) | `rfr-api` | `sub` = username, `aud: ["rangiffler-api"]`, `authorities: ["read","write"]`, `scope` | 30 минут (`access-token-time-to-live: 30m`) |
| **id_token** (JWT) | Фронтенд (`aud` = `client`) | `sub`, `sid` и др. OIDC-claims | 30 минут (фиксировано в Spring Authorization Server) |

- `aud` и `authorities` добавляет `AccessTokenCustomizer` только в access token. Authorities берутся из таблицы `authority`
  пользователя; служебные `FACTOR_*` (их добавляет Spring Security 7) отбрасываются. Значение audience - свойство
  `rangiffler-api.audience`.
- `rfr-api` принимает только access token: проверяет audience и требует `read` для query и `write` для mutation.
  `id_token` фронтенд использует только как `id_token_hint` при logout.
- Ключ подписи RSA генерируется при каждом старте (`jwkSource()`): после перезапуска `rfr-auth` все ранее выданные
  токены становятся невалидными.

## Endpoints

| Endpoint | Назначение |
|---|---|
| `GET /oauth2/authorize` | Authorization endpoint (code + PKCE) |
| `POST /oauth2/token` | Обмен code на токены |
| `POST /oauth2/revoke` | Отзыв токена (RFC 7009); для публичного клиента достаточно `client_id` |
| `POST /oauth2/introspect` | Introspection (требует confidential client, фронтенд не использует) |
| `GET /oauth2/jwks` | JWKS для проверки подписи |
| `GET /.well-known/openid-configuration` | OIDC discovery |
| `GET /userinfo` | OIDC UserInfo |
| `GET/POST /connect/logout` | OIDC RP-initiated logout |
| `GET /login`, `POST /login` | Страница и форма логина |
| `GET /register`, `POST /register` | Страница и форма регистрации |

### Security filter chains

1. **Order 1 - Authorization Server**: все OAuth/OIDC endpoints. Неаутентифицированный браузерный запрос
   (`text/html`) перенаправляется на `/login`. Здесь же подключены CORS и аутентификация публичного клиента на `/oauth2/revoke`.
2. **Order 2 - приложение**: публичные `/.well-known/**`, `/register`, `/error`, статика; form login на `/login`.

`GET /login` без сохраненного OAuth-запроса (`/oauth2/authorize` с redirect URI фронтенда) перенаправляет на фронтенд -
вход нужно начинать с `/oauth2/authorize` или кнопки Login на фронтенде.

### Отзыв токена публичным клиентом

Spring Authorization Server аутентифицирует клиента с методом `none` только на token endpoint при наличии PKCE
`code_verifier`, поэтому `/oauth2/revoke` из SPA из коробки отвечает `401 invalid_client`.
`PublicClientRevocationAuthenticationConverter` и `PublicClientRevocationAuthenticationProvider` опознают
публичного клиента по `client_id` только на revocation endpoint (так разрешает RFC 7009); остальные endpoints
используют стандартную аутентификацию клиентов.

```bash
curl -X POST http://localhost:9001/oauth2/revoke \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "token=<access_token>&token_type_hint=access_token&client_id=client"
# 200 OK (и для неизвестного токена - так требует RFC 7009)
```

Отзыв инвалидирует токен в хранилище авторизаций `rfr-auth`. `rfr-api` проверяет JWT локально по подписи и про отзыв
не знает, поэтому отозванный access token принимается API до `exp` (не более 30 минут).

## Основные процессы

### Регистрация

`POST /register` - `application/x-www-form-urlencoded` с CSRF-токеном той же сессии (сначала `GET /register`).

| Поле | Правила |
|---|---|
| `username` | Не пустой, 3-50 символов, без whitespace |
| `password` | Не пустой, 3-12 символов, без whitespace |
| `passwordSubmit` | Совпадает с `password` |

Whitespace проверяется через `Character.isWhitespace` (пробел, таб, перевод строки и т. п.; неразрывный пробел U+00A0 к ним не относится).
Успех - HTTP 201 и страница "Congratulations! You've registered!". Ошибка валидации или занятый username
(`` Username `x` already exists ``) - HTTP 400 и та же форма с сообщением.
Пользователь получает authorities `read` и `write`. Пароль хранится через `DelegatingPasswordEncoder` (`{bcrypt}`).

Регистрация создает запись только в `rangiffler-auth`. API-профиль в `rangiffler-api` появляется при первом
авторизованном `Query.user`.

### Вход: Authorization Code + PKCE

```
1. Frontend -> GET /oauth2/authorize?response_type=code&client_id=client&scope=openid
               &redirect_uri=http://localhost:3001/authorized
               &code_challenge=<S256>&code_challenge_method=S256
2. rfr-auth -> 302 /login (если нет сессии)
3. User     -> POST /login (username, password, _csrf)
4. rfr-auth -> 302 http://localhost:3001/authorized?code=...   (экран согласия появится, только если запросить scope кроме openid)
5. Frontend -> POST /oauth2/token
               grant_type=authorization_code&code=...&redirect_uri=...&client_id=client&code_verifier=...
6. rfr-auth -> { "access_token": "...", "id_token": "...", "token_type": "Bearer", "expires_in": 1799, "scope": "openid" }
```

Фронтенд сохраняет `access_token` и `id_token` в localStorage и передает в `rfr-api` только access token.

### Logout

1. Фронтенд отзывает access token: `POST /oauth2/revoke` (ошибка не блокирует logout).
2. Переход на `/connect/logout?id_token_hint=<id_token>&post_logout_redirect_uri={front url}/logout`.
3. `OidcClearCookiesLogoutHandler` очищает `JSESSIONID` и `XSRF-TOKEN`, сессия `rfr-auth` завершается.
4. Редирект на `/logout` фронтенда, который удаляет токены и PKCE из localStorage.

### Тема оформления

`ThemeCookieFilter` сохраняет параметр `?theme=light|dark` (фронтенд добавляет его к ссылкам на auth) в cookie
`rangiffler-theme` на год. Без параметра и cookie страницы используют системную схему.

## База данных

Схема `rangiffler-auth` (создается автоматически), миграция `db/migration/rangiffler-auth/V1__schema_init.sql`:

| Таблица | Содержимое |
|---|---|
| `user` | UUID, уникальный username (до 50), закодированный пароль, флаги `enabled`, `account_non_expired`, `account_non_locked`, `credentials_non_expired` |
| `authority` | `user_id`, `authority` - `read` / `write` |

OAuth-авторизации, consent и ключи в БД не хранятся (in-memory) и теряются при перезапуске.

## Запуск

Требования: JDK 25 (Gradle toolchain), Docker для MySQL.

```bash
# из корня проекта
docker compose up -d mysql
./gradlew :rfr-auth:bootRun
# или main class io.student.rangiffler.RangifflerAuthApplication из IDE
```

Проверка:

```bash
curl http://localhost:9001/.well-known/openid-configuration
```

## Troubleshooting

- **Токены перестали проходить после перезапуска `rfr-auth`.** Ключ подписи генерируется при старте - войдите заново.
- **`rfr-api` отвечает 401 на валидный на вид токен.** Проверьте, что в Bearer передается access token, а не id_token
  (у id_token `aud` = `client`), и что у токена не истёк `exp` (TTL access token — 30 минут).
- **CORS-ошибки.** Фронтенд должен работать на `rangiffler-front.base-uri` (`http://localhost:3001`); origins задает `CorsCustomizer`.
- **`GET /login` сразу уводит на фронтенд.** Это ожидаемо без сохраненного OAuth-запроса - начинайте с `/oauth2/authorize`.
