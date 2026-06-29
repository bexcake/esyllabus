# Use cases

## 1. Акторы

| Актор | Роль | Основные действия |
| --- | --- | --- |
| Преподаватель | `TEACHER` | Создает и редактирует силлабус, выбирает коллег и директора, отправляет на согласование. |
| Коллега-рецензент | `TEACHER` | Проверяет назначенный силлабус, подтверждает или возвращает на доработку. |
| Директор школы | `DIRECTOR` | Финально подтверждает силлабус, возвращает на доработку, подтверждает/отклоняет библиотечные заявки. |
| Библиотекарь | `LIBRARIAN` | Видит утвержденные заявки, выгружает XLSX, оставляет feedback по закупке. |
| Интеграционный источник DU | External system | Передает JWT и справочники сотрудников/школ/программ/дисциплин. |
| MegaPro | External system | Источник книг и библиотечных ресурсов. |

## 2. UC-01. Авторизация пользователя через Digital University

**Цель:** определить текущего пользователя, его DU identifiers, школу и роль.

**Предусловия:**

- Frontend получил DU JWT.
- Backend настроен с `DU_JWT_SECRET`.

**Основной поток:**

1. Frontend вызывает `GET /api/me` с `Authorization: Bearer <DU_JWT>`.
2. Backend валидирует JWT локально.
3. Backend ищет пользователя в `staff_profiles` по `du_user_id` или email.
4. Если профиль уже синхронизирован, backend возвращает полные данные.
5. Если профиль еще не найден, backend возвращает быстрый fallback из JWT и ставит DU sync в background pending.

**Результат:** frontend получает `AuthenticatedUser`.

## 3. UC-02. Синхронизация справочников Digital University

**Цель:** обновить локальные справочники школ, сотрудников, программ и дисциплин.

**Триггеры:**

- startup приложения;
- cron каждые 12 часов;
- приход валидного пользовательского DU JWT, если sync был pending.

**Основной поток:**

1. Backend проверяет marker `du_cache_entries.reference-data`.
2. Если данные свежие, sync не запускается.
3. Если данные устарели и есть валидный пользовательский токен, backend запускает background sync.
4. Backend получает DU employees, schools, education programs, teacher disciplines.
5. Backend upsert-ит `staff_profiles`, `schools`, `du_programs`, `courses`.
6. Backend обновляет marker успешной синхронизации.

**Исключения:**

- Если DU вернул ошибку или `429 Too Many Requests`, backend ставит cooldown.
- Обычный пользовательский запрос не должен ждать завершения sync.

## 4. UC-03. Преподаватель создает силлабус

**Endpoint:** `POST /api/syllabi`

**Request example:**

```json
{
  "courseId": "du-subject-123"
}
```

**Основной поток:**

1. Преподаватель выбирает дисциплину из `GET /api/courses`.
2. Frontend вызывает `POST /api/syllabi`.
3. Backend создает `DRAFT` силлабус.
4. Если `courseId` указан, content создается из данных курса.
5. Преподаватель редактирует content через `PUT /api/syllabi/{syllabusId}`.

## 5. UC-04. Преподаватель назначает коллег и директора

**Endpoints:**

- `PUT /api/syllabi/{syllabusId}/reviewers`
- `PUT /api/syllabi/{syllabusId}/director`
- `GET /api/directory/staff/picker`
- `GET /api/directory/reviewers`

**Reviewers request:**

```json
{
  "reviewerUsernames": [
    "teacher.colleague@astanait.edu.kz"
  ]
}
```

**Director request:**

```json
{
  "directorUsername": "director.school@astanait.edu.kz"
}
```

**Правила:**

- В colleagues нельзя добавить владельца силлабуса.
- В colleagues нельзя добавить библиотекаря.
- В colleagues нельзя добавить директора школы, потому что директор подтверждает финальным отдельным действием.
- Директор выбирается отдельно и должен иметь staff role `SCHOOL_DIRECTOR`.

## 6. UC-05. Преподаватель отправляет силлабус на согласование

