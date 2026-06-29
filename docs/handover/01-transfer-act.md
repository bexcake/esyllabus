# Акт передачи кода и технической документации

## 1. Общая информация

| Поле | Значение |
| --- | --- |
| Проект | ESyllabus |
| Назначение | Backend-сервис для управления силлабусами, согласования силлабусов, заявок на библиотечные ресурсы и интеграций с Digital University и MegaPro. |
| Передающая сторона | `TODO` |
| Принимающая сторона | `TODO` |
| Дата передачи | `TODO` |
| Версия/commit | `TODO: указать git commit hash` |
| Среда передачи | `TODO: test/stage/prod` |

## 2. Передаваемые артефакты

| Артефакт | Статус | Комментарий |
| --- | --- | --- |
| Backend source code | Передается | Java 21, Spring Boot 4.0.3, Maven. |
| Dockerfile | Передается | Multi-stage build, runtime image на Eclipse Temurin 21 JRE. |
| docker-compose.yml | Передается | Локальный запуск backend + PostgreSQL. |
| docker-compose.prod.yml | Передается | Production-oriented compose для CI/CD деплоя. |
| OpenAPI/Swagger | Генерируется приложением | `/swagger-ui.html`, `/v3/api-docs`. |
| Handover documentation | Передается | Каталог `docs/handover`. |
| Unit/integration tests | Передается | Maven tests в `src/test`. |
| CI/CD variables | Передаются отдельно | Секреты не хранятся в Git. |

## 3. Ссылки

| Система | Ссылка | Комментарий |
| --- | --- | --- |
| Git repository | `TODO` | Ссылка на репозиторий проекта. |
| Jira project/board | `TODO` | Ссылка на задачи, backlog, bugs. |
| Confluence space | `TODO` | Ссылка на документацию проекта. |
| Test environment | `TODO` | URL тестового backend. |
| Production environment | `TODO` | URL production backend. |
| Swagger UI | `TODO: https://<host>/swagger-ui.html` | Интерактивная OpenAPI-документация. |
| OpenAPI JSON | `TODO: https://<host>/v3/api-docs` | Машиночитаемый API contract. |

## 4. Состав проекта

| Раздел | Описание |
| --- | --- |
| `src/main/java/kz/iqadam/esyllabus/web` | REST controllers. |
| `src/main/java/kz/iqadam/esyllabus/security` | DU JWT authentication, current user, role normalization. |
| `src/main/java/kz/iqadam/esyllabus/directory` | Школы, сотрудники, справочники для UI. |
| `src/main/java/kz/iqadam/esyllabus/syllabus` | Силлабусы, курсы, PDF export, workflow согласования. |
| `src/main/java/kz/iqadam/esyllabus/requests` | Библиотечные заявки, approval, feedback, XLSX export. |
| `src/main/java/kz/iqadam/esyllabus/integration/digital` | Digital University bridge, sync справочников, локальный cache marker. |
| `src/main/java/kz/iqadam/esyllabus/integration/megapro` | MegaPro search/sync и локальный кэш книг. |
| `src/main/resources/application.properties` | Основные настройки приложения. |
| `pom.xml` | Maven dependencies/build config. |

## 5. Передача доступов

Доступы и секреты должны передаваться отдельным защищенным каналом, не в Git и не в этом акте.

| Доступ | Передается | Комментарий |
| --- | --- | --- |
| Git access | `TODO` | Read/write или read-only по решению университета. |
| Server SSH | `TODO` | Только ответственным инженерам. |
| CI/CD variables | `TODO` | `POSTGRES_*`, `DU_JWT_SECRET`, `APP_PORT`, DU/MegaPro settings. |
| Database credentials | `TODO` | PostgreSQL credentials. |
| Digital University JWT secret | `TODO` | HMAC secret для валидации пользовательских JWT. |
| MegaPro API credentials | `TODO` | Если включается внешняя MegaPro-интеграция. |

## 6. Проверка перед передачей

| Проверка | Команда/URL | Ожидаемый результат |
| --- | --- | --- |
| Unit/integration tests | `./mvnw.cmd -q test` или `./mvnw -q test` | Build success. |
| Docker build | `docker compose build app` | Image собирается без ошибок. |
| Local healthcheck | `GET /api/public/health` | `{"status":"ok"}`. |
| Swagger UI | `GET /swagger-ui.html` | Открывается Swagger UI. |
| OpenAPI JSON | `GET /v3/api-docs` | Возвращается JSON contract. |
| Auth check | `GET /api/me` с DU Bearer JWT | Возвращается профиль текущего пользователя. |

## 7. Подписи

| Сторона | ФИО | Должность | Подпись | Дата |
| --- | --- | --- | --- | --- |
| Передающая сторона | `TODO` | `TODO` | `TODO` | `TODO` |
| Принимающая сторона | `TODO` | `TODO` | `TODO` | `TODO` |

