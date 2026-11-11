# Локальный запуск

## Требования

- **JDK 25** (например, Temurin 25). Более старые версии не соберут проект:
  используются возможности Java 25 и preview-API (`StructuredTaskScope`).
- **Maven 3.9+**.

## Сборка

```bash
mvn -B clean package
```

Сборка запускает и тесты. Результат — `target/dns-block-and-redirect-configurer-1.0-SNAPSHOT.jar`
со всеми зависимостями внутри (maven-shade-plugin).

## Запуск

Linux / macOS:

```bash
DNS=NextDNS CLIENT_ID=abc123 AUTH_SECRET=your-key REDIRECT=https://example.com/hosts java --enable-preview -jar target/dns-block-and-redirect-configurer-1.0-SNAPSHOT.jar
```

Windows PowerShell:

```powershell
$env:DNS="Cloudflare"; $env:CLIENT_ID="account-id"; $env:AUTH_SECRET="token"; $env:BLOCK="https://example.com/hosts"
java --enable-preview -jar target/dns-block-and-redirect-configurer-1.0-SNAPSHOT.jar
```

Флаг `--enable-preview` обязателен, без него JVM откажется запускать jar.

## Цвета в логе

Скрипт выводит ANSI-цвета. В старой консоли Windows (`cmd.exe`) вместо цветов
могут появиться символы вида `[1;93m` — используйте Windows Terminal или
PowerShell 7.

## Безопасность

Не сохраняйте `AUTH_SECRET` в файлах внутри репозитория и в истории командной
строки общих машин. Скрипт меняет настройки реального аккаунта — для
экспериментов заведите отдельный профиль NextDNS или отдельный аккаунт Cloudflare.
