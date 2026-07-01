# Deployment and operations

## 1. Требования

| Компонент | Версия |
| --- | --- |
| Java | 21 |
| Maven | Maven Wrapper из проекта |
| Docker | Совместимая версия с Compose v2 |
| Database | PostgreSQL 17 Alpine в compose |

## 2. Локальный запуск

```bash
docker compose up -d --build
```

Проверка:

```bash
curl http://localhost:8080/api/public/health
curl http://localhost:8080/actuator/health
curl http://localhost:8080/swagger-ui.html
```

По умолчанию local compose публикует backend на `8080`.

## 3. Production compose

Файл: `docker-compose.prod.yml`

Запуск:

```bash
docker compose -f docker-compose.prod.yml up -d --build
```

Проверка:

```bash
docker compose -f docker-compose.prod.yml ps
docker compose -f docker-compose.prod.yml logs --tail=200 app
curl http://<host>:<APP_PORT>/api/public/health
```

## 4. CI/CD variables

Минимальный набор:

| Variable | Required | Пример | Комментарий |
| --- | --- | --- | --- |
| `POSTGRES_DB` | Yes | `esyllabus` | Имя БД. |
| `POSTGRES_USER` | Yes | `esyllabus` | Пользователь БД. |
| `POSTGRES_PASSWORD` | Yes | `***` | Передавать секретно. |
| `APP_PORT` | Yes | `8084` | Внешний порт backend. |
| `DU_ENABLED` | Yes | `true` | Включить DU. |
| `DU_BASE_URL` | Yes | `https://bridge-du.astanait.edu.kz` | DU bridge. |
| `DU_JWT_ENABLED` | Yes | `true` | Включить DU JWT validation. |
| `DU_JWT_SECRET` | Yes | `***` | Передавать секретно. |
| `DU_CACHE_ENABLED` | Recommended | `true` | Включить sync справочников. |
| `DU_CACHE_REFRESH_INTERVAL` | Optional | `12h` | TTL локального marker. |
| `DU_CACHE_REFRESH_CRON` | Optional | `0 0 */12 * * *` | Cron sync каждые 12 часов. |
| `DU_CACHE_PAGE_SIZE` | Optional | `100` | Page size DU employees/disciplines. |
| `DU_CACHE_MAX_PAGES` | Optional | `50` | Защита от бесконечной пагинации. |

Не задавать:

| Variable | Причина |
| --- | --- |
| `DU_SERVICE_TOKEN` | Не используется. DU sync работает только через пользовательские DU JWT. |
| `APP_USER_*` | Старые basic-auth users не используются. |

## 5. Сборка и тесты

Windows:

```powershell
.\mvnw.cmd -q test
.\mvnw.cmd -q -DskipTests package
```

Linux:

```bash
./mvnw -q test
./mvnw -q -DskipTests package
```

Docker build:

```bash
docker compose build app
```

## 6. Health and observability

Health endpoint:

```http
GET /api/public/health
GET /actuator/health
GET /actuator/info
```

Expected:

```json
{"status":"ok"}
```

Actuator health expected:

```json
{"status":"UP"}
```

HTTP request logs включают:

- method;
- path;
- status;
- durationMs;
- user;
- remoteIp;
- forwardedFor;
- userAgent;
- referer;
- errorType/errorMessage, если было исключение.

Пример:

```text
http_request method=GET path="/api/me" status=200 durationMs=18 user=user@astanait.edu.kz remoteIp=10.1.20.5
```

## 7. Database operations

Текущая схема управляется Hibernate `spring.jpa.hibernate.ddl-auto=update`.

Рекомендации для production:

- делать регулярные PostgreSQL backups;
- перед обновлением версии приложения делать dump БД;
- в будущем рассмотреть Flyway/Liquibase для строгого контроля миграций.

Backup example:

```bash
docker exec esyllabus-postgres pg_dump -U "$POSTGRES_USER" "$POSTGRES_DB" > esyllabus-backup.sql
```

Restore example:

```bash
cat esyllabus-backup.sql | docker exec -i esyllabus-postgres psql -U "$POSTGRES_USER" "$POSTGRES_DB"
```

## 8. Troubleshooting

| Проблема | Что проверить |
| --- | --- |
| App не стартует | `docker compose logs app`, DB healthcheck, `DB_URL`, `DB_USER`, `DB_PASSWORD`. |
| Swagger не открывается | `/swagger-ui.html`, reverse proxy path, SpringDoc settings. |
| `401 Unauthorized` | Bearer token, `DU_JWT_SECRET`, expiration, server time. |
| `/api/me` медленный | Не должно быть DU blocking calls; проверить логи sync и DB latency. |
| DU sync не запускается | Был ли валидный пользовательский JWT после рестарта, `DU_CACHE_ENABLED=true`. |
| Ошибка `429` от DU | Проверить частоту sync, cooldown, frontend не должен напрямую дергать DU через backend compatibility route. |
| Пустой directory/staff | DU sync не прошел или пользовательский token не пришел. |
| Пустые книги | MegaPro выключен или cache пустой. |

## 9. Production hardening recommendations

- Настроить reverse proxy HTTPS.
- Передавать `X-Forwarded-*` headers корректно; приложение использует `server.forward-headers-strategy=framework`.
- Хранить секреты только в CI/CD secret variables.
- Ограничить доступ к Swagger в production, если требуется политика ИБ.
- Добавить централизованный сбор логов.
- Добавить мониторинг `/api/public/health`.
- Добавить автоматический DB backup.
