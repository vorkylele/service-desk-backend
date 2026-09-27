# service-desk-backend

Серверная часть портала внутренних заявок — учебный проект к выпускной квалификационной работе
«Автоматизация обработки внутренних заявок сотрудников в организации на примере ООО «ВБ ТЕХ»».

Связанные репозитории:

| Репозиторий | Назначение |
|---|---|
| **service-desk-backend** (этот) | REST API, прикладная логика, схема базы данных |
| [service-desk-frontend](https://github.com/vorkylele/service-desk-frontend) | портал самообслуживания (React) |
| [service-desk-autotest](https://github.com/vorkylele/service-desk-autotest) | автотесты API, базы данных и интерфейса, полный стенд в Docker |

## Что делает

- **Техническая поддержка** — приём инцидентов, автоматическая маршрутизация в группу исполнителей
  с балансировкой нагрузки, контроль нормативов срока (SLA) в рабочем времени.
- **Управление доступом** — маршрут согласования (руководитель → владелец ресурса → информационная
  безопасность для платёжного контура), реестр прав с полным следом предоставления и отзыва,
  автоматический отзыв прав при увольнении.
- **Отчётность** — соблюдение нормативов срока по видам заявок, нагрузка на исполнителей.

## Стек

Kotlin 2.0 · Spring Boot 3.3 (Web, Data JPA, Security, OAuth2 Resource Server) · Flyway · PostgreSQL 16 · JUnit 5

## Запуск

```bash
docker compose up -d        # PostgreSQL на порту 5433
./gradlew bootRun           # REST API на http://localhost:8080
./gradlew test              # модульные тесты расчёта контрольного срока
```

Схема и демонстрационные данные создаются миграциями Flyway (`src/main/resources/db/migration`).
Демонстрационные учётные записи (пароль `demo`): `kravtsov@vbtech.example` — заявитель,
`sokolova@…` — руководитель, `orlov@…` — владелец ресурса, `ershova@…` — информационная безопасность,
`melnikova@…`, `zaitsev@…` — исполнители, `gromov@…` — руководитель поддержки, `frolova@…` — администратор.

## Настройки

| Переменная окружения | Назначение | По умолчанию |
|---|---|---|
| `SERVICEDESK_DB_URL`, `SERVICEDESK_DB_USER`, `SERVICEDESK_DB_PASSWORD` | подключение к PostgreSQL | `jdbc:postgresql://localhost:5433/servicedesk` |
| `SERVICEDESK_JWT_SECRET` | секрет подписи токенов (не короче 32 байт) | значение для разработки |
| `SERVICEDESK_CLOCK_STARTAT` | момент, с которого идут часы приложения (стенд тестирования) | системное время |

## Структура

```
src/main/kotlin/ru/servicedesk/
  config/   SecurityConfig (JWT HS256, роли), TimeProvider (единый источник времени)
  domain/   сущности: 8 справочников и 5 оперативных таблиц
  repo/     репозитории Spring Data
  service/  TicketService · SlaCalculator · RoutingService · ApprovalRouteBuilder · AccessService · ReportService · AuthService
  web/      контроллер на каждый ресурс API, DTO, единый формат ошибок (ErrorAdvice)
src/test/kotlin/ru/servicedesk/
  service/  SlaCalculatorTest — граничные случаи расчёта срока в рабочем времени
```

## REST API (кратко)

`POST /api/auth/login` · `GET /api/catalog` · `POST /api/tickets` · `GET /api/tickets?scope=my|queue|all` ·
`POST /api/tickets/{id}/take|resolve|close` · `GET /api/approvals` · `POST /api/approvals/{id}/decision` ·
`GET /api/access-grants` · `POST /api/access-grants/{id}/revoke` · `POST /api/employees/{id}/dismiss` ·
`GET /api/reports/sla|workload?from=&to=`
