# Добавление DNS-провайдера

Пример ниже — гипотетический провайдер `Example`. Общий код (`common`)
трогать не нужно, кроме выбора пакета в `App`.

## 1. Пакет

```
com.novibe.dns.example
├── ExampleTaskRunner.java
├── service/
└── http/
    ├── ExampleHttpClient.java
    └── dto/
        ├── request/
        └── response/
```

Всё, что должно попасть в Spring-контекст, помечается `@Service` или
`@Component` и лежит внутри этого пакета.

## 2. HTTP-клиент

Унаследуйтесь от `HttpRequestSender`:

```java
@Service
public class ExampleHttpClient extends HttpRequestSender {

    @Override
    protected String apiUrl() {
        return "https://api.example.com/v1/accounts/%s".formatted(CLIENT_ID);
    }

    @Override
    protected String authHeaderName() { return "Authorization"; }

    @Override
    protected String authHeaderValue() { return "Bearer " + AUTH_SECRET; }

    @Override
    protected void react401() { Log.fail("Invalid API key!"); }

    @Override
    protected void react403() { Log.fail("API key has no access to this account!"); }
}
```

`get` / `post` / `delete` уже реализованы. Тело запроса должно реализовывать
`Jsonable`, тип ответа — класс для Gson.

Если у API жёсткий лимит частоты, посмотрите на
`NextDnsRateLimitedApiProcessor` как на образец очереди с повтором.

## 3. Сценарий

```java
@Service
@RequiredArgsConstructor
public class ExampleTaskRunner implements DnsTaskRunner {

    private final HostsBlockListsLoader blockListsLoader;
    private final HostsOverrideListsLoader overrideListsLoader;

    @Override
    public void run() {
        Log.global("EXAMPLE");
        List<String> blocks = blockListsLoader.fetchWebsites(EnvParser.parse(BLOCK));
        List<HostsOverrideListsLoader.BypassRoute> overrides =
                overrideListsLoader.fetchWebsites(EnvParser.parse(REDIRECT));
        // синхронизация с API провайдера
        Log.global("FINISHED");
    }
}
```

В пакете провайдера должна быть **ровно одна** реализация `DnsTaskRunner`.

## 4. Регистрация в `App`

```java
case "EXAMPLE" -> "com.novibe.dns.example";
```

Обновите и текст ошибки в ветке `default`.

## 5. Решите и задокументируйте стратегию синхронизации

Это главное решение. Ответьте явно:

- Как отличить свои записи от ручных? (Cloudflare — префикс имени + sessionId;
  NextDNS — никак, поэтому скрипт не удаляет записи, кроме полной очистки.)
- Пересоздавать всё или считать разницу?
- Что делать при пустых `BLOCK` / `REDIRECT`?
- Как вести себя при частичной ошибке?

## 6. Документация

- Создайте `docs/user/<provider>.md` по образцу [cloudflare.md](../user/cloudflare.md):
  получение ключей, поведение, ошибки авторизации.
- Добавьте строку в таблицы [configuration.md](../user/configuration.md),
  [quick-start.md](../user/quick-start.md), [api-reference.md](api-reference.md)
  и ссылку в [docs/README.md](../README.md).
- Обновите оба README в корне.
