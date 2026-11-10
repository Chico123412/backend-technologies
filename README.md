# Лабораторна №6 — Quartz Scheduler

У проєкті автосервісу Quartz регулярно зберігає кількість послуг у каталозі й очищає старі знімки. Розклад зберігається в PostgreSQL і переживає перезапуск застосунку.

### Розклад

Усі Job і Trigger оголошені в `CatalogQuartzConfig.java`, група — `catalog`. Часовий пояс — `Europe/Kyiv`.

| Job | Trigger | Коли запускається |
| --- | --- | --- |
| `catalog-snapshot` | `daily-snapshot` | Щодня о 02:00 |
| `catalog-snapshot` | `weekly-snapshot` | Щопонеділка о 03:00 |
| `snapshot-cleanup` | `daily-cleanup` | Щодня о 04:00 |

Для знімка типовий `reportType=daily`. Тижневий тригер перевизначає його на `weekly`, а `zoneId` бере з Job. Для очищення `retentionDays=30`. Якщо обов’язковий параметр відсутній, Job завершується з помилкою.

### Як працює

- `CatalogSnapshotJob` і `SnapshotCleanupJob` читають об’єднані параметри та викликають метод `CatalogSnapshotService`. Транзакції знаходяться в сервісі.
- Знімок зберігається один раз для пари «тип звіту + дата». Повторне очищення з тією самою датою запуску та строком зберігання нічого не видаляє, якщо перший запуск уже очистив усі старі записи.
- `@DisallowConcurrentExecution` не дає двом запускам одного JobKey виконуватися одночасно. Пропущені за розкладом запуски Quartz не надолужує (`DO_NOTHING`). У разі збою Quartz записує помилку в лог, а завдання чекає наступного запуску за розкладом.
- Liquibase створює таблиці `QRTZ_*` через `002-quartz.yaml`, а `catalog_snapshots` — через `003-catalog-snapshots.yaml`. SQL-схеми взяті з Quartz 2.5.2 ([Apache 2.0](docs/quartz-license.txt)); видалено команди скидання таблиць і `COMMIT`, щоб схемою керував Liquibase.
- У `application.yml` увімкнено JDBC JobStore, `initialize-schema: never` та `overwrite-existing-jobs: false`, тому застосунок під час запуску не пересоздає таблиці й не перезаписує наявні тригери.

### Як перевірити

```bash
./mvnw clean verify -Pintegration-tests
```

Тести перевіряють параметри, часовий пояс, misfire, обробку помилки, повторний запуск і збереження тригера після перезапуску планувальника. Інтеграційні тести використовують H2.

Для запуску з PostgreSQL виконай `docker compose up -d --wait`, дочекайся Keycloak і запусти `./mvnw spring-boot:run`. Після старту застосунку можна перевірити розклад і знімки:

```bash
docker compose exec postgres psql -U autoservice -d autoservice \
  -c 'SELECT trigger_name, job_name, misfire_instr FROM qrtz_triggers ORDER BY trigger_name;' \
  -c 'SELECT * FROM catalog_snapshots ORDER BY report_date DESC;'
```

До першого виконання завдань таблиця знімків буде порожньою.
