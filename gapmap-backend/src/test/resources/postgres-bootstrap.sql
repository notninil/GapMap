-- Disposable Testcontainers database only. Never run against a real deployment.
CREATE ROLE gapmap_runtime NOLOGIN;
CREATE ROLE gapmap_test_migrator LOGIN PASSWORD 'migration-test-only';
CREATE ROLE gapmap_test_app LOGIN PASSWORD 'runtime-test-only';
GRANT gapmap_runtime TO gapmap_test_app;
CREATE SCHEMA gapmap AUTHORIZATION gapmap_test_migrator;
