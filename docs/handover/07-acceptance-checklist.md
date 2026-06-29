# Acceptance checklist

## 1. Repository and documentation

| Check | Status | Comment |
| --- | --- | --- |
| Git repository доступен принимающей стороне | `TODO` | URL: `TODO`. |
| Jira ссылка передана | `TODO` | URL: `TODO`. |
| Confluence ссылка передана | `TODO` | URL: `TODO`. |
| Handover docs находятся в `docs/handover` | `TODO` | Этот пакет. |
| Swagger UI доступен | `TODO` | `/swagger-ui.html`. |
| OpenAPI JSON доступен | `TODO` | `/v3/api-docs`. |

## 2. Build and deploy

| Check | Команда/действие | Expected |
| --- | --- | --- |
| Tests | `./mvnw -q test` | Success. |
| Package | `./mvnw -q -DskipTests package` | JAR created. |
| Docker build | `docker compose build app` | Success. |
| Prod compose starts | `docker compose -f docker-compose.prod.yml up -d --build` | `app` and `postgres` healthy/running. |
| Healthcheck | `GET /api/public/health` | `{"status":"ok"}`. |

## 3. Auth and DU integration

| Check | Expected |
| --- | --- |
| Request without token to protected API | `401 Unauthorized`. |
| Request with valid DU JWT to `/api/me` | AuthenticatedUser JSON. |
| `DU_SERVICE_TOKEN` absent from CI/CD | No such variable required. |
| First DU sync after restart waits for valid user token | No startup failure due missing service token. |
| DU sync fills `schools`, `staff_profiles`, `du_programs`, `courses` | Records exist after sync. |

## 4. Syllabus workflow

| Step | Expected |
| --- | --- |
| Teacher creates syllabus | Status `DRAFT`. |
| Teacher updates content | Progress/sections updated. |
| Teacher selects colleagues | Reviewers saved. |
| Teacher selects director | Director saved and must be school director. |
| Submit review with colleagues | Status `PENDING_COLLEAGUE_CONFIRMATION`. |
| Colleagues approve | After all approvals status `PENDING_DIRECTOR_REVIEW`. |
| Director approve | Status `PUBLISHED`. |
| Director return for fix | Status back to `DRAFT`, comment saved. |
| PDF export | Downloads valid PDF. |

## 5. Library workflow

| Step | Expected |
| --- | --- |
| Published syllabus with books | Linked library request created/updated. |
| Manual request create | Status `DRAFT`. |
| Submit request | Status `PENDING_DIRECTOR_APPROVAL`. |
| Director approve | Status `APPROVED_BY_DIRECTOR`. |
| Director reject | Status `REJECTED_BY_DIRECTOR`, comment required. |
| Librarian feedback | Status `FEEDBACK_PROVIDED`, optional month format `yyyy-MM`. |
| Export all requests | XLSX downloaded. |
| Export single request form | XLSX downloaded. |

## 6. MegaPro/library resources

| Check | Expected |
| --- | --- |
| `GET /api/library/books?query=...` | Returns books from cache. |
| `GET /api/library/book-tags` | Returns tags and counts. |
| `GET /api/library/disciplines` | Returns discipline catalog with book counts. |
| Manual MegaPro sync | Report returned, cache updated if MegaPro enabled. |

## 7. Operational readiness

| Check | Expected |
| --- | --- |
| Logs include request duration/user/path | `http_request` lines present. |
| DB backup procedure documented | Section exists in operations guide. |
| Secrets are not stored in Git | No real passwords/JWT secrets in repo. |
| Reverse proxy/HTTPS planned | `TODO` by infrastructure team. |
| Monitoring healthcheck planned | `TODO` by infrastructure team. |

