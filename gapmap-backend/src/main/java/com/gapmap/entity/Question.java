package com.gapmap.entity;

import jakarta.persistence.*;
import java.util.*;

@Entity
@Table(name = "questions")
public class Question {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @ManyToOne(optional = false)
  public Assessment assessment;

  @ManyToOne(optional = false)
  public Topic topic;

  @Enumerated(EnumType.STRING)
  public Difficulty difficulty;

  @Column(length = 4000)
  public String text;

  @Column(length = 4000)
  public String explanation;

  @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("id")
  public List<QuestionOption> options = new ArrayList<>();
}
