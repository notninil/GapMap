package com.gapmap.entity;

import jakarta.persistence.*;
import java.util.*;

@Entity
@Table(name = "learning_gaps")
public class LearningGap {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @ManyToOne(optional = false)
  public User student;

  @ManyToOne(optional = false)
  public Topic topic;

  @ManyToOne(optional = false)
  public Attempt attempt;

  @Enumerated(EnumType.STRING)
  public GapType gapType;

  @Enumerated(EnumType.STRING)
  public Status status = Status.PENDING;
}
