package com.csit321g1.thebackburners.gapmap;

import static org.junit.jupiter.api.Assertions.*;
import java.sql.DriverManager;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Reuses all 11 real workflow tests against migrated PostgreSQL with a restricted login. */
@Testcontainers
@ActiveProfiles("supabase")
@AutoConfigureMockMvc
@SpringBootTest(properties = {
    "spring.flyway.enabled=true",
    "spring.flyway.url=${SUPABASE_DB_URL}",
    "spring.flyway.user=${SUPABASE_MIGRATION_USER}",
    "spring.flyway.password=${SUPABASE_MIGRATION_PASSWORD}",
    "spring.jpa.hibernate.ddl-auto=validate", "app.seed=true",
    "app.seed-password=GapMap123!", "app.cookie-secure=false"
})
class PostgreSqlWorkflowTest extends WorkflowIntegrationTest {
  @Container
  static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
      .withInitScript("postgres-bootstrap.sql");

  @DynamicPropertySource
  static void database(DynamicPropertyRegistry r) {
    // Local disposable container has no TLS. Production profile still requires verified TLS.
    r.add("SUPABASE_DB_URL", () -> postgres.getJdbcUrl() + "?sslmode=verify-full");
    r.add("SUPABASE_DB_USER", () -> "gapmap_test_app");
    r.add("SUPABASE_DB_PASSWORD", () -> "runtime-test-only");
    r.add("SUPABASE_MIGRATION_USER", () -> "gapmap_test_migrator");
    r.add("SUPABASE_MIGRATION_PASSWORD", () -> "migration-test-only");
    r.add("JWT_SECRET", () -> "integration-test-secret-32-characters-minimum");
    r.add("TEACHER_INVITE_CODE", () -> "TEST-TEACHER");
    r.add("FRONTEND_ORIGIN", () -> "http://localhost:5173");
    r.add("spring.datasource.url", postgres::getJdbcUrl);
    r.add("spring.flyway.url", postgres::getJdbcUrl);
  }

  @Test void migrationsCoverAllTablesAndRuntimeCannotCreateOrReadHistory() throws Exception {
    try (var c = DriverManager.getConnection(postgres.getJdbcUrl(), "gapmap_test_app", "runtime-test-only");
         var s = c.createStatement()) {
      try (var rows = s.executeQuery("select count(*) from information_schema.tables where table_schema='gapmap'")) {
        assertTrue(rows.next());
        // Migration history is intentionally invisible to the runtime role.
        assertEquals(15, rows.getInt(1));
      }
      assertThrows(java.sql.SQLException.class,
          () -> s.execute("create table gapmap.forbidden(id bigint)"));
      assertThrows(java.sql.SQLException.class,
          () -> s.executeQuery("select * from gapmap.flyway_schema_history"));
    }
  }
}
