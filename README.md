# Автосервіс

Навчальний проєкт з дисципліни «Технології Back-end».
Каталог послуг: назва, опис, ціна та тривалість. Адміністратор може додавати,
редагувати й видаляти послуги, клієнт — переглядати каталог.

## Лабораторна №3. Анотації

Гілка: `lab-3-annotations`.
Мета — реалізувати три власні анотації з різними механізмами роботи у Spring Boot.

### 1. Валідація даних послуги

`@ValidServiceDetails` стоїть на DTO `ServiceRequest`.
У проєкті прийнято правило: для послуги тривалістю понад 60 хвилин потрібно
заповнити опис. Для коротших послуг опис залишається необов’язковим.
Правило пов’язує два поля DTO, тому перевіряється на рівні класу.

Файли в `src/main/java/com/autoservice/`:

- `validation/ValidServiceDetails.java` — constraint-анотація з `@Constraint`.
- `validation/ServiceDetailsValidator.java` — перевірка тривалості та опису.
- `dto/ServiceRequest.java` — застосування анотації.

Bean Validation знаходить валідатор через `validatedBy`, вручну в контролерах
він не створюється. Наявний `@Valid` запускає перевірку для API та форми.
Порушення прив’язане до поля `description`, тому повідомлення видно біля
поля опису. API повертає 400. Відсутню тривалість окремо перевіряє `@NotNull`.

### 2. Композитна анотація

`annotation/ApiController.java` об’єднує `@RestController` і `@RequestMapping`.
Шлях передається через `@AliasFor`. Анотація використовується в
`ServiceController`, `AdminController` та `ProfileController`.

Приклад: `@ApiController("/api/services")` замінює дві окремі анотації.
Маршрути каталогу, JSON-відповіді та HTTP-статуси залишилися тими самими.

### 3. Параметр поточного користувача

`@CurrentUsername` дозволяє отримати ім’я користувача в параметрі методу контролера.

- `annotation/CurrentUsername.java` — анотація параметра.
- `web/CurrentUsernameResolver.java` — `HandlerMethodArgumentResolver`.
- `config/WebConfig.java` — реєстрація резолвера через `WebMvcConfigurer`.
- `controller/ProfileController.java` — `GET /api/profile`.

Резолвер підтримує лише `String` з `@CurrentUsername`. Він бере
`preferred_username` з JWT або OAuth2-користувача. Якщо атрибут відсутній чи
порожній, використовується `Authentication.getName()`.
Без автентифікації повертається 401. Контролер використовує отримане ім’я
у JSON-відповіді, наприклад `{"username":"client"}`.
API, як і раніше, потребує Bearer JWT; сесія входу в UI його не замінює.

### Перевірки

```bash
./mvnw clean verify
./mvnw -Pintegration-tests verify
```

Перша команда запускає модульні тести й перевірку покриття JaCoCo.
Друга додатково виконує інтеграційні тести з Spring, H2 та MockMvc.
Docker для цих тестів не потрібен, зовнішній Keycloak у тестах підмінений.

Нові тести в `src/test/java/com/autoservice/`:

| Файл | Що перевіряється |
| --- | --- |
| `validation/ServiceDetailsValidatorTest.java` | Автоматичний виклик валідатора, допустимі дані, межа 60/61 хв, порожній опис і текст помилки (AC1–3) |
| `annotation/ApiControllerTest.java` | Маршрути, JSON, статус і порівняння з окремими стандартними анотаціями (AC4–5) |
| `web/CurrentUsernameResolverTest.java` | supportsParameter, JWT/OAuth2, резервне ім’я та відмова без входу (AC6–7) |
| `controller/ProfileControllerTest.java` | Виклик через MVC з зареєстрованим резолвером і використання імені у відповіді (AC8) |
| `CatalogIntegrationTest.java` | Додані запити до профілю через Bearer JWT, перевірка нового правила через API і форму (AC3, AC8) |

Результат перевірки: 70 модульних і 17 інтеграційних тестів пройшли без помилок.
Покриття рядків модульними тестами — 83,21%, `CatalogService` — 100%.
Звіт JaCoCo: `target/site/jacoco/index.html`.
Запити для ручної перевірки: `requests/lab3.http`, виконуються зверху вниз
у HTTP Client IntelliJ за наявності цієї можливості в IDE. Postman не потрібен.

## Запуск застосунку

Java 17+ (локально можна JDK 25), Maven Wrapper, Docker Desktop.
Spring Boot, PostgreSQL, Liquibase, MapStruct, Thymeleaf і Keycloak залишені
з попередніх лабораторних. Нові залежності у третій лабораторній не додавалися.

1. Запустити Docker Desktop.
2. У корені проєкту виконати `docker compose up -d --wait`.
3. Дочекатися відкриття `http://localhost:8081/realms/autoservice/.well-known/openid-configuration`.
4. Виконати `./mvnw spring-boot:run`.
5. Відкрити `http://localhost:8080/services`.

Демонстраційні користувачі: `manager` / `manager123` (ADMIN),
`client` / `client123` (USER). Консоль Keycloak — `http://localhost:8081`,
`admin` / `admin`. Це дані лише для локального запуску.
PostgreSQL: `localhost:55432`, база і користувач `autoservice`, пароль `autoservice_dev`.
Схему створює Liquibase; Hibernate перевіряє її через `ddl-auto: validate`.

## Попередні лабораторні

- №1: CRUD каталогу (`controller`, `service`, `repository`, `entity`, `dto`, `mapper`),
  міграції (`src/main/resources/db/changelog`), сторінки (`templates`),
  авторизація (`config/SecurityConfig.java`) та Keycloak (`docker-compose.yml`, `keycloak`).
  Готові запити — `requests/autoservice.http`.
- №2: ізольовані тести сервісу, контролерів, мапера й ролей у `src/test/java`,
  налаштування JaCoCo в `pom.xml`. Детальний звіт — [docs/LAB2.md](docs/LAB2.md).

Кожна лабораторна має окрему гілку, злиття між ними не виконуються.
