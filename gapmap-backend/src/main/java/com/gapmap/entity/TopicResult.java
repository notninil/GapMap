package com.gapmap.entity;

import jakarta.persistence.*;
import java.util.*;

@Entity
@Table(
    name = "topic_results",
    uniqueConstraints = @UniqueConstraint(columnNames = {"attempt_id", "topic_id"}))
public class TopicResult {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @ManyToOne(optional = false)
  public Attempt attempt;

  @ManyToOne(optional = false)
  public Topic topic;

  public double scorePercent;

  @Enumerated(EnumType.STRING)
  public MasteryLevel masteryLevel;
}
