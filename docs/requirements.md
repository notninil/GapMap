# GapMap Requirements & Feature Backlog

## Problem Statement
Students often struggle to identify specific knowledge gaps after completing assessments, while teachers lack streamlined real-time visibility into classroom-wide struggle areas. **GapMap** bridges this disconnect by delivering interactive assessments, instant automated scoring, targeted learning gap detection, and curated material recommendations.

## Target Users
* **Students:** Enrolls in classrooms, takes mini-quizzes, views test results, and reviews recommended learning materials mapped to their weak areas.
* **Teachers:** Creates classrooms, generates student join codes, designs assessments/quizzes, and monitors aggregated class performance.

## MVP Scope Definition
The initial Minimum Viable Product (MVP) scope includes core authentication, classroom enrollment via join codes, mini-quiz execution, automated gap detection, and seed data initialization for instant testing.

---

## Epics & Feature Backlog

### Epic 1: Authentication & User Management (AUTH)
* **AUTH-01: User Registration & Role Selection**
  * **User Story:** As a user, I want to register as a Student or Teacher so that I can access role-specific dashboard tools.
  * **Priority:** Must Have (MVP)
  * **Dependencies:** None
  * **Acceptance Criteria:**
    * Validates unique email formats and enforces password complexity.
    * Requires a valid teacher invitation code (`GAPMAP-TEACHER`) for Teacher accounts.
    * Saves user account to MySQL `users` table upon submission.

* **AUTH-02: JWT Authentication & Session Control**
  * **User Story:** As a user, I want to log in securely with JWT tokens so my session remains authenticated across client views.
  * **Priority:** Must Have (MVP)
  * **Dependencies:** AUTH-01
  * **Acceptance Criteria:**
    * Returns a valid JWT signed with `app.jwt-secret` upon login.
    * Restricts protected API routes (`/api/*`) for unauthenticated requests.

* **AUTH-03: Password Reset via Token**
  * **User Story:** As a user, I want to request a password reset link so that I can recover lost access to my account.
  * **Priority:** Should Have
  * **Dependencies:** AUTH-01
  * **Acceptance Criteria:**
    * Generates a 30-minute expiration reset token upon request.
    * Writes reset links to backend logs when mail server is unbundled (`RESET_LINKS_IN_LOG=true`).

---

### Epic 2: Classroom & Enrollment Management (CLASS)
* **CLASS-01: Classroom Creation & Join Code Generation**
  * **User Story:** As a Teacher, I want to create a classroom that automatically generates a unique join code so students can enroll easily.
  * **Priority:** Must Have (MVP)
  * **Dependencies:** AUTH-01
  * **Acceptance Criteria:**
    * Generates a unique 6-character alphanumeric `join_code` upon classroom creation.
    * Displays created classrooms on the teacher dashboard.

* **CLASS-02: Student Classroom Enrollment**
  * **User Story:** As a Student, I want to enter a join code so that I can join a teacher's classroom.
  * **Priority:** Must Have (MVP)
  * **Dependencies:** CLASS-01
  * **Acceptance Criteria:**
    * Validates the input `join_code` against active classrooms.
    * Creates a row in `class_room_enrollments` linking student ID to classroom ID.
    * Prevents duplicate enrollment attempts.

---

### Epic 3: Assessment & Quiz System (QUIZ)
* **QUIZ-01: Mini-Quiz & Assessment Retrieval**
  * **User Story:** As a Student, I want to view assigned quizzes and assessments so that I can practice core learning topics.
  * **Priority:** Must Have (MVP)
  * **Dependencies:** CLASS-02
  * **Acceptance Criteria:**
    * Fetches published assessments attached to enrolled classrooms.
    * Displays questions alongside multiple-choice option selections.

* **QUIZ-02: Assessment Submission & Automated Scoring**
  * **User Story:** As a Student, I want to submit my quiz responses so that my score is instantly calculated and recorded.
  * **Priority:** Must Have (MVP)
  * **Dependencies:** QUIZ-01
  * **Acceptance Criteria:**
    * Evaluates student answers against correct option IDs.
    * Calculates percentage score and creates an `attempts` table record.

* **QUIZ-03: Assessment Creation & Topic Assignment**
  * **User Story:** As a Teacher, I want to create mini-quizzes and assign them to specific subject topics.
  * **Priority:** Should Have
  * **Dependencies:** CLASS-01, QUIZ-01
  * **Acceptance Criteria:**
    * Saves new assessments into the `assessments` table linked to `teacher_id`.
    * Allows setting pass/fail thresholds and question option weights.

---

### Epic 4: Learning Gap Analysis & Analytics (ANALYTICS)
* **ANALYTICS-01: Learning Gap Identification**
  * **User Story:** As a Student, I want to see identified learning gaps after completing a quiz so I know which concepts need review.
  * **Priority:** Must Have (MVP)
  * **Dependencies:** QUIZ-02
  * **Acceptance Criteria:**
    * Flags topics with score percentages below target threshold (`app.mastery.developing=50`) as learning gaps.
    * Saves identified gap records into `learning_gaps`.

* **ANALYTICS-02: Teacher Class Performance Overview**
  * **User Story:** As a Teacher, I want to view aggregate performance metrics across my class so I can identify common student gaps.
  * **Priority:** Should Have
  * **Dependencies:** ANALYTICS-01
  * **Acceptance Criteria:**
    * Displays overall score averages, student completion rates, and top struggle areas.
    * Provides individual student drill-down views.

---

### Epic 5: Learning Materials & Recommendations (RECS)
* **RECS-01: Material Mapping & Resource Recommendations**
  * **User Story:** As a Student, I want personalized study material links recommended based on my identified gaps.
  * **Priority:** Should Have
  * **Dependencies:** ANALYTICS-01
  * **Acceptance Criteria:**
    * Maps topic IDs in `learning_gaps` to associated `learning_materials` rows.
    * Displays recommended study links directly on the student dashboard.

* **RECS-02: Seed Data Auto-Initialization**
  * **User Story:** As a Developer, I want initial demo quizzes, users, and topics seeded automatically when `app.seed=true` so the app is instantly testable.
  * **Priority:** Must Have (MVP)
  * **Dependencies:** AUTH-01, QUIZ-01
  * **Acceptance Criteria:**
    * Triggers `SeedData.java` on application boot whenever `userRepository.count() == 0`.
    * Generates default demo accounts, assessments, and question options.

* **RECS-03: Topic Prerequisite Mapping**
  * **User Story:** As a System, I want to enforce prerequisite topic links so learning paths follow a logical sequence.
  * **Priority:** Could Have
  * **Dependencies:** QUIZ-03
  * **Acceptance Criteria:**
    * Populates `topic_prerequisites` table mapping parent topics to dependent topics.