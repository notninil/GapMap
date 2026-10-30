# GapMap

**A Learning Gap Analysis and Recommendation System**

GapMap helps students understand their quiz results by topic and helps teachers decide where extra support is needed. It includes a real Spring Boot REST backend, a React frontend, persistent results, and a deterministic recommendation engine.

## Start here

The project has the requested two application folders:

```text
gapmap/
  gapmap-backend/    Java 17, Spring Boot 3, Spring Security, JPA, JUnit/Mockito
  gapmap-frontend/   React, Vite, React Router, Axios, Tailwind CSS, Recharts
  docs/             Schema, API reference, design decisions, verification
  compose.yaml      MySQL + backend + frontend
```

### Option A: Run everything with Docker and MySQL

Install Docker Desktop with Docker Compose support, then open a terminal in the extracted `gapmap` folder:

```bash
docker compose up --build
```

Open **http://localhost:5173** after the backend finishes starting. The first build downloads Maven and npm dependencies and can take several minutes. MySQL has a health check; the backend starts after the database is ready. The frontend may briefly show a connection error while the backend seeds its records; retry after startup completes.

The database persists in the `gapmap_mysql` Docker volume. `docker compose down` stops the app and preserves it. Do not remove that volume unless you intentionally want to delete all saved data.

This Compose configuration is for local development: it binds the frontend to localhost and uses clearly labeled demo secrets. Copy `.env.example` to `.env` to change settings before building a deployment. The backend and MySQL are reachable by the frontend's internal proxy; their ports are not exposed to the host.

### Option B: Quick demo without installing MySQL

Requirements: **Java 17+**, **Maven 3.9+**, **Node.js 20.19+ or 22+**, and npm.

Terminal 1:

```bash
cd gapmap-backend
mvn spring-boot:run -Dspring-boot.run.profiles=demo
```

Terminal 2:

```bash
cd gapmap-frontend
npm ci
npm run dev
```

Open **http://localhost:5173**. The `demo` profile uses a persistent local H2 database in `gapmap-backend/data/`. This is an optional convenience profile; MySQL remains the default database. The demo profile exposes Student demo and Teacher demo buttons on the login screen. Those password-free demo buttons are disabled outside that profile by default.

### Option C: Local MySQL + Maven + Vite

Create the database and development account in MySQL:

```sql
CREATE DATABASE gapmap CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'gapmap'@'localhost' IDENTIFIED BY 'gapmap_local';
GRANT ALL PRIVILEGES ON gapmap.* TO 'gapmap'@'localhost';
```

Configure `gapmap-backend/src/main/resources/application.properties`, or set environment variables:

| Variable              | Purpose                                                                     |
| --------------------- | --------------------------------------------------------------------------- |
| `DB_URL`              | Default: `jdbc:mysql://localhost:3306/gapmap?createDatabaseIfNotExist=true` |
| `DB_USERNAME`         | Default: `gapmap`                                                           |
| `DB_PASSWORD`         | Default local password: `gapmap_local`                                      |
| `JWT_SECRET`          | Required outside demo: at least 32 characters; use a long random secret     |
| `FRONTEND_ORIGIN`     | Exact browser origin; default `http://localhost:5173`                       |
| `COOKIE_SECURE`       | `false` for local HTTP; set `true` for HTTPS deployment                     |
| `TEACHER_INVITE_CODE` | Required invitation code for teacher registrations                          |
| `SEED_DATA`           | `true` loads sample records only when the users table is empty              |
| `SEED_PASSWORD`       | Required when seeding; at least 8 characters                                |
| `PORT`                | Backend port; default `8080`                                                |

Example for **Windows PowerShell**, inside `gapmap-backend`:

```powershell
$env:JWT_SECRET = "local-demo-change-this-secret-before-production-123456789"
$env:TEACHER_INVITE_CODE = "GAPMAP-TEACHER"
$env:SEED_DATA = "true"
$env:SEED_PASSWORD = "GapMap123!"
mvn spring-boot:run
```

