package com.csit321g1.thebackurners.entity;

import jakarta.persistence.*;
import java.util.*;

@Entity
@Table(name = "question_options")
public class QuestionOption {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @ManyToOne(optional = false)
  public Question question;

  @Column(length = 2000)
  public String text;

  public boolean correct;
}
