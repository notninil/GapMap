package com.gapmap.service;

import com.gapmap.dto.Dtos.*;
import com.gapmap.entity.*;
import com.gapmap.repository.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
  private final UserRepository users;
  private final PasswordEncoder passwords;
  private final JwtEncoder encoder;
  private final PasswordResetTokenRepository resets;
  private static final Logger log = LoggerFactory.getLogger(AuthService.class);
  private static final SecureRandom RANDOM = new SecureRandom();

  @Value("${app.teacher-code}")
  String teacherCode;

  @Value("${app.frontend-origin:http://localhost:5173}")
  String frontendOrigin;

  @Value("${app.reset-token-minutes:30}")
  long resetMinutes;

  @Value("${app.reset-links-in-log:false}")
  boolean logResetLinks;

  public AuthService(
      UserRepository u, PasswordEncoder p, JwtEncoder e, PasswordResetTokenRepository r) {
    users = u;
    passwords = p;
    encoder = e;
    resets = r;
  }

  @Transactional
  public User register(Register d) {
    AccessService.require(
        d.password().getBytes(java.nio.charset.StandardCharsets.UTF_8).length <= 72,
        "Password must be at most 72 UTF-8 bytes");
    AccessService.require(
        d.role() != Role.TEACHER || (!teacherCode.isBlank()
                && d.teacherCode() != null
                && MessageDigest.isEqual(
                    teacherCode.getBytes(StandardCharsets.UTF_8),
                    d.teacherCode().getBytes(StandardCharsets.UTF_8))),
        "A valid teacher invitation code is required");
    String email = d.email().trim().toLowerCase(Locale.ROOT);
    AccessService.require(users.findByEmail(email).isEmpty(), "Email already registered");
    User u = new User();
    u.name = d.name().trim();
    u.email = email;
    u.role = d.role();
    u.passwordHash = passwords.encode(d.password());
    return users.save(u);
  }

  public User login(Login d) {
    var u = users.findByEmail(d.email().trim().toLowerCase(Locale.ROOT));
    if (u.isEmpty() || !passwords.matches(d.password(), u.get().passwordHash))
      throw new org.springframework.web.server.ResponseStatusException(
          org.springframework.http.HttpStatus.UNAUTHORIZED, "Invalid email or password");
    return u.get();
  }

  public String token(User u) {
    return encoder
        .encode(
            JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(),
                JwtClaimsSet.builder()
                    .subject(u.id.toString())
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(28800))
                    .claim("role", u.role.name())
                    .claim("tv", u.version())
                    .build()))
        .getTokenValue();
  }

  public static UserView view(User u) {
    return new UserView(u.id, u.name, u.email, u.role);
  }

  static String hash(String token) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
    } catch (java.security.NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * Starts a password reset. Never reveals whether the email exists: callers always show the same
   * message. Returns the reset link only when an account was found and a token was issued, so the
   * demo profile can show it; production delivery is the link written to the log (see README) or
   * an email integration plugged in here.
   */
  @Transactional
  public Optional<String> requestReset(ForgotPassword d) {
    Instant now = Instant.now();
    resets.deleteExpiredBefore(now.minus(1, ChronoUnit.DAYS));
    var found = users.findByEmail(d.email().trim().toLowerCase(Locale.ROOT));
    if (found.isEmpty()) return Optional.empty();
    User u = found.get();
    if (resets.countByUserAndCreatedAtAfter(u, now.minus(1, ChronoUnit.HOURS)) >= 3)
      return Optional.empty();
    resets.invalidateOutstanding(u, now);
    byte[] raw = new byte[32];
    RANDOM.nextBytes(raw);
    String token = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
    var t = new PasswordResetToken();
    t.user = u;
    t.tokenHash = hash(token);
    t.createdAt = now;
    t.expiresAt = now.plus(resetMinutes, ChronoUnit.MINUTES);
    resets.save(t);
    String link = frontendOrigin + "/reset-password?token=" + token;
    if (logResetLinks) log.info("Password reset link for {} (valid {} min): {}", u.email, resetMinutes, link);
    return Optional.of(link);
  }

  @Transactional
  public void resetPassword(ResetPassword d) {
    AccessService.require(
        d.password().getBytes(StandardCharsets.UTF_8).length <= 72,
        "Password must be at most 72 UTF-8 bytes");
    Instant now = Instant.now();
    var t =
        resets
            .lockByTokenHash(hash(d.token().trim()))
            .filter(x -> x.usedAt == null && x.expiresAt.isAfter(now))
            .orElseThrow(
                () ->
                    new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.BAD_REQUEST,
                        "This reset link is invalid or has expired. Please request a new one."));
    User u = t.user;
    u.passwordHash = passwords.encode(d.password());
    u.tokenVersion = u.version() + 1; // signs out every existing session
    users.save(u);
    resets.invalidateOutstanding(u, now);
  }
}
