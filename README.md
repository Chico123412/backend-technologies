# Автосервіс

Семестровий проєкт з дисципліни «Технології Back-end».
Предметна область — керування автосервісом. Основа проєкту — каталог послуг;
подальший розвиток передбачає клієнтів, автомобілі та записи на обслуговування.

## Технології

- Java 17 або новіша версія JDK
- Spring Boot 3.5.16
- Spring Data JPA / Hibernate
- PostgreSQL 16
- Liquibase
- Maven Wrapper

## Запуск

Потрібні JDK та запущений Docker Desktop.

```bash
docker compose up -d --wait
chmod +x mvnw
./mvnw spring-boot:run
```

Адреса застосунку: http://localhost:8080.

PostgreSQL доступний на `localhost:55432`. База та користувач — `autoservice`,
пароль для локального запуску — `autoservice_dev`.
За потреби параметри підключення змінюються через змінні середовища
`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`.

Схему створює Liquibase під час запуску застосунку.
Hibernate перевіряє її відповідність сутностям через `ddl-auto: validate`.
Таблиця `service_items` зберігає назву, опис, ціну та тривалість послуги у хвилинах.
Дані PostgreSQL зберігаються в Docker volume `postgres-data`.

## Перевірка міграції

```bash
docker compose exec postgres psql -U autoservice -d autoservice -c '\dt'
```

У базі мають бути `service_items`, `databasechangelog` та `databasechangeloglock`.

## Збірка

```bash
./mvnw clean package
```
