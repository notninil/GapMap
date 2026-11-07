# GapMap Contribution & Collaboration Guidelines

Welcome to the **GapMap** repository! To maintain clean code quality and clear traceability between project task cards and GitHub code commits, all contributors must adhere to the rules outlined below.

---

## 1. Branching Strategy

All work must be developed in dedicated short-lived branches. Direct commits to `main` are strictly forbidden.

* **Stable Main Branch:** `main` (Protected; requires PR and peer review approval)
* **Feature Branches:** `feature/<issue-number>-<short-description>`  
  * *Example:* `feature/1-user-registration`
* **Bug Fix Branches:** `fix/<issue-number>-<short-description>`  
  * *Example:* `fix/4-jwt-secret-length`

---

## 2. Commit Message Conventions

Commit messages must be descriptive and reference the relevant Trello/GitHub issue card number:

```bash
# Syntax: <type>(<scope>): <short summary> (#<issue-number>)

# Feature examples:
git commit -m "feat(auth): implement teacher invitation code validation (#1)"
git commit -m "feat(quiz): add student assessment submission endpoint (#6)"

# Fix examples:
git commit -m "fix(db): update mysql connection properties for xampp (#4)"
git commit -m "fix(security): resolve positive key length requirement for jwt encoder (#8)"