# ESyllabus backend

Backend-сервис для управления силлабусами, процессом согласования силлабусов, библиотечными заявками и интеграциями с Digital University и MegaPro.

## Кратко

- Java 21, Spring Boot 4.0.3, Maven.
- PostgreSQL для runtime, H2 для tests/local fallback.
- Stateless authentication через Digital University Bearer JWT.
- Basic Auth и тестовые `APP_USER_*` пользователи не используются.
- OpenAPI доступен через `/swagger-ui.html` и `/v3/api-docs`.
- Actuator health/info доступны публично через `/actuator/health` и `/actuator/info`.
- Docker deployment описан в `docker-compose.yml` и `docker-compose.prod.yml`.

## Документация для передачи

Полный пакет передачи находится в [docs/handover](docs/handover/README.md):

- акт передачи кода;
- архитектура проекта;
- use cases;
- API reference;
- integration guide по Digital University и MegaPro;
- deployment/operations guide;
- checklist приемки.

## Быстрый запуск

```bash
docker compose up -d --build
```

Проверка:

```bash
curl http://localhost:8080/api/public/health
curl http://localhost:8080/actuator/health
```

Swagger UI:

```text
http://localhost:8080/swagger-ui.html
```

## Тесты

Windows:

```powershell
.\mvnw.cmd -q test
```

Linux/macOS:

```bash
./mvnw -q test
```

## Основные production variables

| Variable | Назначение |
| --- | --- |
| `POSTGRES_DB` | Имя PostgreSQL database. |
| `POSTGRES_USER` | Пользователь PostgreSQL. |
| `POSTGRES_PASSWORD` | Пароль PostgreSQL. |
| `APP_PORT` | Внешний порт приложения. |
| `DU_ENABLED` | Включить Digital University integration. |
| `DU_BASE_URL` | Base URL Digital University bridge. |
| `DU_JWT_ENABLED` | Включить DU JWT validation. |
| `DU_JWT_SECRET` | HMAC secret для DU JWT. |
| `DU_CACHE_ENABLED` | Включить sync справочников DU. |

`DU_SERVICE_TOKEN` не используется: sync DU работает только через валидные пользовательские DU JWT, полученные от frontend.
