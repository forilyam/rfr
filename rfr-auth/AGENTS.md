# rfr-auth

- Не объединяй две security filter chains без отдельного согласования.
- Не меняй уже применённые миграции Flyway; добавляй новые.
- При изменении issuer, audience, claims, CORS или redirect URI проверяй совместимость с `rfr-api` и `rfr-gql-client`.
- При изменении страниц сохраняй CSRF-защиту и проверяй светлую и тёмную темы.
