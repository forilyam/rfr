# RFR-API - GraphQL Backend

## Описание

`rfr-api` - бэкенд проекта Rangiffler с GraphQL API, работающий как OAuth 2.0 Resource Server.
Пользователи, страны и дружба реализованы на реальной БД; фотографии, лайки, лента и статистика пока
отдаются mock-слоем из JSON-файлов и **ничего не сохраняют**.

## Технологический стек

- **Java 25**, Gradle 9.7.1 (wrapper в корне проекта)
- **Spring Boot 4.1.1** (Jackson 3)
- **Spring for GraphQL** + **DGS Codegen** - Java-модель генерируется из схемы
- **Spring Security 7** - OAuth 2.0 Resource Server (JWT), `@PreAuthorize` на контроллерах
- **Spring Data JPA / Hibernate** - работа с БД
- **Flyway** - миграции
- **MySQL 8.4**, схема `rangiffler-api`

## Структура модуля

```
rfr-api/
├── build.gradle                                   # Spring Boot + DGS codegen (generateJava)
└── src/main/
    ├── java/io/student/rangiffler/
    │   ├── RangifflerApiApplication.java          # Точка входа
    │   ├── config/
    │   │   ├── RangifflerApiConfiguration.java    # Security: resource server, audience, authorities, CORS
    │   │   └── GraphQlExceptionResolver.java      # Исключения -> GraphQL errors (BAD_REQUEST / NOT_FOUND)
    │   ├── controller/
    │   │   ├── CountryQueryController.java        # Query.countries
    │   │   ├── UserQueryController.java           # Query.user, Query.users, User.friends/...Invitations
    │   │   ├── UserMutationController.java        # Mutation.user, Mutation.friendship
    │   │   ├── FeedMockQueryController.java       # Query.feed, Feed.photos, Feed.stat, Photo.likes (mock)
    │   │   └── PhotoMockMutationController.java   # Mutation.photo, Mutation.deletePhoto (mock)
    │   ├── data/
    │   │   ├── entity/                            # CountryEntity, UserEntity, FriendshipEntity, FriendshipStatus
    │   │   ├── projection/UserWithStatus.java     # Пользователь + статус дружбы относительно текущего
    │   │   └── repository/                        # CountryRepository, UserRepository, FriendshipRepository
    │   ├── exception/                             # ResourceNotFoundException, FriendshipActionException
    │   ├── service/
    │   │   ├── api/                               # CountryService, UserService
    │   │   ├── impl/                              # CountryServiceImpl, UserServiceImpl
    │   │   ├── mock/FeedMockData.java             # Чтение mock-ленты из JSON
    │   │   └── cors/CorsCustomizer.java           # CORS для фронтенда
    │   └── util/                                  # BytesAsString, StringAsBytes, GqlQueryPaginationAndSort
    └── resources/
        ├── application.yml
        ├── graphql/query.graphqls                 # GraphQL схема (источник для codegen)
        ├── db/migration/rangiffler-api/V1__schema_init.sql
        └── mock/
            ├── query_feed.json                    # Лента "Only my": 4 фото
            └── query_feed_with_friends.json       # Лента "With friends": те же 4 + 3 фото друзей
```

Сгенерированные классы (`io.student.rangiffler.model.types.*`, например `User`, `Photo`, `UserInput`) появляются в
`build/generated/sources/dgs-codegen` после `./gradlew :rfr-api:generateJava` (выполняется автоматически при сборке).
`UserConnection` и `PhotoConnection` маппятся на `org.springframework.data.domain.Page<...>`, скаляр `Date` - на `LocalDate`.

## Что реализовано, а что mock

| Возможность | Реализация |
|---|---|
| Профиль пользователя (`Query.user`, `Mutation.user`) | БД. Профиль создается при первом `Query.user` со страной `ru` |
| Справочник стран (`Query.countries`) | БД, заполняется миграцией (238 стран) |
| Люди, поиск, пагинация (`Query.users`) | БД, сортировка по username |
| Дружба (`Mutation.friendship`, `friends`, `incomeInvitations`, `outcomeInvitations`) | БД, одна строка на пару, проверка переходов |
| Лента и статистика (`Query.feed`) | Mock: JSON-файлы, одинаковые для всех пользователей; фильтр по стране по захардкоженному соответствию |
| Создание / редактирование / лайк фото (`Mutation.photo`) | Mock: возвращает корректный объект, но ничего не сохраняет |
| Удаление фото (`Mutation.deletePhoto`) | Mock: всегда `true`, ничего не удаляет |

Таблицы `photo`, `like`, `photo_like`, `statistic` уже есть в миграции, но приложение их не использует -
реализация фото, лайков и статистики - задача дипломного проекта.

## GraphQL API

Схема: `src/main/resources/graphql/query.graphqls`. Endpoint: `POST http://localhost:8081/graphql`.

### Queries

```graphql
type Query {
    countries: [Country!]!
    user: User!
    users(page:Int = 0, size:Int = 10, searchQuery:String): UserConnection
    feed(withFriends: Boolean!): Feed!
}
```

Текущий пользователь и его связи:

```graphql
query GetUser {
  user {
    id
    username
    firstname
    surname
    avatar
    location { code name flag }
    friends(page: 0, size: 10) {
      edges { node { id username friendStatus } }
      pageInfo { hasNextPage hasPreviousPage }
    }
  }
}
```

Лента с фильтром по стране:

