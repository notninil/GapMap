package com.csit321g1.thebackburners.gapmap.config;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class SupabaseConfigurationTest {
  MockEnvironment settings() {
    return new MockEnvironment()
        .withProperty("SUPABASE_DB_URL", "jdbc:postgresql://localhost/test?sslmode=verify-full")
        .withProperty("SUPABASE_DB_USER", "runtime")
        .withProperty("SUPABASE_DB_PASSWORD", "test-only")
        .withProperty("JWT_SECRET", "test-only-secret-at-least-32-characters")
        .withProperty("TEACHER_INVITE_CODE", "test-only")
        .withProperty("FRONTEND_ORIGIN", "https://example.invalid");
  }

  @Test void acceptsCompleteSettings() {
    assertDoesNotThrow(() -> SupabaseConfiguration.validate(settings()));
  }
  @Test void enabledMigrationsRequireSeparateCredentials() {
    var env = settings().withProperty("spring.flyway.enabled", "true");
    assertEquals("Required setting is missing: SUPABASE_MIGRATION_USER",
        assertThrows(IllegalStateException.class, () -> SupabaseConfiguration.validate(env)).getMessage());
    env.withProperty("SUPABASE_MIGRATION_USER", "migrator");
    assertEquals("Required setting is missing: SUPABASE_MIGRATION_PASSWORD",
        assertThrows(IllegalStateException.class, () -> SupabaseConfiguration.validate(env)).getMessage());
    env.withProperty("SUPABASE_MIGRATION_PASSWORD", "test-only");
    assertDoesNotThrow(() -> SupabaseConfiguration.validate(env));
  }
  @Test void rejectsMissingSecretsWithoutPrintingValues() {
    var env = settings().withProperty("SUPABASE_DB_PASSWORD", " ");
    assertEquals("Required setting is missing: SUPABASE_DB_PASSWORD",
        assertThrows(IllegalStateException.class, () -> SupabaseConfiguration.validate(env)).getMessage());
  }
  @Test void rejectsUnverifiedTlsAndSharedLogin() {
    var env = settings().withProperty("SUPABASE_DB_URL", "jdbc:postgresql://localhost/test?sslmode=require");
    assertThrows(IllegalStateException.class, () -> SupabaseConfiguration.validate(env));
    var sameLogin = settings().withProperty("spring.flyway.enabled", "true")
        .withProperty("SUPABASE_MIGRATION_USER", "runtime")
        .withProperty("SUPABASE_MIGRATION_PASSWORD", "test-only");
    assertThrows(IllegalStateException.class, () -> SupabaseConfiguration.validate(sameLogin));
  }
  @Test void rejectsDemoProfileAndShortJwtSecret() {
    var env = settings();
    env.setActiveProfiles("demo", "supabase");
    assertThrows(IllegalStateException.class, () -> SupabaseConfiguration.validate(env));
    assertThrows(IllegalStateException.class, () -> SupabaseConfiguration.validate(
        settings().withProperty("JWT_SECRET", "short")));
  }
}
