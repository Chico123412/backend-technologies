# Лабораторна №9 — Interservice communication

Сервіс звітів (A) запитує дані каталогу (B) для трьох дій: оцінки вартості, планування часу та отримання картки послуги. Спосіб зв’язку перемикається між HTTP і gRPC без зміни контролера.

### Що реалізовано

| Файли | Призначення |
| --- | --- |
| `catalog-contracts/src/main/proto/catalog.proto` | Спільний контракт: повідомлення ServiceId, ServiceData і rpc GetService. Java-класи генеруються в target під час збірки. |
| `catalog-service/.../controller/InternalCatalogController.java` | REST-читання послуги: `GET /internal/catalog/{id}`. |
| `catalog-service/.../grpc/` | gRPC API та запуск/зупинка сервера разом із застосунком. Відсутня послуга дає NOT_FOUND. |
| `reporting-service/.../catalog/` | Три порти: PriceLookup, DurationLookup, ServiceDetailsLookup; HTTP і gRPC адаптери, власні винятки. |
| `reporting-service/.../config/CatalogLookupConfig.java` | Декларативний HTTP-клієнт Spring через GetExchange, gRPC channel і вибір адаптера. |
| `reporting-service/.../controller/ServicePlanningController.java` | Три бізнес-дії; працює лише через порти. |
| `reporting-service/.../controller/CatalogLookupErrors.java` | Перетворення власних винятків у HTTP 404 та 503. |

Java-шляхи в таблиці починаються з `src/main/java/com/autoservice/`.

### Налаштування

`catalog.lookup.transport` має значення `http` або `grpc`. Невідоме значення зупиняє запуск. gRPC-сервер каталогу за замовчуванням вимкнений.

| Змінна середовища | Типове значення |
| --- | --- |
| `CATALOG_TRANSPORT` | http |
| `CATALOG_HTTP_URL` | http://localhost:8080 |
| `CATALOG_GRPC_TARGET` | localhost:9090 |
| `CATALOG_GRPC_ENABLED` | false |
| `CATALOG_GRPC_PORT` | 9090 |
| `CATALOG_CONNECT_TIMEOUT_MS` | 1000 |
| `CATALOG_READ_TIMEOUT_MS` | 1500 |
| `CATALOG_DEADLINE_MS` | 1500 |

У Compose REST-адреса — `http://catalog:8080`, gRPC-адреса — `catalog:9090`. Брокерні події восьмої лабораторної залишилися окремим процесом і не замінені HTTP-викликами.

Помилки обох адаптерів зведено до `CatalogItemNotFound` і `CatalogUnavailable`. HTTP 404 та gRPC NOT_FOUND дають 404 у сервісі A. Тайм-аут, deadline, відмова в з’єднанні та інші помилки дають 503. Для локального gRPC використовується plaintext. Внутрішній REST-ендпоінт також дозволяє читання без JWT; правила доступу до звичайного API каталогу збережені.

### Збірка й запуск

```bash
./mvnw clean install -Pintegration-tests
docker compose up -d --wait
```

Install встановлює parent і модуль контрактів у локальний Maven-репозиторій, щоб сервіси можна було запускати окремо. Після готовності Keycloak:

```bash
./mvnw -pl catalog-service spring-boot:run
```

```bash
./mvnw -pl reporting-service spring-boot:run
```

Для режиму gRPC сервіси запускаються з іншими налаштуваннями:

```bash
CATALOG_GRPC_ENABLED=true ./mvnw -pl catalog-service spring-boot:run
```

```bash
CATALOG_TRANSPORT=grpc ./mvnw -pl reporting-service spring-boot:run
```

### Перевірка

```bash
./mvnw verify -Pintegration-tests
```

Тести HTTP-клієнта використовують локальний HTTP-сервер, а gRPC — справжній сервер на випадковому порту. Перевіряються три дії, 404/503, перевищення read timeout/deadline, зупинений сервер, невідомий адаптер і життєвий цикл gRPC. Docker для цих тестів не потрібен. Дві перевірки RabbitMQ з попередньої лабораторної залишаються окремо вимкненими без `RUN_RABBITMQ_TESTS=true`.

Запити — `requests/lab9.http`. Нові ендпоінти сервісу звітів працюють з однаковими URL в обох режимах:

- `GET /api/estimates/{id}?quantity=2` — загальна вартість.
- `GET /api/planning/{id}` — тривалість.
- `GET /api/cards/{id}` — картка послуги.

Для перевірки 503 достатньо зупинити каталог. Для 404 використовується ID, якого немає в БД.
