package com.gapmap.entity;

import jakarta.persistence.*;
import java.util.*;

@Entity
@Table(name = "learning_materials")
public class LearningMaterial {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @ManyToOne(optional = false)
  public Topic topic;

  public String title;

  @Enumerated(EnumType.STRING)
  public MaterialType type;

  @Column(length = 12000)
  public String urlOrContent;

  public boolean archived;
}
