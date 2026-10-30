package com.gapmap.entity;

import jakarta.persistence.*;
import java.util.*;

@Entity
@Table(name = "recommendations")
public class Recommendation {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @ManyToOne(optional = false)
  public User student;

  @ManyToOne(optional = false)
  public LearningGap gap;

  @ManyToOne(optional = false)
  public LearningMaterial material;

  @Column(length = 4000)
  public String explanation;

  public int orderIndex;

  @Enumerated(EnumType.STRING)
  public Status status = Status.PENDING;
}
