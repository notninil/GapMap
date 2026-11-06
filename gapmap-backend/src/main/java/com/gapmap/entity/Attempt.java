package com.gapmap.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.*;

@Entity
@Table(
    name = "attempts",
    uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "submissionKey"}))
public class Attempt {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @ManyToOne(optional = false)
  public Assessment assessment;

  @ManyToOne(optional = false)
  public User student;

  @Column(nullable = false)
  public Instant submittedAt;

  @Column(nullable = false)
  public String submissionKey;

  @Column(nullable = false, length = 64)
  public String payloadHash;

  public double totalScore;

  @OneToMany(mappedBy = "attempt", cascade = CascadeType.ALL)
  public List<Response> responses = new ArrayList<>();

  @OneToMany(mappedBy = "attempt", cascade = CascadeType.ALL)
  public List<TopicResult> topicResults = new ArrayList<>();
}
