# Используемые API

Только те вызовы, которые делает скрипт. Полная документация:
[Cloudflare Gateway API](https://developers.cloudflare.com/api/resources/zero_trust/subresources/gateway/),
[NextDNS API](https://nextdns.github.io/api/).

## Cloudflare Zero Trust Gateway

База: `https://api.cloudflare.com/client/v4/accounts/{CLIENT_ID}/gateway`
Авторизация: `Authorization: Bearer {AUTH_SECRET}`

| Метод и путь | Клиент | Назначение |
|---|---|---|
| `GET /lists` | `CloudflareListClient.getLists` | Все списки аккаунта (`result: null` трактуется как пустой список) |
| `POST /lists` | `CloudflareListClient.postList` | Создать список |
| `DELETE /lists/{id}` | `CloudflareListClient.deleteListById` | Удалить список |
| `GET /rules` | `CloudflareRuleClient.getRules` | Все правила |
| `POST /rules` | `CloudflareRuleClient.createBlockingRule` | Создать правило (и block, и override) |
| `DELETE /rules/{id}` | `CloudflareRuleClient.removeRuleById` | Удалить правило |

Ответ всегда обёрнут: `{ "success", "errors", "messages", "result", "result_info" }`
(`CloudflareApiResponse<T>`).

Пример тела создания списка (`CreateListRequest`):

```json
{
  "name": "Blocked websites by script 1",
  "type": "DOMAIN",
  "description": "<sessionId>",
  "items": [{ "value": "ads.example.com" }]
}
```

Пример правила перенаправления (`CreateRuleRequest`):

```json
{
  "name": "Rules set by script override to IP -> 1.2.3.4",
  "description": "<sessionId>",
  "action": "override",
  "filters": ["dns"],
  "traffic": "any(dns.domains[*] in $<listId1>) or any(dns.domains[*] in $<listId2>)",
  "precedence": 3,
  "rule_settings": { "override_ips": ["1.2.3.4"] },
  "enabled": true
}
```

У правила блокировки `action: "block"`, нет `rule_settings`, `precedence: 0`.

## NextDNS

База: `https://api.nextdns.io/profiles/{CLIENT_ID}`
Авторизация: `X-Api-Key: {AUTH_SECRET}`

| Метод и путь | Клиент | Назначение |
|---|---|---|
| `GET /denylist` | `NextDnsDenyClient.fetchDenylist` | Текущий denylist |
| `POST /denylist` | `NextDnsDenyClient.saveDeny` | Добавить домен: `{ "id": "<domain>", "active": true }` |
| `DELETE /denylist/{id}` | `NextDnsDenyClient.deleteDenyById` | Удалить домен (id = домен) |
| `GET /rewrites` | `NextDnsRewriteClient.fetchRewrites` | Текущие rewrites |
| `POST /rewrites` | `NextDnsRewriteClient.saveRewrite` | Добавить: `{ "name": "<domain>", "content": "<ip>" }` |
| `DELETE /rewrites/{id}` | `NextDnsRewriteClient.deleteRewriteById` | Удалить rewrite по id |

Ответ: `{ "data": ..., "errors": [{ "code": ... }] }` (`NextDnsResponse<T>`).

Коды, которые обрабатываются особо: `429` и `524` — повтор через 60 секунд;
`401`/`403` — завершение с сообщением о ключе.
