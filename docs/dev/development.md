# Разработка

## Окружение

| Инструмент | Версия | Примечание |
|---|---|---|
| JDK | 25 | `maven-compiler-plugin`: `source`/`target` 25, `--enable-preview` |
| Maven | 3.9+ | Maven Wrapper в репозитории нет |
| IDE | любая с поддержкой Lombok | В IntelliJ IDEA включите *Annotation Processing* и уровень языка *25 (Preview)* |

## Зависимости (`pom.xml`)

| Артефакт | Версия | Назначение |
|---|---|---|
| `org.springframework:spring-context` | 7.0.2 | DI-контейнер |
| `org.projectlombok:lombok` | 1.18.42 | Геттеры, билдеры, `@SneakyThrows`, `@Cleanup` (scope `provided`) |
| `com.google.code.gson:gson` | 2.10.1 | JSON |
| `org.junit.jupiter:junit-jupiter` | 5.11.4 | Тесты (scope `test`) |
| `maven-surefire-plugin` | 3.5.2 | Запуск тестов с `--enable-preview` |
| `maven-shade-plugin` | 3.6.0 | Fat jar с `Main-Class: com.novibe.App` |

Lombok подключён и как зависимость, и в `annotationProcessorPaths` — второе
обязательно, иначе сборка падает (см. коммит `fc7498b`).

## Сборка, тесты и запуск

```bash
mvn -B clean package
```

Команда собирает jar и запускает тесты. Только тесты:

```bash
mvn -B test
```

Тесты лежат в `src/test/java` в тех же пакетах, что и проверяемый код, — так
им доступны методы с пакетной видимостью (`ListLoader.parse`,
`ListService.cutChunks`). Не вызывайте в тестах `toJson()` и всё, что
обращается к `App.context`: Spring-контекст в тестах не поднимается.

```bash
java --enable-preview -jar target/dns-block-and-redirect-configurer-1.0-SNAPSHOT.jar
```

Переменные окружения — см. [Локальный запуск](../user/local-run.md).

## Используемые возможности Java 25

| Возможность | Где | Статус в Java 25 |
|---|---|---|
| Метод `static void main()` без аргументов | `App` | стабильная (JEP 512) |
| `java.lang.IO.println` | `Log` | стабильная (JEP 512) |
| `StructuredTaskScope.open()` | `ListLoader`, `RuleService` | **preview** (JEP 505) |
| Виртуальные потоки | `ListService` | стабильная |

Из-за preview-API скомпилированные классы привязаны к Java 25: на Java 26+
jar придётся пересобрать, а API `StructuredTaskScope` может измениться.

## Соглашения в коде

- Внедрение зависимостей — через конструктор (`@RequiredArgsConstructor` +
  `final`-поля). В абстрактных базовых классах — через сеттеры
  (`@Setter(onMethod_ = @Autowired)`).
- Логирование — только через `com.novibe.common.util.Log`, не через
  `System.out` и не через `java.util.logging`.
- Тела запросов — `record` или класс, реализующий `Jsonable`. Ответы — классы
  с Lombok `@Data`/`@Getter`, наследники `CloudflareApiResponse<T>` /
  `NextDnsResponse<T>`. Имена полей в snake_case — через `@SerializedName`.
- Структура провайдера: `<Provider>TaskRunner` → `service/` → `http/` → `http/dto/`.
- Сообщения в логах — на английском.

## CI

`.github/workflows/github_action.yml` — не проверка PR, а **рабочий запуск**:
по cron `30 1 * * *` и вручную (`workflow_dispatch`) собирает проект и
применяет настройки к аккаунту из окружения `DNS`. Тесты выполняются как часть
`mvn package`, поэтому упавший тест остановит ежедневный запуск до изменения
настроек. Отдельной проверки PR нет — перед пушем выполните `mvn -B clean package`.

## Облачные сессии Claude Code

Окружение `DnsConf` в Claude Code ставит JDK 25 скриптом настройки и прописывает
его в `/etc/mavenrc`, поэтому `mvn` в новой сессии сразу использует Java 25.

## Ручная проверка изменений

Сценарии с API провайдеров тестами не покрыты, поэтому их изменения
проверяются запуском на тестовом аккаунте:

1. Отдельный профиль NextDNS или отдельный аккаунт Cloudflare Zero Trust.
2. Маленький собственный hosts-файл (например, в gist), где есть строки
   и блокировок, и перенаправлений.
3. Проверить три сценария: оба источника, только один, оба пустые.
4. Для Cloudflare — запустить дважды подряд и убедиться, что после второго
   запуска не осталось объектов от первого.
