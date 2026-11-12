# GapMap

An AI-driven learning gap analysis tool. GapMap analyzes quiz results per
topic, finds each student's weak areas and root causes, and recommends a
personalized study path. Teachers get a class-level report of the most
common gaps.

---

## Team
* **Cabahug, Imee** — Project Manager & Full-Stack Developer
* **Labagala, Carlo Jose** — UI, QA, & Full-Stack Developer
* **Mutya, Ryza Janell** — Full-Stack Developer, QA & UI

---

## Tech Stack Overview

* **Backend:** Java target 17, Spring Boot 3.4.5, Spring Security, Spring Data JPA, JWT HttpOnly session cookies. Use a Spring Boot-supported JDK; local checks used JDK 25, while Java 17 runtime verification remains outstanding.
* **Frontend:** React 19, Vite, Tailwind CSS, Axios, Recharts.
* **Databases:** H2 file-backed demo; PostgreSQL/Supabase runtime in a private `gapmap` schema. Legacy MySQL settings and Compose configuration remain available but are not verified by the PostgreSQL checks.

## Supported configurations

| Configuration | Backend | Frontend | Schema management |
| --- | --- | --- | --- |
| H2 demo (`demo`) | http://localhost:8080 | http://localhost:5173 | Existing Hibernate update behavior |
| Supabase local HTTPS (`supabase,local-https`) | https://localhost:8443 | https://localhost:5173 | Hibernate validate; runtime Flyway disabled |
| Legacy default MySQL | http://localhost:8080 | http://localhost:5173 | Existing Hibernate update behavior; not recently verified |

Local HTTPS is development infrastructure, not public deployment. Do not combine `demo` and `supabase` profiles.

## Project structure

- `gapmap-backend/`: Spring Boot API, PostgreSQL migrations, unit/workflow tests.
- `gapmap-frontend/`: React UI, Vite API proxy, local HTTPS proxy check.
- `docs/`: requirements, architecture, team roles, database and setup runbooks.
- `.env.example`: demo defaults and placeholder configuration names; no real deployment secrets.
- `CONTRIBUTING.md`: contribution and branch conventions.

## H2 demo setup

Install Maven, a compatible JDK (Java target 17), Node.js and npm. H2 requires no XAMPP or separate database server. Use fresh terminals without Supabase/HTTPS environment settings.

```powershell
# Backend terminal, from repository root:
Set-Location gapmap-backend
mvn spring-boot:run '-Dspring-boot.run.profiles=demo'
```

```powershell
# Frontend terminal, from repository root:
Set-Location gapmap-frontend
npm ci
npm run dev
```

Open http://localhost:5173. The demo stores data under `gapmap-backend/data/`; back up a stopped file database before changing it. Demo startup seeds fixtures; never apply demo settings to Supabase.

### Demo-only accounts

The seed/controller implementation uses `teacher@gapmap.test` and `student1@gapmap.test`, with the demo password `GapMap123!`. The demo teacher invitation code is `GAPMAP-TEACHER`. Demo sign-in is also available through the UI. These are public local fixtures, not deployment credentials. Supabase disables demo sign-in, seeding and reset-link logging.

## Supabase PostgreSQL setup

1. Follow [the PostgreSQL runbook](docs/POSTGRESQL.md) to review administrative provisioning and Flyway V1–V3. Provision the private `gapmap` schema with separate `gapmap_migrator`, `gapmap_runtime` and `gapmap_app` roles. Migrations cover 15 application tables plus protected Flyway history.
2. Run reviewed migrations separately as the migrator; do not run them through normal runtime startup. Runtime uses `gapmap_app`, validates mappings, and does not create/alter tables or seed data.
3. Follow [Supabase runtime instructions](docs/SUPABASE-RUNTIME.md) for exact Windows launch commands and masked secret prompts. Supply `SUPABASE_DB_URL`, `SUPABASE_DB_USER`, `SUPABASE_DB_PASSWORD`, a stable random `JWT_SECRET`, `TEACHER_INVITE_CODE`, and `FRONTEND_ORIGIN` through process environment or a hosting secret manager. Runtime needs no migration credentials.
4. JDBC requires `sslmode=verify-full` and a trusted database CA file. Session pooling uses port 5432 and username `gapmap_app.<project-ref>`; copy the actual host from the project's Connect dialog.
5. Follow [local HTTPS instructions](docs/LOCAL-HTTPS.md) for externally stored certificates and the HTTPS frontend-to-backend proxy. Set `FRONTEND_ORIGIN=https://localhost:5173`. Secure/HttpOnly/SameSite=Strict cookies remain enabled.

Keep passwords, JWT/invitation secrets, real `.env`/`flyway.conf`, private keys, CA keys and database files outside Git. `.env.example` is a template: Spring Boot does not automatically load a repository `.env` file. No frontend Supabase key or migration password is required. Each developer must establish their own trusted local CA; do not share CA private keys.

## Legacy MySQL/XAMPP setup

The default profile still accepts `DB_URL`, `DB_USER` and `DB_PASSWORD`, and `docs/schema.mysql.sql` remains available for a reviewed fresh database. Existing XAMPP users may continue their established setup; this integration does not establish that the legacy schema/import or Compose stack currently works. Do not import that schema into Supabase or an existing database blindly. Supply process environment variables explicitly rather than relying on automatic `.env` loading.

## Verification and limits

```powershell
# Backend directory: local tests/build, no Supabase connection
mvn clean package
# Requires local Docker; uses a disposable PostgreSQL container
mvn -Ppostgres-tests test
# Frontend directory
npm run build
# With the local certificate environment configured as documented:
node scripts/check-local-https.mjs
```

The existing workflows exercise registration/login, classroom enrollment, assessment submission/scoring and related reports against isolated test databases. They were already present in `dev`; this change adds PostgreSQL execution and configuration support. Test results and limitations are documented in the PR and runbook. Passing tests do not establish every remote browser workflow, every incorrect-answer recommendation scenario, or production readiness. No public deployment is claimed.

---
## Git Workflow & Conventions

* **`main`**: Stable, demo-ready code only (Protected; requires PR & review).
* **`dev`**: Integration branch for ongoing development.
* **`feature/<issue-number>-<name>`**: One branch per task card, merged into dev/main via pull request.
* **`fix/<issue-number>-<name>`**: Bug fix branches.

---

## Project Links & Documentation

* **Trello Board:** [GapMap Trello Board Workspace](https://trello.com/invite/b/6adb4b9b0c91bfe81bc90152/ATTI383569daa0584c8bbadb001c72765421E4042A28/gapmap)
* **Feature Requirements:** `docs/requirements.md`
* **System Architecture:** `docs/architecture.md`
* **Team Roles & RACI:** `docs/team-roles.md`
* **Contribution Guidelines:** `CONTRIBUTING.md`
