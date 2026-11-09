package com.csit321g1.thebackurners.entity;

import jakarta.persistence.*;
import java.util.*;

@Entity
@Table(name = "users")
public class User {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(nullable = false)
  public String name;

  @Column(nullable = false, unique = true)
  public String email;

  @Column(nullable = false)
  public String passwordHash;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  public Role role;

  /** Bumped on password reset; session tokens carrying an older value are rejected. */
  public Integer tokenVersion;

  public int version() {
    return tokenVersion == null ? 0 : tokenVersion;
  }
}
