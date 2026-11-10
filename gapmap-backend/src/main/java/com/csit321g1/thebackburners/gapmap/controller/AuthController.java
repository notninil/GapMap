package com.csit321g1.thebackburners.gapmap.controller;

import jakarta.validation.Valid;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import com.csit321g1.thebackburners.gapmap.dto.Dtos.*;
import com.csit321g1.thebackburners.gapmap.entity.User;
import com.csit321g1.thebackburners.gapmap.service.*;

@RestController
@RequestMapping("/api")
public class AuthController {
  private final AuthService auth;
  private final AccessService access;

  @Value("${app.cookie-secure}")
  boolean secure;

  @Value("${app.demo-login:false}")
  boolean demoLogin;

  private final com.csit321g1.thebackburners.gapmap.repository.UserRepository users;

  public AuthController(AuthService a, AccessService s, com.csit321g1.thebackburners.gapmap.repository.UserRepository u) {
    auth = a;
    access = s;
    users = u;
  }

  private ResponseEntity<UserView> signed(User u) {
    return ResponseEntity.ok()
        .header(
            HttpHeaders.SET_COOKIE,
            ResponseCookie.from("gapmap_session", auth.token(u))
                .httpOnly(true)
                .secure(secure)
                .sameSite("Strict")
                .path("/api")
                .maxAge(Duration.ofHours(8))
                .build()
                .toString())
        .body(AuthService.view(u));
  }

  @PostMapping("/auth/login")
  public ResponseEntity<UserView> login(@Valid @RequestBody Login d) {
    return signed(auth.login(d));
  }

  @PostMapping("/auth/register")
  public ResponseEntity<UserView> register(@Valid @RequestBody Register d) {
    return signed(auth.register(d));
  }

  @PostMapping("/auth/forgot-password")
  public java.util.Map<String, Object> forgot(@Valid @RequestBody ForgotPassword d) {
    var link = auth.requestReset(d);
    var body = new java.util.LinkedHashMap<String, Object>();
    body.put(
        "message",
        "If an account exists for that email, a password reset link has been prepared.");
    // Demo profile only: there is no mail server, so show the link to make the flow testable.
    if (demoLogin) link.ifPresent(l -> body.put("demoResetLink", l));
    return body;
  }

  @PostMapping("/auth/reset-password")
  public java.util.Map<String, String> reset(@Valid @RequestBody ResetPassword d) {
    auth.resetPassword(d);
    return java.util.Map.of("message", "Your password has been updated. Please sign in.");
  }

  @GetMapping("/auth/me")
  public UserView me() {
    return AuthService.view(access.current());
  }

  @PostMapping("/auth/logout")
  public ResponseEntity<Void> logout() {
    return ResponseEntity.noContent()
        .header(
            HttpHeaders.SET_COOKIE,
            ResponseCookie.from("gapmap_session", "")
                .httpOnly(true)
                .secure(secure)
                .sameSite("Strict")
                .path("/api")
                .maxAge(0)
                .build()
                .toString())
        .build();
  }

  @PostMapping("/auth/demo")
  public ResponseEntity<UserView> demo(@RequestBody java.util.Map<String, String> d) {
    if (!demoLogin)
      throw new org.springframework.web.server.ResponseStatusException(
          HttpStatus.NOT_FOUND, "Demo sign-in is disabled");
    String email = "TEACHER".equals(d.get("role")) ? "teacher@gapmap.test" : "student1@gapmap.test";
    return signed(AccessService.found(users.findByEmail(email)));
  }

  @GetMapping("/health")
  public java.util.Map<String, Object> health() {
    return java.util.Map.of("status", "ok", "demoLogin", demoLogin);
  }
}
