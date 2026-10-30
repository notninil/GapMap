package com.gapmap.config;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.*;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
  @Value("${app.jwt-secret}")
  String secret;

  @Value("${app.frontend-origin}")
  String origin;

  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  JwtDecoder decoder() {
    if (secret.length() < 32)
      throw new IllegalStateException("Set JWT_SECRET to at least 32 characters.");
    return NimbusJwtDecoder.withSecretKey(
            new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"))
        .macAlgorithm(MacAlgorithm.HS256)
        .build();
  }

  @Bean
  JwtEncoder encoder() {
    return new NimbusJwtEncoder(
        new com.nimbusds.jose.jwk.source.ImmutableSecret<>(
            secret.getBytes(StandardCharsets.UTF_8)));
  }

  @Bean
  SecurityFilterChain security(HttpSecurity h) throws Exception {
    var authorities = new JwtGrantedAuthoritiesConverter();
    authorities.setAuthoritiesClaimName("role");
    authorities.setAuthorityPrefix("ROLE_");
    var converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(authorities);
    h.cors(c -> c.configurationSource(cors()))
        .csrf(c -> c.disable())
        .sessionManagement(c -> c.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
    // Cookie authentication is protected by an exact Origin check on every mutation, including
    // login.
    h.addFilterBefore(
        new OncePerRequestFilter() {
          protected void doFilterInternal(
              HttpServletRequest q, HttpServletResponse s, FilterChain c)
              throws ServletException, IOException {
            if (!Set.of("GET", "HEAD", "OPTIONS").contains(q.getMethod())
                && !origin.equals(q.getHeader("Origin"))) {
              s.setStatus(403);
              s.setContentType("application/json");
              s.getWriter().write("{\"message\":\"Request origin is not allowed\"}");
              return;
            }
            c.doFilter(q, s);
          }
        },
        UsernamePasswordAuthenticationFilter.class);
    h.authorizeHttpRequests(
        a ->
            a.requestMatchers(
                    "/api/auth/login", "/api/auth/register", "/api/auth/demo", "/api/health")
                .permitAll()
                .anyRequest()
                .authenticated());
    h.oauth2ResourceServer(
        o ->
            o.bearerTokenResolver(
                    q -> {
                      if (q.getCookies() != null)
                        for (var c : q.getCookies())
                          if (c.getName().equals("gapmap_session")) return c.getValue();
                      return null;
                    })
                .jwt(t -> t.jwtAuthenticationConverter(converter))
                .authenticationEntryPoint(
                    (q, s, e) -> {
                      s.setStatus(401);
                      s.setContentType("application/json");
                      s.getWriter().write("{\"message\":\"Please sign in to continue\"}");
                    }));
    h.exceptionHandling(
        e ->
            e.accessDeniedHandler(
                (q, s, x) -> {
                  s.setStatus(403);
                  s.setContentType("application/json");
                  s.getWriter().write("{\"message\":\"Access denied\"}");
                }));
    return h.build();
  }

  @Bean
  CorsConfigurationSource cors() {
    var c = new CorsConfiguration();
    c.setAllowedOrigins(List.of(origin));
    c.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    c.setAllowedHeaders(List.of("Content-Type"));
    c.setAllowCredentials(true);
    var source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", c);
    return source;
  }
}
