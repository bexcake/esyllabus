# ESyllabus handover package

Этот каталог предназначен для передачи проекта университету. Документы можно приложить к акту передачи кода или перенести в Confluence без изменения структуры.

## Состав пакета

| Документ | Назначение |
| --- | --- |
| [01-transfer-act.md](01-transfer-act.md) | Шаблон акта передачи кода, ссылок, доступов и артефактов. |
| [02-project-architecture.md](02-project-architecture.md) | Полное описание архитектуры backend-проекта, модулей, БД и потоков данных. |
| [03-use-cases.md](03-use-cases.md) | Пользовательские сценарии для преподавателя, коллеги-рецензента, директора школы и библиотекаря. |
| [04-api-reference.md](04-api-reference.md) | Сводная карта REST API и ссылки на OpenAPI/Swagger. |
| [05-integration-guide.md](05-integration-guide.md) | Справка по интеграциям с Digital University и MegaPro, включая примеры запросов и sequence-диаграммы. |
| [06-deployment-operations.md](06-deployment-operations.md) | Инструкции запуска, Docker Compose, CI/CD variables, эксплуатация и диагностика. |
| [07-acceptance-checklist.md](07-acceptance-checklist.md) | Checklist приемки проекта принимающей стороной. |

## Что нужно заполнить перед официальной передачей

| Поле | Значение |
| --- | --- |
| Git repository URL | `TODO: вставить ссылку на Git` |
| Jira project URL | `TODO: вставить ссылку на Jira` |
| Confluence space/page URL | `TODO: вставить ссылку на Confluence` |
| Production URL | `TODO: вставить URL сервера` |
| Swagger URL | `https://<host>/swagger-ui.html` или `http://<host>:<APP_PORT>/swagger-ui.html` |
| OpenAPI JSON | `https://<host>/v3/api-docs` или `http://<host>:<APP_PORT>/v3/api-docs` |
| Ответственный со стороны передающей команды | `TODO` |
| Ответственный со стороны университета | `TODO` |

## Важное по секретам

Секреты не должны храниться в документации или Git. Значения `POSTGRES_PASSWORD`, `DU_JWT_SECRET`, пароли серверов и CI/CD credentials передаются отдельным защищенным каналом.

