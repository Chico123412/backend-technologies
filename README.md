# Лабораторна №7 — Multi-module Maven

Автосервіс складається з двох Spring Boot сервісів. Кореневий `pom.xml` має тип `pom`, перелічує модулі та містить спільні залежності, зокрема `spring-boot-starter-test`. Кожен модуль успадковує цей POM і оголошує свій Spring Boot plugin.

| Модуль | Головний клас | Порт | Призначення |
| --- | --- | --- | --- |
| `catalog-service` | `AutoserviceApplication` | 8080 | Каталог, вебінтерфейс і REST API з Keycloak |
| `reporting-service` | `ReportingApplication` | 8082 | Знімки каталогу та очищення старих звітів за розкладом Quartz |

У кожному модулі є власний `application.yml`, тести й Dockerfile. Сервіси збираються в окремі JAR і не залежать один від одного через Maven. PostgreSQL спільна: каталог керує таблицею `service_items`, а сервіс звітів читає з неї кількість послуг і керує `catalog_snapshots` та таблицями Quartz. Планувальник зберігає розклад у базі. Для створення знімків потрібна ініціалізована таблиця каталогу; запущений процес каталогу для цього не потрібен.

### Збірка й тести

```bash
./mvnw clean verify
./mvnw verify -Pintegration-tests
```

Другий запуск додає інтеграційні тести каталогу. Тести звітів із H2 запускаються в обох випадках.

```bash
./mvnw -pl catalog-service dependency:tree
./mvnw -pl reporting-service dependency:tree
```

### Локальний запуск

```bash
docker compose up -d --wait
```

Після готовності Keycloak каталог і звіти запускаються в окремих терміналах. Перший запуск каталогу створює таблицю `service_items`.

```bash
./mvnw -pl catalog-service spring-boot:run
```

```bash
./mvnw -pl reporting-service spring-boot:run
```

Каталог: [localhost:8080](http://localhost:8080). Стан планувальника: [localhost:8082/status](http://localhost:8082/status).

### Docker-образи

Dockerfile кожного модуля використовує JAR, створений під час збірки Maven.

```bash
docker build -t autoservice-catalog ./catalog-service
docker build -t autoservice-reporting ./reporting-service
```
