package com.gapmap.controller;

import com.gapmap.dto.Dtos.*;
import com.gapmap.service.*;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ReportController {
  final AttemptService attempts;
  final ReportService reports;
  final AccessService access;
  final StudyPathService paths;

  public ReportController(AttemptService a, ReportService r, AccessService ac, StudyPathService p) {
    attempts = a;
    reports = r;
    access = ac;
    paths = p;
  }

  @PostMapping("/attempts")
  @PreAuthorize("hasRole('STUDENT')")
  public AttemptView submit(@Valid @RequestBody Submission d) {
    return attempts.submit(d);
  }

  @GetMapping({"/attempts/{id}", "/results/{id}"})
  public AttemptView result(@PathVariable Long id) {
    return attempts.result(id);
  }

  @GetMapping({"/dashboards/student", "/progress"})
  @PreAuthorize("hasRole('STUDENT')")
  public StudentReport student() {
    return reports.report(access.current().id);
  }

  @GetMapping("/gaps")
  @PreAuthorize("hasRole('STUDENT')")
  public List<GapView> gaps() {
    return student().gaps();
  }

  @PostMapping("/recommendations/refresh")
  @PreAuthorize("hasRole('STUDENT')")
  @org.springframework.transaction.annotation.Transactional
  public List<RecommendationView> refresh() {
    var u = access.lockCurrent();
    paths.generateStudyPath(u.id);
    return reports.report(u.id).recommendations();
  }

  @GetMapping("/recommendations")
  @PreAuthorize("hasRole('STUDENT')")
  public List<RecommendationView> recommendations() {
    return student().recommendations();
  }

  public record Completion(boolean done) {}

  @PatchMapping("/recommendations/{id}")
  @PreAuthorize("hasRole('STUDENT')")
  public RecommendationView done(@PathVariable Long id, @RequestBody Completion d) {
    return reports.done(id, d.done());
  }

  @GetMapping("/dashboards/teacher")
  @PreAuthorize("hasRole('TEACHER')")
  public TeacherDashboard teacher() {
    return reports.teacher();
  }

  @GetMapping("/students/{id}/report")
  @PreAuthorize("hasRole('TEACHER')")
  public StudentReport report(@PathVariable Long id) {
    return reports.report(id);
  }
}
