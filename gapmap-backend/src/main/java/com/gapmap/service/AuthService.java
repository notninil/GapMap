package com.gapmap.service;

import com.gapmap.dto.Dtos.*;
import com.gapmap.entity.*;
import com.gapmap.repository.*;
import java.time.Instant;
import java.util.Locale;
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

  @Value("${app.teacher-code}")
  String teacherCode;

  public AuthService(UserRepository u, PasswordEncoder p, JwtEncoder e) {
    users = u;
    passwords = p;
    encoder = e;
  }

  @Transactional
  public User register(Register d) {
    AccessService.require(
        d.password().getBytes(java.nio.charset.StandardCharsets.UTF_8).length <= 72,
        "Password must be at most 72 UTF-8 bytes");
    AccessService.require(
        d.role() != Role.TEACHER || (!teacherCode.isBlank() && teacherCode.equals(d.teacherCode())),
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
                    .build()))
        .getTokenValue();
  }

  public static UserView view(User u) {
    return new UserView(u.id, u.name, u.email, u.role);
  }
}
