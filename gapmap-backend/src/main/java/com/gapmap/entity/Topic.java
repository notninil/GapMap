package com.gapmap.entity;

import jakarta.persistence.*;
import java.util.*;

@Entity
@Table(name = "topics")
public class Topic {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  public String name;

  @Column(length = 4000)
  public String description;

  @ManyToOne(optional = false)
  public User teacher;

  @ManyToMany
  @JoinTable(
      name = "topic_prerequisites",
      joinColumns = @JoinColumn(name = "topic_id"),
      inverseJoinColumns = @JoinColumn(name = "prerequisite_id"))
  public Set<Topic> prerequisites = new LinkedHashSet<>();
}
