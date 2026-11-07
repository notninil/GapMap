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
* **Backend:** Java 25, Spring Boot 3.4.5, Spring Security, Spring Data JPA, JWT Auth (`http://localhost:8080`)
* **Frontend:** React 18, Vite, Tailwind CSS, Axios, Recharts (`http://localhost:5173`)
* **Database:** MySQL 5.7/8.0 via XAMPP Control Panel (`gapmap_local` on Port 3306)

---

## Project Structure
```text
GapMap/
├── docs/                       # Requirements, Architecture, Team Roles, & MySQL Schema
│   ├── requirements.md
│   ├── architecture.md
│   ├── team-roles.md
│   └── schema.mysql.sql
├── gapmap-backend/             # Spring Boot REST API source code
├── gapmap-frontend/            # React + Vite application source code
├── .env.example                # Local environment variables template
├── .gitignore
├── CONTRIBUTING.md             # Development guidelines & PR process
└── README.md                   # Main project overview & setup guide
```

## How to Run Locally

### Prerequisites
1. Install **XAMPP Control Panel** (Apache & MySQL).
2. Install **Java JDK 17+** (or Java 25).
3. Install **Node.js** (v18+) and **npm**.

---

### Step 1: Database Setup (XAMPP)
1. Open XAMPP Control Panel and start **Apache** and **MySQL**.
2. Open phpMyAdmin in your browser at `http://localhost/phpmyadmin`
3. Create a new database named **`gapmap_local`**.
4. Select `gapmap_local`, click the **Import** tab, choose `docs/schema.mysql.sql` from the repository, and click **Go**.

---

### Step 2: Environment Configuration
Copy `.env.example` to `.env` in the root project folder:
```cmd
cp .env.example .env 
```
Ensure DB_USER=root and DB_PASSWORD= match your local XAMPP MySQL defaults.

---

### Step 3: Start the Backend (Spring Boot)
1. Open a terminal and navigate to the backend directory:
```cmd
cd gapmap-backend
```
2. Run the application:
```cmd
mvn spring-boot:run
```
3. The server will start on http://localhost:8080. Seeding will execute automatically if the database is fresh.

---

### Step 4: Start the Frontend (React / Vite)
1. Open a second terminal and navigate to the frontend directory:
```cmd
cd gapmap-frontend
```
2. Install node dependencies (first time only):
```cmd
npm install
```
3. Start the Vite development server:
```cmd
npm run dev
```
4. Access the application in your browser at http://localhost:5173.

---

## Demo Credentials & Seed Data

When `SEED_DATA=true` is enabled on initial boot, log in using these default accounts:

* **Teacher Account:** `teacher@gapmap.demo` / `GapMap123!`
* **Student Account:** `student@gapmap.demo` / `GapMap123!`
* **Teacher Invitation Code (for new registrations):** `GAPMAP-TEACHER`

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
