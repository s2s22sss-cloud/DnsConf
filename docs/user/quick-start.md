# Быстрый старт (GitHub Actions)

Скрипт не нужно скачивать и устанавливать: GitHub сам соберёт и запустит его
в вашем форке по расписанию.

## 1. Выберите провайдера

| | Cloudflare Zero Trust | NextDNS |
|---|---|---|
| Бесплатный лимит запросов | 100 000 DNS-запросов в день | 300 000 DNS-запросов в месяц |
| Ограничение | IPv4-DNS привязан к одному IP (DoH, DoT, IPv6 — без ограничений) | API: ~60 запросов в минуту, синхронизация больших списков идёт долго |
| Подход скрипта | Каждый запуск полностью пересоздаёт свои списки и правила | Добавляет новое, обновляет изменившееся, остальное не трогает |
| Рекомендуется | `BLOCK` и `REDIRECT` | Только `REDIRECT`, блок-листы — встроенные на вкладке *Privacy* |

Подробно: [Cloudflare](cloudflare.md), [NextDNS](nextdns.md).

## 2. Получите ключи

- Cloudflare: [API-токен и Account ID](cloudflare.md#получение-учётных-данных).
- NextDNS: [API-ключ и ID профиля](nextdns.md#получение-учётных-данных).

## 3. Сделайте форк и создайте окружение

1. Нажмите **Fork** на странице репозитория.
2. В форке откройте *Settings → Environments → New environment*, имя — `DNS`
   (именно так, workflow ссылается на него по имени).
3. В **Environment secrets** добавьте:
   - `CLIENT_ID` — Account ID (Cloudflare) или ID профиля (NextDNS);
   - `AUTH_SECRET` — API-токен / API-ключ.
4. В **Environment variables** добавьте:
   - `DNS` — `Cloudflare` или `NextDNS` (регистр не важен);
   - `REDIRECT` — ссылки на hosts-файлы для перенаправлений через запятую;
   - `BLOCK` — ссылки на hosts-файлы для блокировок через запятую.

   `REDIRECT` и `BLOCK` можно не задавать. Что произойдёт без них, зависит от
   провайдера — см. [справочник переменных](configuration.md).

Пример значения `REDIRECT`:

```
https://raw.githubusercontent.com/Internet-Helper/GeoHideDNS/refs/heads/main/hosts/hosts
```

> Ставьте ссылки через запятую **без пробелов** и только прямые ссылки на файл
> (`raw.githubusercontent.com`, а не `github.com/.../blob/...`). Почему — в разделе
> [Источники данных](data-sources.md#требования-к-ссылкам).

## 4. Запустите

- Вручную: вкладка *Actions* → workflow
  **DNS Block&Redirect Configurer cron task** → **Run workflow**.
  В форке GitHub может сначала попросить включить Actions — подтвердите.
- Автоматически: ежедневно в **01:30 UTC** (04:30 МСК).
  Время меняется в `.github/workflows/github_action.yml`, строка `cron`.

## 5. Проверьте результат

Откройте лог шага **Run application**. Успешный запуск заканчивается строкой
`#==#==# FINISHED #==#==#`. Строки с `!!!` — ошибки или предупреждения;
их разбор — в [Диагностике](troubleshooting.md).
