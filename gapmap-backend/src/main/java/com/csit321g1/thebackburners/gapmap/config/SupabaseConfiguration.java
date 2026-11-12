package com.csit321g1.thebackburners.gapmap.config;

import java.util.Arrays;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

/** Fail before database initialization for unsafe or incomplete remote-profile settings. */
@Configuration
@Profile("supabase")
public class SupabaseConfiguration {
  static void validate(Environment env) {
    if (Arrays.asList(env.getActiveProfiles()).contains("demo"))
      throw new IllegalStateException("Do not combine supabase and demo profiles");
    for (String name : new String[] {"SUPABASE_DB_URL", "SUPABASE_DB_USER",
        "SUPABASE_DB_PASSWORD",
        "JWT_SECRET", "TEACHER_INVITE_CODE", "FRONTEND_ORIGIN"}) {
      if (env.getProperty(name, "").isBlank())
        throw new IllegalStateException("Required setting is missing: " + name);
    }
    String url = env.getProperty("SUPABASE_DB_URL");
    if (!url.startsWith("jdbc:postgresql://")
        || !url.matches(".*[?&]sslmode=verify-full(?:&.*)?$"))
      throw new IllegalStateException("SUPABASE_DB_URL must use PostgreSQL with sslmode=verify-full");
    if (env.getProperty("JWT_SECRET").length() < 32)
      throw new IllegalStateException("JWT_SECRET must contain at least 32 characters");
    // Only the opt-in local migration tests need migration credentials.
    if (env.getProperty("spring.flyway.enabled", Boolean.class, false)) {
      for (String name : new String[] {"SUPABASE_MIGRATION_USER", "SUPABASE_MIGRATION_PASSWORD"}) {
        if (env.getProperty(name, "").isBlank())
          throw new IllegalStateException("Required setting is missing: " + name);
      }
      if (env.getProperty("SUPABASE_DB_USER").equals(env.getProperty("SUPABASE_MIGRATION_USER")))
        throw new IllegalStateException("Use separate runtime and migration logins");
    }
  }

  @Bean
  static BeanFactoryPostProcessor validateSupabaseSettings(Environment env) {
    return factory -> validate(env);
  }
}
