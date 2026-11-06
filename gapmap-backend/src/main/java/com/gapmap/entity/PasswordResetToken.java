package com.gapmap.entity;

import jakarta.persistence.*;
import java.time.Instant;

/** Single-use password reset token. Only the SHA-256 hash of the emailed token is stored. */
@Entity
@Table(name = "password_reset_tokens")
public class PasswordResetToken {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  public User user;

  @Column(nullable = false, unique = true, length = 64)
  public String tokenHash;

  @Column(nullable = false)
  public Instant createdAt;

  @Column(nullable = false)
  public Instant expiresAt;

  public Instant usedAt;
}
