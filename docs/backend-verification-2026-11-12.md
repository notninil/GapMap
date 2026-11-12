# Backend verification — 2026-11-12

## Environment and compilation

- Apache Maven 3.10.0, Maven home `C:\Program Files\Apache\Maven`.
- Oracle JDK 25 (build 25+37-LTS-3491), used by Maven from `C:\Program Files\Java\jdk-25`.
- Spring Boot remains 3.4.5; Java target remains 17. No upgrades or configuration edits.
- `mvn clean compile`: BUILD SUCCESS, 50 source files compiled using release 17.
- `javap` confirms class major version 61 (Java 17).
- Initial sandbox execution could not access `C:\.m2\repository`; approved execution outside the sandbox succeeded.
- Java 17 runtime execution was not tested: JDK 17 is not installed in the inspected Java installation directory. JDK 25 passed these checks, but lies beyond the documented Java 24 upper compatibility limit of the Spring Boot 3.4 line. Use JDK 17 for a supported baseline before deployment.

## Existing automated tests

`mvn -B test`: BUILD SUCCESS.

| Suite | Passed | Failed | Errored | Skipped |
| --- | ---: | ---: | ---: | ---: |
| AnalysisServiceTest | 4 | 0 | 0 | 0 |
| MisconceptionServiceTest | 2 | 0 | 0 | 0 |
| PrerequisiteServiceTest | 6 | 0 | 0 | 0 |
| StudyPathServiceTest | 3 | 0 | 0 | 0 |
| WorkflowIntegrationTest | 11 | 0 | 0 | 0 |
| Total | 26 | 0 | 0 | 0 |

Surefire evidence is in `gapmap-backend/target/surefire-reports/`. Workflow tests use isolated in-memory H2 with create-drop; they do not use the demo file database. No tests were disabled or removed.

## Local demo verification

- Inspected demo configuration: file-backed H2 in MySQL compatibility mode; inherited Hibernate `ddl-auto=update`.
- Copied both existing H2 database files before startup to `verification-backups/20261112-023418/`. Database backups are ignored by existing Git patterns and must remain local.
- Started with `mvn -B spring-boot:run`, demo profile, explicitly bound to `127.0.0.1:18080` and the local H2 URL, username and empty password. Disabled reset-link logging for this run. No production database was used.
- Startup log confirmed active demo profile, H2 connection, and `Started GapMapApplication in 3.818 seconds`.

| Request | Result |
| --- | --- |
| GET /api/health | 200; status=ok, demoLogin=true |
| GET /api/auth/me without cookie | 401 |
| POST /api/auth/demo with allowed Origin, student role | 200; session cookie retained only in memory |
| GET /api/auth/me with cookie | 200; STUDENT role |
| GET /api/dashboards/student with cookie | 200 |
| POST /api/auth/logout with allowed Origin | 204 |

Live authentication used the existing demo sign-in route. Password-based login is exercised by the passing workflow integration suite. No credentials or session cookie values are included in this report.

Stopped the verification server and confirmed port 18080 no longer responds. H2 files were updated during normal startup/use; original copies remain in the backup directory. No data deletion or restoration was performed.

## Changes, limitations, and readiness

- No source, POM, authentication, or application configuration changes were needed.
- Added this report; generated build/test artifacts under target and local database backups. Existing H2 files changed during normal operation.
- The inherited environment has DEBUG enabled, producing verbose Spring request logs, including synthetic test login data. Avoid enabling that logging with real credentials. No real credential login was performed in the live check.
- README demo account addresses differ from the seeded addresses; live verification followed the seed/controller implementation. Documentation was left unchanged during this verification phase.
- Compilation, existing tests, startup, and checked APIs pass. This establishes the H2 baseline for the next migration phase; PostgreSQL compatibility and migration correctness remain unverified. Resolve the runtime JDK support caveat before deployment.
- No Supabase migration, production service connection, commits, pushes, or branch changes were performed.

Reference: https://docs.spring.io/spring-boot/3.4/system-requirements.html (Spring Boot 3.4 line documents Java 17 through 24).