For macOS/Linux, use `export NAME='value'` instead of `$env:NAME = "value"`. Run the frontend as shown in Option B.

`.env` is read by Docker Compose. Spring Boot and Vite do not automatically load the root `.env` when launched independently; use environment variables or `application.properties` in that case.

## Demo accounts

With the demo profile or the supplied Compose defaults:

| Role           | Email                                                  | Password     |
| -------------- | ------------------------------------------------------ | ------------ |
| Teacher        | `teacher@gapmap.test`                                  | `GapMap123!` |
| Student        | `student1@gapmap.test`                                 | `GapMap123!` |
| Other students | `student2@gapmap.test` through `student10@gapmap.test` | `GapMap123!` |

Teacher invitation: **GAPMAP-TEACHER**. Seeded class join code: **GAPMAP-G1**.

The seed creates one teacher, ten students, one enrolled class, six linked topics, 30 main-assessment questions across Easy/Medium/Hard, six mini-quizzes with three questions each, two learning materials per topic, and 30 historical attempts. Seeded names and results are fictional. Repeated starts do not duplicate the seed.

## Try the main workflows

### Student

1. Sign in as a seeded student, or register and join a class using its code.
2. Open **Assessments** and choose an assessment.
3. Answer each question and select Not sure, Fairly sure, or Very sure.
4. Review and submit. Answers, total score, topic results, gaps, and recommendations are committed together.
5. Read the topic breakdown and answer explanations on the result page.
6. Open **My learning path**, read an activity, and mark it complete.
7. Take the matching mini-quiz to check understanding.
8. Open **My progress** to compare topic scores across all attempts.

### Teacher

1. Create a class and share its join code with students.
2. Create topics and choose prerequisites. Cycles and links to another teacher's topics are rejected.
3. Add lessons, practice instructions, video links, or other links.
4. Create an assessment with one correct answer per question, a topic, difficulty, and explanation.
5. Publish it to an owned class. Drafts are not visible to students.
6. Use the classroom heatmap, weak-topic list, and support list to inspect results.
7. Open a student's report, inspect possible misconceptions, and export the current topic report as CSV.

## Analysis rules

- Topic score = correct answers in the topic / all questions answered in that topic × 100.
- Questions have equal weight. Difficulty is recorded but does not change scoring.
- Defaults: **Strong ≥ 80%**, **Developing ≥ 50% and < 80%**, **Needs practice < 50%**.
- Change `app.mastery.strong` and `app.mastery.developing` in `application.properties`. Invalid threshold ordering fails startup.
- `app.gaps.include-developing=true` includes Developing topics in study recommendations. Set it to `false` to include only Needs practice, root gaps, and misconception signals.
- A possible misconception is an **incorrect answer with Very sure confidence**. It is a signal for review, not a diagnosis.
- Root analysis uses each topic's latest assessed result. It follows all prerequisite branches and chooses weak prerequisites with no deeper weak prerequisite below them. Unassessed topics are unknown, not automatically weak.
- Study recommendations are ordered by root gaps, direct gaps, then misconception review. Within a category, prerequisite topics precede dependent topics. Lessons and practice activities are attached from the teacher's material collection.
- Completing a lesson does not change mastery. A new assessment or mini-quiz provides the evidence for improved mastery.
- Use **Refresh learning path** after a teacher adds resources or edits prerequisites. New submissions also regenerate the path automatically. Completed activity states are preserved for matching active recommendations; no-longer-active gaps are resolved.
- Progress charts show each saved attempt. An unassessed topic is a gap in the chart, not a zero score.
- Teacher topic averages use the latest result per student per topic. The support list flags a student with at least one Needs practice topic. This is a transparent rule, not an AI prediction.

## Editing and history

An assessment with no submissions can be edited or deleted. Once it has submissions, its questions become immutable. The editor offers **Save as new assessment** to create a revised version; deletion archives the original. Historical attempts still refer to their original questions and explanations.

Topics used by questions or reports cannot be deleted while referenced. Remove prerequisite links before deleting an unused topic. Materials are archived rather than physically removed.

