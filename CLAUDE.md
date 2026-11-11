# DnsConf — заметки для Claude Code

Консольная утилита на Java 25: загружает hosts-файлы и синхронизирует правила
блокировки/перенаправления в Cloudflare Zero Trust или NextDNS. Запускается
ежедневно через GitHub Actions (`.github/workflows/github_action.yml`).

## Команды

- Сборка + тесты: `mvn -B clean package`
- Только тесты: `mvn -B test`
- Запуск: `java --enable-preview -jar target/dns-block-and-redirect-configurer-1.0-SNAPSHOT.jar`
  (нужны переменные `DNS`, `CLIENT_ID`, `AUTH_SECRET`, опционально `BLOCK`, `REDIRECT`, `NEXTDNS_CLEAR_ALL`)

Нужен JDK 25 (используется preview-API `StructuredTaskScope`, флаг `--enable-preview`
обязателен и при сборке, и при запуске).

## Документация

Вся документация — на русском, в `docs/` (точка входа `docs/README.md`).
При изменении кода обновляйте соответствующие страницы по таблице в
`docs/dev/docs-guide.md`; известные ошибки — в `docs/known-issues.md`.

## Важно

- Не запускайте jar с настоящими ключами без явной просьбы: он меняет
  настройки реального DNS-аккаунта.
- Тесты не поднимают Spring-контекст: не вызывайте в них `toJson()` и всё,
  что обращается к `App.context`.
