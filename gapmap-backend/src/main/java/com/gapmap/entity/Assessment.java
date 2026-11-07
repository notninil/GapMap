package com.gapmap.entity;

import jakarta.persistence.*;
import java.util.*;

@Entity
@Table(name = "assessments")
public class Assessment {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  public String title;

  @ManyToOne(optional = false)
  public ClassRoom classRoom;

  @ManyToOne(optional = false)
  public User createdBy;

  public boolean miniQuiz;
  public boolean published;
  public boolean archived;
  @ManyToOne public Topic topic;
  @Version public long version;

  @OneToMany(mappedBy = "assessment", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("id")
  public List<Question> questions = new ArrayList<>();
}
