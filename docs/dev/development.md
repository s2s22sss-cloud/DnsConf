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
| `maven-shade-plugin` | 3.6.0 | Fat jar с `Main-Class: com.novibe.App` |

Lombok подключён и как зависимость, и в `annotationProcessorPaths` — второе
обязательно, иначе сборка падает (см. коммит `fc7498b`).

## Сборка и запуск

```bash
mvn -B clean package
```

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
применяет настройки к аккаунту из окружения `DNS`. Отдельного CI для сборки
и проверки изменений нет. Перед пушем соберите проект локально.

## Ручная проверка изменений

Тестов нет, поэтому изменения проверяются запуском на тестовом аккаунте:

1. Отдельный профиль NextDNS или отдельный аккаунт Cloudflare Zero Trust.
2. Маленький собственный hosts-файл (например, в gist), где есть строки
   и блокировок, и перенаправлений.
3. Проверить три сценария: оба источника, только один, оба пустые.
4. Для Cloudflare — запустить дважды подряд и убедиться, что после второго
   запуска не осталось объектов от первого.