```graphql
query GetFeed($withFriends: Boolean!, $country: String) {
  feed(withFriends: $withFriends) {
    photos(page: 0, size: 12, country: $country) {
      edges {
        node {
          id src description isOwner creationDate
          country { code name flag }
          likes { total likes { user username creationDate } }
        }
      }
      pageInfo { hasNextPage hasPreviousPage }
    }
    stat { count country { code } }
  }
}
```

`withFriends: false` читает `query_feed.json`, `true` - `query_feed_with_friends.json`. Список фото режется по `page/size`.
`Feed.stat` берется из JSON и не зависит от фильтра; в JSON у страны в `stat` есть только `code`.

### Mutations

```graphql
type Mutation {
    user(input: UserInput!): User!
    photo(input: PhotoInput!): Photo!
    deletePhoto(id: ID!): Boolean
    friendship(input: FriendshipInput!): User!
}
```

Обновление профиля (`null`/пропущенное поле - "не менять"):

```graphql
mutation UpdateUser($input: UserInput!) {
  user(input: $input) { id username firstname surname avatar location { code } }
}
```
```json
{ "input": { "firstname": "Ivan", "surname": "Petrov", "avatar": "data:image/png;base64,...", "location": { "code": "fr" } } }
```

Дружба:

```graphql
mutation FriendshipAction($input: FriendshipInput!) {
  friendship(input: $input) { id username friendStatus }
}
```
```json
{ "input": { "user": "<API UUID другого пользователя>", "action": "ADD" } }
```

| Действие | Кто может | Результат |
|---|---|---|
| `ADD` | Любой, если у пары еще нет записи | Строка `PENDING`, ответ `INVITATION_SENT` |
| `ACCEPT` | Только адресат заявки | `ACCEPTED`, ответ `FRIEND` |
| `REJECT` | Только адресат заявки | Строка удаляется, ответ `NOT_FRIEND` |
| `DELETE` | Любая сторона, любой статус | Строка удаляется: удаление друга или отмена исходящей заявки |

Фото (mock): создание требует `src` и `country`; при `id` существующего mock-фото и `like` - добавляется лайк:

```graphql
mutation CreatePhoto($input: PhotoInput!) {
  photo(input: $input) { id src description country { code name flag } likes { total } }
}
```
```json
{ "input": { "src": "data:image/jpeg;base64,...", "description": "Paris", "country": { "code": "fr" } } }
```

## Ошибки

Ошибки выполнения GraphQL-запроса могут прийти с HTTP 200; проверяйте `errors`.
Невалидный или просроченный Bearer-токен отклоняется фильтрами с HTTP 401 до GraphQL.
`GraphQlExceptionResolver` переводит исключения:

| Исключение | `extensions.classification` | Пример `message` |
|---|---|---|
| `ResourceNotFoundException` | `NOT_FOUND` | `Country not found by code: xx`, `User not found by id: ...` |
| `FriendshipActionException` | `BAD_REQUEST` | `Invitation already sent`, `No pending invitation from this user`, `Cannot perform friendship action on yourself` |
| `IllegalArgumentException` | `BAD_REQUEST` | `Photo country is required`, некорректный UUID, `size=0` |
| Нет токена / нет нужной authority | `UNAUTHORIZED` / `FORBIDDEN` | Spring for GraphQL |
| Прочие | `INTERNAL_ERROR` | Без деталей |

## Security

- `POST /graphql` пропускается фильтрами, доступ проверяется на контроллерах:
  query-контроллеры - `@PreAuthorize("hasAuthority('read')")`, mutation-контроллеры - `hasAuthority('write')`.
- JWT проверяется по JWKS `rfr-auth` (`spring.security.oauth2.resourceserver.jwt.issuer-uri`): подпись, `iss`, `exp`
  и **audience** (`jwt.audiences: rangiffler-api`). Принимается только access token; `id_token` (aud = client_id) отклоняется с 401.
- Claim `authorities` access token превращается в `GrantedAuthority` без префикса (`JwtAuthenticationConverter`
  в `RangifflerApiConfiguration`).
- Username текущего пользователя - claim `sub` (`@AuthenticationPrincipal Jwt`).
- Невалидный или просроченный Bearer-токен - HTTP 401 от фильтра, до GraphQL.
- GraphiQL (`/graphiql`) включен, но `GET` на него требует аутентификации - см. раздел Security config в корневом README.

## База данных

Схема `rangiffler-api` (создается автоматически, `createDatabaseIfNotExist=true`), миграция
`db/migration/rangiffler-api/V1__schema_init.sql`, `ddl-auto: none`.

| Таблица | Назначение |
|---|---|
| `user` | API-профиль: username (связь с `rfr-auth` только по username), имя, фамилия, аватар (LONGBLOB, data URL), страна |
| `country` | Справочник стран, флаги - data URL в BLOB |
| `friendship` | Одна строка на пару: `requester_id`, `addressee_id`, `status` (`PENDING`/`ACCEPTED`), `created_at`, `responded_at`, `version`; уникальный индекс неупорядоченной пары |
| `photo`, `like`, `photo_like`, `statistic` | Заготовлены, приложением пока не используются |

UUID пользователя в `rangiffler-api` и `rangiffler-auth` независимы.

## Запуск

Требования: JDK 25 (Gradle toolchain), Docker для MySQL, запущенный `rfr-auth` (нужен для JWKS).

```bash
# из корня проекта, при запущенном rfr-auth
docker compose up -d mysql
./gradlew :rfr-api:bootRun
# или main class io.student.rangiffler.RangifflerApiApplication из IDE
```

Проверка (access token можно взять в localStorage фронтенда после логина, ключ `access_token`):

```bash
curl -X POST http://localhost:8081/graphql \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <access_token>" \
  -d '{"query":"{ user { id username location { code } } }"}'
```
