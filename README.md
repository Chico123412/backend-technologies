# Лабораторна №8 — Message brokers

Каталог надсилає події про створення, редагування та видалення послуг через RabbitMQ. Сервіс звітів отримує їх і зберігає журнал. Прямих HTTP-викликів між сервісами в цьому процесі немає.

### Реалізація

| Файли | Що зроблено |
| --- | --- |
| `compose.yml` | RabbitMQ, постійний volume і порти 5672 та 15672. У контейнерах сервісів `RABBITMQ_HOST=rabbitmq`. |
| `catalog-service/.../service/CatalogService.java` | Після успішної зміни послуги записується одна подія. Запис послуги й події входить в одну транзакцію. |
| `catalog-service/.../messaging/` | `CatalogEventOutbox` записує подію в БД, `CatalogEventRelay` надсилає зафіксовані події в RabbitMQ раз на 2 секунди. |
| `reporting-service/.../events/` | Listener отримує подію, а `CatalogEventJournal` додає її в журнал. |
| `reporting-service/.../controller/CatalogJournalController.java` | `GET /api/catalog-events` повертає останні 100 подій. |
| `config/RabbitConfig.java` в обох модулях | Durable exchange `catalog.events`, черга `reporting.catalog-events`, JSON-конвертер і routing key `service.changed`. |

Шляхи до Java-файлів починаються з `src/main/java/com/autoservice/`. Для кожного сервісу є свій запис `ServiceEvent`; формат JSON однаковий, але залежності між модулями немає.

Якщо транзакція відкочується, запис події теж відкочується. Після commit фонове завдання може прочитати подію й надіслати її. Якщо RabbitMQ недоступний, подія залишається в БД для повторного надсилання. Після підтвердження брокера вона позначається надісланою.

Повідомлення persistent, черга durable, дані RabbitMQ зберігаються у volume. Якщо сервіс звітів зупинений, каталог продовжує працювати, а події очікують у черзі. Після запуску споживача вони потрапляють у журнал. Унікальний `event_id` не дозволяє повторній доставці додати той самий запис удруге. Підтвердження споживача відбувається після повернення listener і commit журналу.

### Запуск

PostgreSQL, Keycloak і RabbitMQ:

```bash
docker compose up -d --wait
```

Після готовності Keycloak обидва сервіси запускаються в окремих терміналах:

```bash
./mvnw -pl catalog-service spring-boot:run
```

```bash
./mvnw -pl reporting-service spring-boot:run
```

Стандартна адреса брокера — `localhost:5672`. Налаштування беруться з `RABBITMQ_HOST`, `RABBITMQ_PORT`, `RABBITMQ_USER` і `RABBITMQ_PASSWORD`. У Compose логін `autoservice`, пароль `autoservice_dev`. Інтерфейс RabbitMQ — [localhost:15672](http://localhost:15672), журнал — [localhost:8082/api/catalog-events](http://localhost:8082/api/catalog-events).

### Перевірка

```bash
./mvnw clean verify -Pintegration-tests
```

Цей набір не потребує Docker. Модульний тест `CatalogEventJournalTest` перевіряє повторну доставку, а інтеграційний тест — відсутність дубля в справжній H2-таблиці. Тести каталогу перевіряють три події CRUD і rollback. `CatalogEventRelayTest` перевіряє persistent-повідомлення, підтвердження та відмову брокера.

Окремі тести з живим RabbitMQ запускаються з кореня: каталог залишає дві події, а наступний модуль звітів обробляє їх своїм listener. Перед запуском звичайні екземпляри сервісів мають бути зупинені, оскільки тести очищають навчальну чергу.

```bash
RUN_RABBITMQ_TESTS=true ./mvnw verify -Pintegration-tests
```

Вони перевіряють доставку після commit, відсутність повідомлення після rollback, накопичення двох подій без споживача та обробку черги зі справжнім listener.

Ручні запити — `requests/lab8.http`. Створення й редагування доступні manager/manager123. Для перевірки зупиненого споживача достатньо зупинити reporting-service, двічі створити послугу й знову запустити reporting-service. Записи з’являться в журналі після доставки. Кожне нове натискання POST створює окрему бізнес-дію; повторна доставка означає той самий `eventId`.

Для збірки Docker-образів залишені окремі Dockerfile. Профіль `apps` в Compose задає брокер за ім’ям `rabbitmq`; основний варіант локальної перевірки вище використовує сервіси з IDE/Maven.
