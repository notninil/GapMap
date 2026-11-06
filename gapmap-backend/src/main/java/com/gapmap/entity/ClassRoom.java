package com.gapmap.entity;

import jakarta.persistence.*;
import java.util.*;

@Entity
@Table(name = "class_rooms")
public class ClassRoom {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  public String name;

  @ManyToOne(optional = false)
  public User teacher;

  @Column(unique = true, nullable = false)
  public String joinCode;

  @ManyToMany
  @JoinTable(
      name = "enrollments",
      joinColumns = @JoinColumn(name = "class_room_id"),
      inverseJoinColumns = @JoinColumn(name = "student_id"),
      uniqueConstraints = @UniqueConstraint(columnNames = {"class_room_id", "student_id"}))
  public Set<User> students = new LinkedHashSet<>();
}
