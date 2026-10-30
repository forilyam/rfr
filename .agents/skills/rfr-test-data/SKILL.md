---
name: rfr-test-data
description: Готовит и ревьюит тестовые данные и предусловия rfr-e2e. Применять к @User, UserRequest, TestUser, декораторам UserFixture и объектам data/db, при проверке уникальности, создания профилей, очистки и использования заранее заведённых пользователей.
---

# Тестовые данные rfr-e2e — v1

Работай с данными в rfr-e2e/src/test/java/io/student/rangiffler/: data, data/db и предусловиями jupiter/annotation/User; тесты используют готовый TestUser через @User.

**Уникальность данных.** Основному пользователю и партнёрам — отдельный TestUser.unique(), как в UserRequest.fixture() и декораторах: RandomDataUtils даёт rfr_ + часть UUID, без гарантии отсутствия коллизий. Без постоянных логинов, общего изменяемого TestUser и зависимости от порядка тестов.

**Владелец — дерево декораторов.** Корень UserFixture из UserRequest.fixture() регистрируется в ExtensionContext.Store до create(); Store закрывает его как AutoCloseable, UserWithRelationsFixture владеет origin, partners и своими Friendship. Для каждой записи проследи получение, хранение и освобождение: TearDown удаляет свои связи → партнёров → origin даже после ошибок и частичного create(). Удаление — по username или паре, без широких DELETE/TRUNCATE; статика, ThreadLocal и поля extension'а данными не владеют.

**Новая связь — значение Relation.** Новому виду связи достаточно значения Relation со статусом Friendship, направлением и ссылкой TestUser::with…; создание и очистку партнёров SingleUserFixture выполняет общий UserWithRelationsFixture. Счётчик проходит @User → UserRequest(User) → UserRequest.fixture(), список и with… в TestUser сохраняют остальные связи; UserRequest отклоняет отрицательные значения до фабрики. Новый декоратор для связи не нужен; без флагов в SingleUserFixture и веток подготовки в UserExtension.

**SQL — только в объектах строк data/db.** Фикстуры вызывают create/delete/exist у AuthAccount, ApiProfile, Friendship, Invitation и Friendships, без прямого JdbcClient. Объекты data/db выполняют SQL из src/test/resources/sql/ через Jdbc; в тестах, jupiter и Page Objects SQL нет.

**Профиль API создаётся явно.** SingleUserFixture.create() создаёт AuthAccount в rangiffler-auth, затем ApiProfile в rangiffler-api с тем же username, и для партнёров тоже — до Friendship/Invitation. На первый вход или запрос API создание профиля не откладывай.

**Минимальные предусловия.** Для одной связи достаточно счётчика 1; смешанные friends/incomeInvitations/outcomeInvitations нужны для проверки разделения, как во вкладках People. Партнёры живут в TestUser и дереве фикстур, отдельный @User — для независимого участника.

**Пул — только когда создать нельзя.** Укажи подтверждённое ограничение, мешающее создать учётную запись; для заёмных пользователей есть StaticUserFixture, обычные связи готовятся через TestUser.unique() и декораторы. Заёмную запись выдавай эксклюзивно, восстанавливай и возвращай в пул, не удаляй как собственную.

## Проверка результата

Перед запуском назови тесты; для БД нужен MySQL, для UI — также оба бэкенда и фронтенд.
Запускай `./gradlew :rfr-e2e:test --tests '*UserExtensionTest'`; для объектов связей добавь `--tests '*FriendshipTest'`, для UI выбери затронутый test/people/*TabTest.
В UserExtensionTest EngineTestKit и exist() из data/db проверяют PENDING и профиль внутри OutcomeInvitationCase, их отсутствие после закрытия Store и IllegalArgumentException при -1 до фабрики в InvalidOutcomeCase; ReportedFixture подтверждает лишь вызовы жизненного цикла.
В отчёте: владелец, получение, хранение, освобождение; для дефекта — файл, строка, воспроизведение; списки «подтверждено запуском», «по чтению кода», «не проверено». Если дефектов нет, скажи об этом и перечисли ограничения проверки.