**Endpoint:** `POST /api/syllabi/{syllabusId}/submit-review`

**Предусловия:**

- Силлабус в статусе `DRAFT`.
- Заполнены обязательные секции.
- Выбран директор школы или backend может определить директора по школе.

**Результат:**

- Если colleagues выбраны: статус `PENDING_COLLEAGUE_CONFIRMATION`.
- Если colleagues не выбраны: статус `PENDING_DIRECTOR_REVIEW`.

## 7. UC-06. Коллега подтверждает или возвращает силлабус

**Approve endpoint:** `POST /api/syllabi/{syllabusId}/colleague-approve`

**Return endpoint:** `POST /api/syllabi/{syllabusId}/return-for-fix`

**Return request:**

```json
{
  "comment": "Необходимо уточнить список литературы и результаты обучения."
}
```

**Правила:**

- Коллега может подтверждать только силлабусы, где он назначен reviewer.
- Директор не может использовать colleague approve.
- После подтверждения всех коллег статус переходит в `PENDING_DIRECTOR_REVIEW`.
- При возврате на доработку статус становится `DRAFT`, approvals очищаются.

## 8. UC-07. Директор школы финально подтверждает силлабус

**Approve endpoint:** `POST /api/syllabi/{syllabusId}/approve`

**Return endpoint:** `POST /api/syllabi/{syllabusId}/return-for-fix`

**Правила:**

- Подтверждать может только назначенный директор школы.
- Директор подтверждает только статусы `PENDING_DIRECTOR_REVIEW` или compatibility `NEEDS_REVIEW`.
- После подтверждения статус становится `PUBLISHED`.
- После публикации backend синхронизирует библиотечную заявку по книгам из силлабуса.

## 9. UC-08. Работа с библиотечными заявками

**Manual CRUD endpoints:**

- `POST /api/library/requests`
- `GET /api/library/requests`
- `GET /api/library/requests/{requestId}`
- `PUT /api/library/requests/{requestId}`
- `DELETE /api/library/requests/{requestId}`

**Approval endpoints:**

- `POST /api/library/requests/{requestId}/submit`
- `POST /api/library/requests/{requestId}/director-approve`
- `POST /api/library/requests/{requestId}/director-reject`
- `POST /api/library/requests/{requestId}/library-feedback`

**Manual request example:**

```json
{
  "department": "School of Engineering",
  "educationLevel": "Bachelor",
  "requestDate": "2026-06-29",
  "items": [
    {
      "title": "Clean Architecture",
      "author": "Robert C. Martin",
      "isbn": "9780134494166",
      "publisher": "Pearson",
      "publicationYear": "2017",
      "discipline": "Software Engineering",
      "educationalProgram": "Computer Science",
      "courseNumber": 3,
      "trimester": "Term 1",
      "quantity": 10,
      "literatureType": "Main"
    }
  ]
}
```

**Library feedback example:**

```json
{
  "feedback": "Планируется закупка после согласования бюджета.",
  "expectedPurchaseMonth": "2026-09"
}
```

## 10. UC-09. Поиск книг и импорт ресурсов в силлабус

**Endpoints:**

- `GET /api/library/books?query=<text>&limit=20`
- `GET /api/library/book-tags?search=<text>`
- `GET /api/library/disciplines?search=<text>`
- `POST /api/syllabi/{syllabusId}/resources/import-from-library`

**Import request:**

```json
{
  "books": [
    {
      "title": "Database System Concepts",
      "author": "Silberschatz, Korth, Sudarshan",
      "year": "2020",
      "type": "Main",
      "url": "https://example.edu/book"
    }
  ]
}
```

## 11. UC-10. Экспорт

| Экспорт | Endpoint | Формат |
| --- | --- | --- |
| Силлабус | `GET /api/syllabi/{syllabusId}/export-pdf` | PDF |
| Все библиотечные заявки | `GET /api/library/requests/export` | XLSX |
| Одна форма заявки | `GET /api/library/requests/{requestId}/export-form` | XLSX |

