# Лабораторна №5 — Асинхронна обробка

У проєкті автосервісу додав фоновий експорт каталогу й зведення для адміністратора.

- `POST /api/admin/catalog/export` повертає `202` та `exportId`, не чекаючи запису файлу. Результат з’являється в `target/catalog-exports/<exportId>.json`.
- `GET /api/admin/catalog/summary` одночасно запускає два незалежні запити: максимальна ціна й найбільша тривалість послуги. Сервіс об’єднує результати через `thenCombine` і повертає Future контролеру.

### Де що знаходиться

Шляхи нижче відносно `src/main/java/com/autoservice/`.

| Файл | Що робить |
| --- | --- |
| `config/AsyncConfig.java` | Вмикає `@Async`, налаштовує `catalogExecutor`: 2 основні й максимум 2 потоки, черга на 2 завдання, префікс `catalog-async-`. Обробник помилок `void` записує виняток у лог. |
| `service/CatalogReportTasks.java` | Три методи `@Async("catalogExecutor")`: експорт повертає `void`, запити повертають `CompletableFuture`. |
| `service/CatalogReportData.java`, `repository/ServiceRepository.java` | Запис JSON і два окремі запити до БД. Затримка 750 мс імітує повільну операцію; `catalog.async-delay-ms=0` її вимикає. |
| `service/CatalogReportService.java`, `controller/CatalogReportController.java` | Запуск операцій, об’єднання результатів і відповіді API. |

Асинхронні методи викликаються з іншого біна, тому Spring запускає їх через проксі. На зведення відведено до 3 секунд (`catalog.summary-timeout-ms`). При збої чи тайм-ауті `exceptionally` повертає `200` з поясненням замість даних. Тайм-аут припиняє очікування відповіді; вже запущена операція може завершитися пізніше. Якщо зайняті всі потоки й місця в черзі, API повертає `503` з проханням спробувати пізніше.

### Як перевірити

```bash
./mvnw clean verify -Pintegration-tests
```

`CatalogAsyncIntegrationTest` перевіряє, що експорт повертає `202` до завершення запису, обидва запити починаються до завершення будь-якого з них, а помилки й переповнення черги обробляються. `CatalogReportServiceTest` перевіряє тайм-аут. Для тестів Docker не потрібен.

Для ручної перевірки запусти `docker compose up -d --wait`, дочекайся Keycloak на порту 8081, виконай `./mvnw spring-boot:run` і відкрий запити з `requests/lab5.http`. Якщо каталог порожній, максимальні значення будуть `null`.
