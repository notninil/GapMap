package com.gapmap.controller;

import com.gapmap.dto.Dtos.*;
import com.gapmap.service.*;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class CatalogController {
  final CatalogService s;

  public CatalogController(CatalogService s) {
    this.s = s;
  }

  @GetMapping("/classes")
  public List<ClassView> classes() {
    return s.classes();
  }

  @PostMapping("/classes")
  @PreAuthorize("hasRole('TEACHER')")
  public ClassView createClass(@Valid @RequestBody ClassInput d) {
    return s.createClass(d);
  }

  @PostMapping("/classes/join")
  @PreAuthorize("hasRole('STUDENT')")
  public ClassView join(@Valid @RequestBody JoinInput d) {
    return s.join(d);
  }

  @GetMapping("/topics")
  public List<TopicView> topics() {
    return s.topics();
  }

  @PostMapping("/topics")
  @PreAuthorize("hasRole('TEACHER')")
  public TopicView topic(@Valid @RequestBody TopicInput d) {
    return s.saveTopic(null, d);
  }

  @PutMapping("/topics/{id}")
  @PreAuthorize("hasRole('TEACHER')")
  public TopicView topic(@PathVariable Long id, @Valid @RequestBody TopicInput d) {
    return s.saveTopic(id, d);
  }

  @DeleteMapping("/topics/{id}")
  @PreAuthorize("hasRole('TEACHER')")
  public void deleteTopic(@PathVariable Long id) {
    s.deleteTopic(id);
  }

  @GetMapping("/assessments")
  public List<AssessmentView> assessments() {
    return s.assessments();
  }

  @GetMapping("/assessments/{id}")
  public AssessmentView assessment(@PathVariable Long id) {
    return s.assessment(id);
  }

  @GetMapping("/assessments/{id}/questions")
  public List<QuestionView> questions(@PathVariable Long id) {
    return s.assessment(id).questions();
  }

  @PostMapping("/assessments")
  @PreAuthorize("hasRole('TEACHER')")
  public AssessmentView assessment(@Valid @RequestBody AssessmentInput d) {
    return s.saveAssessment(null, d);
  }

  @PutMapping("/assessments/{id}")
  @PreAuthorize("hasRole('TEACHER')")
  public AssessmentView assessment(@PathVariable Long id, @Valid @RequestBody AssessmentInput d) {
    return s.saveAssessment(id, d);
  }

  @DeleteMapping("/assessments/{id}")
  @PreAuthorize("hasRole('TEACHER')")
  public void deleteAssessment(@PathVariable Long id) {
    s.deleteAssessment(id);
  }

  @GetMapping("/materials")
  public List<MaterialView> materials() {
    return s.materials();
  }

  @PostMapping("/materials")
  @PreAuthorize("hasRole('TEACHER')")
  public MaterialView material(@Valid @RequestBody MaterialInput d) {
    return s.saveMaterial(null, d);
  }

  @PutMapping("/materials/{id}")
  @PreAuthorize("hasRole('TEACHER')")
  public MaterialView material(@PathVariable Long id, @Valid @RequestBody MaterialInput d) {
    return s.saveMaterial(id, d);
  }

  @DeleteMapping("/materials/{id}")
  @PreAuthorize("hasRole('TEACHER')")
  public void deleteMaterial(@PathVariable Long id) {
    s.deleteMaterial(id);
  }
}