## Security and persistence

- Passwords are BCrypt hashed. Password hashes and JPA entities are never returned as API responses.
- JWT sessions expire after eight hours and are stored in an HttpOnly, SameSite=Strict cookie. React does not put JWTs in localStorage or sessionStorage.
- Cookie-authenticated mutations require an exact matching `Origin` header, including login and logout. This is the CSRF control. CORS accepts only the configured frontend origin.
- Role checks protect teacher/student endpoints, and ownership/enrollment checks protect individual records.
- Teacher registration requires the configured invitation code.
- Student quiz responses and confidence are saved in **sessionStorage** as drafts for the current tab. They contain no authentication token and are removed after submission or logout.
- Every submission carries a client-generated idempotency key and a hash of its contents. A student-row database lock serializes simultaneous submissions; a database uniqueness constraint is a second safeguard. Retrying the same payload returns the same attempt. Reusing the key for different contents is rejected.
- Submission processing is transactional: an analysis failure rolls back the whole attempt.
- Production deployment should use HTTPS, unique secrets, `COOKIE_SECURE=true`, an exact `FRONTEND_ORIGIN`, and no demo profile. Configure rate limiting, backups, and operational monitoring at the deployment layer if used beyond a classroom demo.

## Tests and build

```bash
cd gapmap-backend
mvn test
mvn package

cd ../gapmap-frontend
npm ci
npm run build
```

Backend unit tests cover all four requested analysis services. Integration tests cover permissions, ownership, enrollment, assessment CRUD, confidence/result recording, recommendations, mini-quizzes, submission rollback, and concurrent retry behavior. See `docs/VERIFICATION.md` for the actual checks performed and remaining environment limits.

The frontend production bundle is created in `gapmap-frontend/dist/`. The executable backend JAR is created in `gapmap-backend/target/`. The provided nginx configuration serves the frontend, supports React Router refreshes, and proxies `/api` to the backend.

## Troubleshooting

- **Login fails or a form says “Request origin is not allowed”:** use `localhost` consistently rather than mixing it with `127.0.0.1`. Match `FRONTEND_ORIGIN` to the browser's exact scheme, host, and port.
- **No demo accounts:** seeding was disabled or the database already contained a user. Seed loading intentionally does not overwrite an existing database.
- **A new student sees no assessments:** join a class first. Its teacher must publish an assessment for that class.
- **A gap has no activities:** add materials for that topic, then refresh the student's learning path.
- **MySQL connection refused:** confirm MySQL is running, the database exists, and the configured user has access.
- **Java cannot find the main class:** run Maven from `gapmap-backend`, and use Java 17 or newer.
- **Frontend connection error:** confirm the backend is on port 8080. For a different backend address, set `API_PROXY_TARGET` when running Vite.

## Scope

All requested Must-have and Should-have workflows are implemented. The optional extra included is CSV export of a student's current topic report. External AI calls, adaptive difficulty, predictive risk models, language translation, PDF export, and Excel workbook export are not implemented. Recommendations are explainable rules based on actual saved responses.
An AI-driven learning gap analysis tool. GapMap analyzes quiz results per
topic, finds each student's weak areas and root causes, and recommends a
personalized study path. Teachers get a class-level report of the most
common gaps.

## Team
- Cabahug, Imee
- Labagala, Carlo Jose
- Mutya, Ryza Janell

## Tech Stack
- Backend: Spring Boot, MySQL
- Frontend: React (Vite), JavaScript

## Project Structure
- `backend/`: Spring Boot API
- `frontend/`: React app
- `docs/`: ERD, API endpoints, wireframes

## How to Run
### Backend
_To be added after setup._
### Frontend
_To be added after setup._

## Links
- Trello board: https://trello.com/invite/b/6adb4b9b0c91bfe81bc90152/ATTI383569daa0584c8bbadb001c72765421E4042A28/gapmap

## Git Workflow
- `main`: stable, demo-ready only
- `dev`: integration branch
- `feature/<name>`: one branch per Trello card, merged into `dev` via pull request
