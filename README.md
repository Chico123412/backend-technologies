# Лабораторна №6 — Quartz Scheduler

У проєкті автосервісу Quartz регулярно зберігає кількість послуг у каталозі й очищає старі знімки. Розклад зберігається в PostgreSQL між перезапусками застосунку.

### Розклад

Усі Job і Trigger оголошені в `CatalogQuartzConfig.java`, група — `catalog`. Часовий пояс — `Europe/Kyiv`.

| Job | Trigger | Коли запускається |
| --- | --- | --- |
| `catalog-snapshot` | `daily-snapshot` | Щодня о 02:00 |
| `catalog-snapshot` | `weekly-snapshot` | Щопонеділка о 03:00 |
| `snapshot-cleanup` | `daily-cleanup` | Щодня о 04:00 |

Для знімка типовий `reportType=daily`. Тижневий тригер перевизначає його на `weekly`, а `zoneId` бере з Job. Для очищення `retentionDays=30`. Якщо обов’язковий параметр відсутній, Job завершується з помилкою.

### Як працює

- `CatalogSnapshotJob` і `SnapshotCleanupJob` читають об’єднані параметри та викликають метод `CatalogSnapshotService`. Транзакції відкриваються на рівні сервісу.
- Знімок зберігається один раз для пари «тип звіту + дата». Повторне очищення з тією самою датою запуску та строком зберігання нічого не видаляє, якщо перший запуск уже очистив усі старі записи.
- `@DisallowConcurrentExecution` не дає двом запускам одного JobKey виконуватися одночасно. Пропущені за розкладом запуски Quartz не надолужує (`DO_NOTHING`). У разі збою Quartz записує помилку в лог, а завдання чекає наступного запуску за розкладом.
- Liquibase створює таблиці `QRTZ_*` через `002-quartz.yaml`, а `catalog_snapshots` — через `003-catalog-snapshots.yaml`. SQL-схеми взяті з Quartz 2.5.2 ([Apache 2.0](docs/quartz-license.txt)); видалено команди скидання таблиць і `COMMIT`, щоб схемою керував Liquibase.
- У `application.yml` увімкнено JDBC JobStore, `initialize-schema: never` та `overwrite-existing-jobs: false`, тому під час запуску таблиці не створюються повторно, а наявні тригери зберігаються.

### Тести

```bash
./mvnw clean verify -Pintegration-tests
```

Інтеграційні тести працюють на H2. Серед перевірок — збереження тригера після перезапуску, обробка пропущених запусків (misfire) і відсутність дублів при повторному виконанні.

### Локальний запуск

PostgreSQL і Keycloak запускаються через Docker Compose:

```bash
docker compose up -d --wait
```

Після готовності Keycloak застосунок запускається командою:

```bash
./mvnw spring-boot:run
```

Розклад зберігається в `qrtz_triggers`, результати — в `catalog_snapshots`. До першого виконання завдання зі збереження знімка таблиця результатів порожня.

```bash
docker compose exec postgres psql -U autoservice -d autoservice \
  -c 'SELECT trigger_name, job_name, misfire_instr FROM qrtz_triggers ORDER BY trigger_name;' \
  -c 'SELECT * FROM catalog_snapshots ORDER BY report_date DESC;'
```
