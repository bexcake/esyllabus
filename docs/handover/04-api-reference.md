# API reference

Полная интерактивная документация генерируется приложением:

- Swagger UI: `/swagger-ui.html`
- OpenAPI JSON: `/v3/api-docs`

Все непубличные endpoint-ы требуют header:

```http
Authorization: Bearer <DIGITAL_UNIVERSITY_JWT>
```

## 1. Public/Auth

| Method | Path | Назначение |
| --- | --- | --- |
| `GET` | `/api/public/health` | Healthcheck, возвращает `{"status":"ok"}`. |
| `GET` | `/api/me` | Текущий пользователь. |
| `GET` | `/api/auth/me` | Compatibility alias текущего пользователя. |
| `GET` | `/api/auth/access-denied` | Сообщение о запрете доступа. |
| `GET` | `/` | Service info. |

## 2. Courses and my syllabi

| Method | Path | Query | Назначение |
| --- | --- | --- | --- |
| `GET` | `/api/courses` | `search`, `degree`, `language`, `status` | Список курсов/дисциплин. |
| `GET` | `/api/courses/{courseId}` | - | Карточка курса. |
| `GET` | `/api/my-syllabi` | - | Силлабусы текущего пользователя. |

## 3. Syllabi

| Method | Path | Назначение |
| --- | --- | --- |
| `POST` | `/api/syllabi` | Создать силлабус. |
| `GET` | `/api/syllabi/review-queue` | Очередь проверки для текущего пользователя. |
| `GET` | `/api/syllabi/{syllabusId}` | Получить силлабус. |
| `GET` | `/api/syllabi/{syllabusId}/metadata-options` | Options для metadata формы. |
| `PUT` | `/api/syllabi/{syllabusId}` | Обновить JSON content силлабуса. |
| `PUT` | `/api/syllabi/{syllabusId}/reviewers` | Назначить коллег-рецензентов. |
| `PUT` | `/api/syllabi/{syllabusId}/director` | Назначить директора школы. |
| `POST` | `/api/syllabi/{syllabusId}/submit-review` | Отправить на согласование. |
| `POST` | `/api/syllabi/{syllabusId}/colleague-approve` | Подтвердить как коллега. |
| `POST` | `/api/syllabi/{syllabusId}/approve` | Финально подтвердить как директор. |
| `POST` | `/api/syllabi/{syllabusId}/return-for-fix` | Вернуть на доработку. |
| `POST` | `/api/syllabi/{syllabusId}/resources/import-from-library` | Импортировать книги в content силлабуса. |
| `GET` | `/api/syllabi/{syllabusId}/export-pdf` | Скачать PDF силлабуса. |

## 4. Directory

| Method | Path | Query | Назначение |
| --- | --- | --- | --- |
| `GET` | `/api/directory/schools` | - | Школы. |
| `GET` | `/api/directory/programs` | `schoolId`, `degreeLevel`, `search` | Образовательные программы. |
| `GET` | `/api/directory/academic-years` | - | Справочник academic years. |
| `GET` | `/api/directory/trimesters` | - | Справочник trimesters. |
| `GET` | `/api/directory/languages` | - | Языки обучения. |
| `GET` | `/api/directory/degree-levels` | - | Уровни образования. |
| `GET` | `/api/directory/course-types` | - | Типы курсов. |
| `GET` | `/api/directory/assessment-stages` | - | Assessment stages. |
| `GET` | `/api/directory/staff` | `schoolId`, `role` | Сотрудники. |
| `GET` | `/api/directory/staff/picker` | `schoolId`, `role`, `search` | Picker сотрудников для frontend. |
| `GET` | `/api/directory/reviewers` | `schoolId`, `syllabusId` | Доступные reviewers. |
| `GET` | `/api/directory/staff/{username}` | - | Профиль сотрудника. |

## 5. Library resources

| Method | Path | Query | Назначение |
| --- | --- | --- | --- |
| `GET` | `/api/library/books` | `query`, `limit` | Поиск книг в локальном MegaPro cache. |
| `GET` | `/api/library/disciplines` | `search` | Дисциплины с количеством синхронизированных книг. |
| `GET` | `/api/library/book-tags` | `search` | Теги книг и количество книг. |
| `POST` | `/api/library/megapro/sync` | - | Ручной запуск MegaPro sync. |
| `GET` | `/api/library/requests/export` | - | XLSX export всех доступных заявок. |

## 6. Library requests

| Method | Path | Query | Назначение |
| --- | --- | --- | --- |
| `POST` | `/api/library/requests` | - | Создать заявку. |
| `GET` | `/api/library/requests` | `status` | Получить заявки текущего пользователя/роли. |
| `GET` | `/api/library/requests/{requestId}` | - | Получить заявку. |
| `PUT` | `/api/library/requests/{requestId}` | - | Обновить заявку. |
| `DELETE` | `/api/library/requests/{requestId}` | - | Удалить заявку. |
| `POST` | `/api/library/requests/{requestId}/submit` | - | Отправить директору. |
| `POST` | `/api/library/requests/{requestId}/director-approve` | - | Подтвердить директором. |
| `POST` | `/api/library/requests/{requestId}/director-reject` | - | Отклонить директором. |
| `POST` | `/api/library/requests/{requestId}/library-feedback` | - | Feedback библиотекаря. |
| `GET` | `/api/library/requests/{requestId}/export-form` | - | XLSX export одной формы заявки. |

## 7. Digital University compatibility endpoint

| Method | Path | Назначение |
| --- | --- | --- |
| `GET` | `/api/v1/user/employees` | Compatibility endpoint для frontend: возвращает сотрудников из локальной БД/cache, а не напрямую из DU. |

## 8. Примеры curl

```bash
TOKEN="<DU_JWT>"
BASE_URL="http://localhost:8084"

curl -H "Authorization: Bearer $TOKEN" "$BASE_URL/api/me"
curl -H "Authorization: Bearer $TOKEN" "$BASE_URL/api/courses?search=software"
curl -H "Authorization: Bearer $TOKEN" "$BASE_URL/api/library/books?query=database&limit=20"
```

Создание силлабуса:

```bash
curl -X POST "$BASE_URL/api/syllabi" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"courseId":"du-subject-123"}'
```

Отправка на согласование:

```bash
curl -X POST "$BASE_URL/api/syllabi/syllabus-uuid/submit-review" \
  -H "Authorization: Bearer $TOKEN"
```

