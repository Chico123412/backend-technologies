# Автосервіс

Семестровий проєкт з дисципліни «Технології Back-end».
Предметна область — каталог послуг автосервісу з можливістю подальшого додавання
клієнтів, автомобілів та записів на обслуговування.

## Стек

Java 17+ (можна використовувати JDK 25), Spring Boot 3.5.16, Spring Data JPA,
Hibernate, PostgreSQL 16, Liquibase, MapStruct 1.6.3, Thymeleaf, Spring Security,
Keycloak 26.8.1. Maven Wrapper завантажує Maven автоматично.

## Запуск

Відкрити Docker Desktop, потім у корені проєкту:

```bash
bash scripts/start.sh
```

Скрипт запускає PostgreSQL та Keycloak, очікує готовності провайдера входу й запускає застосунок.
UI: http://localhost:8080. Каталог: http://localhost:8080/services.

| Обліковий запис | Пароль | Ролі | Доступ |
| --- | --- | --- | --- |
| manager | manager123 | ADMIN, USER | Перегляд, створення, зміна, видалення |
| client | client123 | USER | Перегляд каталогу |

Це облікові дані для локальної демонстрації.
Адмінконсоль Keycloak: http://localhost:8081, обліковий запис `admin` / `admin`.
Налаштований realm — `autoservice`.
Для зміни користувача натиснути «Вийти» у каталозі: завершується також сесія Keycloak.

## API

| Метод | Шлях | Результат | Доступ |
| --- | --- | --- | --- |
| GET | /api/services | 200, список | JWT |
| GET | /api/services/{id} | 200 або 404 | JWT |
| POST | /api/services | 201 та Location | ADMIN |
| PUT | /api/services/{id} | 200 або 404 | ADMIN |
| DELETE | /api/services/{id} | 204 або 404 | ADMIN |
| GET | /api/admin/summary | 200, кількість послуг | ADMIN |

Валідація неправильного тіла запиту повертає 400.
Запити без дійсного JWT повертають 401. Недостатня роль — 403.
UI використовує OIDC authorization code та HTTP-сесію.
API має окремий stateless SecurityFilterChain: сесія UI не замінює Bearer JWT.
CSRF-перевірка збережена для UI; для stateless API вона вимкнена.

Роль для `/api/admin/**` перевіряє SecurityFilterChain.
Операції запису в `CatalogService` захищає `@PreAuthorize` незалежно від URL-правил.
Realm-ролі з JWT та OIDC ID token перетворюються на `ROLE_ADMIN` / `ROLE_USER`.
Spring Security Resource Server перевіряє підпис, строк дії й issuer JWT.

## Готові запити

В IntelliJ відкрити `requests/autoservice.http` і запускати запити зверху вниз.
Або імпортувати `requests/autoservice.postman_collection.json` у Postman та запустити колекцію.
Токени й ID створеної послуги записуються автоматично. Колекція перевіряє HTTP-статуси.
Для отримання демонстраційних JWT окремий клієнт `autoservice-api` має direct access grants.
UI-клієнт `autoservice-web` використовує authorization code flow.

## База даних

Підключення: `localhost:55432`, база та користувач `autoservice`, пароль `autoservice_dev`.
Параметри можна змінити через `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`.
Адресу Keycloak можна змінити через `KEYCLOAK_URL` (також потрібно оновити redirect URI клієнта).
Серверний часовий пояс застосунку — UTC.

Схему створює Liquibase; Hibernate використовує `ddl-auto: validate`.
Зміни структури додаються новими міграціями, застосовані файли не редагуються.
PostgreSQL та Keycloak мають окремі Docker volumes для збереження даних.
Realm імпортується під час першого запуску; наявний realm автоматично не перезаписується.

```bash
docker compose exec postgres psql -U autoservice -d autoservice -c '\dt'
docker compose exec postgres psql -U autoservice -d autoservice -c 'SELECT * FROM service_items;'
```

## Перевірки

```bash
./mvnw clean test
./mvnw package
```

Інтеграційні тести використовують H2 у PostgreSQL mode та справжню міграцію Liquibase.
Вони перевіряють CRUD із фактичним збереженням через repository, валідацію, 401/403,
обидва рівні авторизації, розділення API та UI-сесії, OIDC-редирект, UI-форми та CSRF.
JWT decoder та OAuth2 client registration підмінені в тестах: Docker для них не потрібний.
Реальний вхід і підпис JWT перевіряються запущеним Keycloak та HTTP/Postman-запитами.

## Структура

- `controller` — REST API та сторінки UI.
- `service` — операції з каталогом і перевірки прав на запис.
- `repository` — доступ до даних через Spring Data JPA.
- `entity` — відображення таблиці service_items.
- `dto`, `mapper` — дані запитів/відповідей та перетворення MapStruct.
- `config` — SecurityFilterChain та перетворення ролей Keycloak.
- `db/changelog` — міграції Liquibase.

Кожна лабораторна зберігається в окремій гілці. Для першої — `lab-1-spring-security`.
