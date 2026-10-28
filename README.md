# Автосервіс

Навчальний проєкт з дисципліни «Технології Back-end».
Каталог послуг автосервісу з ролями ADMIN і USER.

## Лабораторна №4. AOP

Гілка: `lab-4-aop`.
Мета — винести наскрізну поведінку у три пари «власна анотація + аспект»
та показати різницю між зовнішнім викликом і self-invocation.

### Що реалізовано

Файли розміщені в `src/main/java/com/autoservice/`.

| Анотація | Аспект | Поведінка та місце використання |
| --- | --- | --- |
| `annotation/RoundMoney.java` | `aspect/RoundMoneyAspect.java` | Результат BigDecimal округлюється до 2 знаків, HALF_UP. Використано на `QuoteService.calculateTotal` |
| `annotation/LimitBatchSize.java` | `aspect/LimitBatchSizeAspect.java` | Перевіряється перший аргумент-список: від 1 до max елементів без null. Порушення дає 400 без виконання методу. Використано на тому самому розрахунку, max = 10 |
| `annotation/CatalogWritable.java` | `aspect/CatalogWritableAspect.java` | У режимі «лише читання» зміни каталогу дають 503 без виконання методу. Використано на `CatalogService.create`, `update`, `delete` |

Усі три анотації мають `@Target(METHOD)` та `@Retention(RUNTIME)`.
Кожен аспект — Spring-компонент з `@Aspect` і `@Around`; pointcut використовує
`@annotation(...)`. Додана залежність `spring-boot-starter-aop` у `pom.xml`.
Проксі у застосунку налаштовує Spring Boot, вручну advice у робочому коді не викликаються.

`service/QuoteService.java` розраховує загальну вартість вибраних послуг.
Для двох і більше позицій діє знижка 5%; повторний ID означає повторну позицію.
Округлення й обмеження розміру винесені в аспекти та не дублюються в розрахунку.
Обмеження виконується перед округленням завдяки `@Order(100)` і `@Order(200)`.
Аспект режиму каталогу має порядок 300, після стандартної перевірки `@PreAuthorize`.
Авторизація й транзакції попередніх лабораторних збережені.

### Навмисний self-invocation

`QuoteService.calculateTotalInternally` викликає **той самий** анотований метод
через `this.calculateTotal(serviceIds)`. Це свідома демонстрація умови лабораторної.
Внутрішній виклик не проходить через проксі, тому ні округлення, ні обмеження
розміру на `calculateTotal` не спрацьовують. Помилка чи попередження не виникає.
Самовиклик навмисно залишено; self-injection і AopContext не використовуються.

`controller/QuoteController.java` дає два способи порівняти поведінку:

| Запит | Шлях виклику | Приклад результату |
| --- | --- | --- |
| `POST /api/quotes` | Контролер → проксі → calculateTotal | `{"total":190.02}` |
| `POST /api/quotes/self-invocation` | Контролер → проксі → calculateTotalInternally → this.calculateTotal | `{"total":190.0190}` |

Приклад: дві позиції по 100,01 грн. Після знижки: `200.02 × 0.95 = 190.0190`.
У першому випадку аспект змінює результат, у другому — ні.
Клієнт JSON може відобразити другий результат як `190.019`, що є тим самим числом.
Тіло запиту — список ID, наприклад `[1, 1]`; ID має існувати в каталозі.
Обидва ендпоінти потребують Bearer JWT. Другий — навчальна демонстрація обходу аспектів,
для звичайного розрахунку призначений перший.

### Режим «лише читання»

За замовчуванням вимкнений: `autoservice.catalog.read-only=false`.
Для перевірки зупинити застосунок і запустити з параметром:

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments="--autoservice.catalog.read-only=true"
```

Адміністратор зможе читати каталог, але POST/PUT/DELETE послуг повертатимуть 503.
Читання й розрахунок вартості залишаються доступними.
Повернення до звичайного запуску вимикає цей режим.

### Тести й відповідність умові

```bash
./mvnw clean verify
./mvnw -Pintegration-tests verify
```

Модульні тести аспектів викликають advice без Spring-контексту.
`ProceedingJoinPoint` замоканий через Mockito: перевіряються результат,
винятки, виклик `proceed()` і відсутність цього виклику при відмові.

Файли в `src/test/java/com/autoservice/`:

- `aspect/RoundMoneyAspectTest.java` — дроби, середина округлення, цілі значення, null і виняток цільового методу.
- `aspect/LimitBatchSizeAspectTest.java` — допустимий список, межі 10/11, порожній список, null та відмова до proceed.
- `aspect/CatalogWritableAspectTest.java` — звичайний режим, блокування запису й передавання винятку.
- `service/QuoteServiceTest.java` — розрахунок, знижка та помилка відсутньої послуги.
- `controller/QuoteControllerTest.java` — два шляхи виклику з контролера.
- `aspect/CatalogAspectsIntegrationTest.java` — справжні Spring-проксі, зовнішній виклик проти self-invocation, блокування трьох операцій запису та доступність читання.
- `CatalogIntegrationTest.java` — додано HTTP-перевірку обох розрахунків через MVC і справжній security filter chain, а також відповіді 400.

Результат: 93 модульні та 24 інтеграційні тести пройшли без помилок.
Покриття рядків модульними тестами — 86,23%, CatalogService — 100%.
Перша команда запускає модульні тести й JaCoCo, друга додає інтеграційні тести.
Для тестів Docker не потрібен: інтеграційні перевірки використовують H2 або mock repository.
Зовнішній Keycloak у тестах підмінений. Звіт покриття: `target/site/jacoco/index.html`.

Запити для ручної перевірки — `requests/lab4.http`, запускати зверху вниз.
Окремо позначений запит для режиму «лише читання» виконується після перезапуску з параметром вище.

## Запуск застосунку

Java 17+ (можна JDK 25), Maven Wrapper, Docker Desktop.

1. Запустити Docker Desktop.
2. Виконати `docker compose up -d --wait` у корені проєкту.
3. Дочекатися доступності `http://localhost:8081/realms/autoservice/.well-known/openid-configuration`.
4. Виконати `./mvnw spring-boot:run`.
5. Відкрити `http://localhost:8080/services`.

Демонстраційні користувачі: `manager` / `manager123` (ADMIN), `client` / `client123` (USER).
Консоль Keycloak: `http://localhost:8081`, `admin` / `admin`.
PostgreSQL: `localhost:55432`, база та користувач `autoservice`, пароль `autoservice_dev`.
Це облікові дані для локального запуску. Схему створює Liquibase.

## Попередні лабораторні

- №1: CRUD, міграції, UI, OIDC/JWT і ролі — `controller`, `service`, `repository`, `entity`, `dto`, `mapper`, `config`, `src/main/resources`.
- №2: unit-тести та JaCoCo — `src/test/java`, `pom.xml`; [звіт](docs/LAB2.md).
- №3: `@ValidServiceDetails`, `@ApiController`, `@CurrentUsername` — `validation`, `annotation`, `web`, `config/WebConfig.java`; [звіт у гілці №3](https://github.com/Chico123412/backend-technologies/blob/lab-3-annotations/README.md).

Лабораторні зберігаються в окремих гілках без злиття. Попередні гілки не змінені.
