# REST API

Base path: `/api`. Requests and responses are JSON. Authentication is an HttpOnly cookie issued by login/register. Send cookies with requests and include the exact configured `Origin` on POST, PUT, PATCH, and DELETE. The React Axios client and browser handle these automatically.

Errors have `message`, with an `errors` list for Bean Validation failures. Typical statuses: 400 invalid input, 401 not signed in, 403 wrong role/ownership/origin, 404 missing record, 409 conflicting or referenced record.

| Method     | Path                                | Access / purpose                                              |
| ---------- | ----------------------------------- | ------------------------------------------------------------- |
| GET        | `/health`                           | Public health and demo-button availability                    |
| POST       | `/auth/register`                    | Public; name, email, password, role, optional teacherCode     |
| POST       | `/auth/login`                       | Public; email and password                                    |
| POST       | `/auth/demo`                        | Demo-profile only; role STUDENT or TEACHER                    |
| GET        | `/auth/me`                          | Signed-in profile DTO                                         |
| POST       | `/auth/logout`                      | Clear session cookie                                          |
| GET        | `/classes`                          | Own or enrolled classes                                       |
| POST       | `/classes`                          | Teacher; name                                                 |
| POST       | `/classes/join`                     | Student; code                                                 |
| GET        | `/topics`                           | Own teacher topics / enrolled teachers' topics                |
| POST / PUT | `/topics`, `/topics/{id}`           | Teacher topic and prerequisite management                     |
| DELETE     | `/topics/{id}`                      | Teacher; only if not referenced                               |
| GET        | `/assessments`                      | Own teacher assessments / published class assignments         |
| GET        | `/assessments/{id}`                 | Assessment with questions; answer keys only for owner teacher |
| GET        | `/assessments/{id}/questions`       | Authorized question DTO list                                  |
| POST / PUT | `/assessments`, `/assessments/{id}` | Teacher; save assessment and nested questions atomically      |
| DELETE     | `/assessments/{id}`                 | Delete unused assessment, archive attempted assessment        |
| POST       | `/attempts`                         | Student; transactional, idempotent submission                 |
| GET        | `/attempts/{id}` or `/results/{id}` | Own attempt or assessment owner's teacher                     |
| GET        | `/gaps`                             | Student's current gaps                                        |
| GET        | `/recommendations`                  | Student's active study path                                   |
| POST       | `/recommendations/refresh`          | Regenerate student path from latest evidence/resources        |
| PATCH      | `/recommendations/{id}`             | Owning student; `{ "done": true }` or false                   |
| GET        | `/progress`                         | Student report with all attempts and latest topic mastery     |
| GET        | `/dashboards/student`               | Student dashboard DTO                                         |
| GET        | `/dashboards/teacher`               | Teacher's class students and per-topic aggregates             |
| GET        | `/students/{id}/report`             | Teacher with that student enrolled; only that teacher's data  |
| GET        | `/materials`                        | Visible learning materials                                    |
| POST / PUT | `/materials`, `/materials/{id}`     | Teacher material management                                   |
| DELETE     | `/materials/{id}`                   | Archive owned learning material                               |

## Create a topic

```json
{ "name": "Loops", "description": "Iteration in Java", "prerequisiteIds": [1] }
```

## Create an assessment

Question editing is part of the assessment save transaction. There is no separate partially saved question state.

```json
{
  "title": "Loops check",
  "classRoomId": 1,
  "miniQuiz": true,
  "topicId": 4,
  "published": true,
  "questions": [
    {
      "topicId": 4,
      "difficulty": "EASY",
      "text": "How many times does for(int i=0;i<3;i++) run?",
      "explanation": "The body runs for i = 0, 1, and 2.",
      "options": [
        { "text": "3", "correct": true },
        { "text": "4", "correct": false }
      ]
    }
  ]
}
```

Use IDs returned by your own API; example IDs above are illustrative.

## Submit

```json
{
  "submissionKey": "a-new-uuid-for-this-attempt",
  "assessmentId": 1,
  "answers": [{ "questionId": 1, "optionId": 2, "confidence": "FAIRLY_SURE" }]
}
```

Supply exactly one response for each assessment question. Option IDs must belong to their stated question. Retries must reuse both the key and the same answer payload. A genuinely new attempt uses a new key.

Response includes `id`, `assessmentId`, `assessment`, `miniQuiz`, `submittedAt`, `totalScore`, `topics` and `answers`. Answer explanations and correct option text appear only after submission or for the owning teacher.

## Material

```json
{
  "topicId": 4,
  "title": "Trace a loop",
  "type": "PRACTICE",
  "urlOrContent": "Make a table of counter and sum values for three iterations."
}
```

Types: LESSON, PRACTICE, VIDEO, LINK. VIDEO/LINK content must be an HTTP(S) URL.
