package com.gapmap.entity;

import jakarta.persistence.*;
import java.util.*;

@Entity
@Table(
    name = "responses",
    uniqueConstraints = @UniqueConstraint(columnNames = {"attempt_id", "question_id"}))
public class Response {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @ManyToOne(optional = false)
  public Attempt attempt;

  @ManyToOne(optional = false)
  public Question question;

  @ManyToOne(optional = false)
  public QuestionOption selectedOption;

  public boolean correct;

  @Enumerated(EnumType.STRING)
  public ConfidenceLevel confidenceLevel;
}
