package com.gapmap.service;

import static com.gapmap.service.AccessService.*;

import com.gapmap.dto.Dtos.*;
import com.gapmap.entity.*;
import com.gapmap.repository.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ReportService {
  final AccessService access;
  final UserRepository users;
  final ClassRoomRepository classes;
  final AttemptRepository attempts;
  final PrerequisiteService prerequisites;
  final LearningGapRepository gaps;
  final RecommendationRepository recommendations;
  final AssessmentRepository assessments;

  public ReportService(
      AccessService ac,
      UserRepository u,
      ClassRoomRepository c,
      AttemptRepository a,
      PrerequisiteService p,
      LearningGapRepository g,
      RecommendationRepository r,
      AssessmentRepository as) {
    access = ac;
    users = u;
    classes = c;
    attempts = a;
    prerequisites = p;
    gaps = g;
    recommendations = r;
    assessments = as;
  }

  private boolean visible(Topic t) {
    return access.current().role == Role.STUDENT || t.teacher.id.equals(access.current().id);
  }

  public StudentReport report(Long id) {
    User viewer = access.current();
    User student = found(users.findById(id));
    require(student.role == Role.STUDENT, "Select a student");
    if (!viewer.id.equals(id)) {
      require(viewer.role == Role.TEACHER, "Access denied");
      boolean enrolled =
          classes.findByTeacherId(viewer.id).stream()
              .anyMatch(c -> c.students.stream().anyMatch(s -> s.id.equals(id)));
      if (!enrolled)
        throw new org.springframework.security.access.AccessDeniedException(
            "Student not in your classes");
    }
    return new StudentReport(
        AuthService.view(student),
        attempts.findByStudentIdOrderBySubmittedAtAscIdAsc(id).stream()
            .filter(a -> viewer.role == Role.STUDENT || a.assessment.createdBy.id.equals(viewer.id))
            .map(AttemptService::view)
            .toList(),
        prerequisites.latest(id).values().stream()
            .filter(t -> visible(t.topic))
            .map(AttemptService::topicScore)
            .toList(),
        gaps.findByStudentIdOrderByIdDesc(id).stream()
            .filter(g -> visible(g.topic) && g.status != Status.RESOLVED)
            .map(
                g -> new GapView(g.id, g.topic.id, g.topic.name, g.gapType, g.status, g.attempt.id))
            .toList(),
        recommendations.findByStudentIdOrderByOrderIndexAscIdAsc(id).stream()
            .filter(
                r ->
                    visible(r.gap.topic)
                        && r.gap.status != Status.RESOLVED
                        && r.status != Status.RESOLVED
                        && !r.material.archived)
            .map(ReportService::recommendationView)
            .toList());
  }

  public static RecommendationView recommendationView(Recommendation r) {
    return new RecommendationView(
        r.id,
        r.gap.topic.id,
        r.gap.topic.name,
        r.gap.gapType,
        CatalogService.materialView(r.material),
        r.explanation,
        r.orderIndex,
        r.status);
  }

  public TeacherDashboard teacher() {
    var u = access.current();
    Map<Long, User> students = new LinkedHashMap<>();
    classes.findByTeacherId(u.id).forEach(c -> c.students.forEach(s -> students.put(s.id, s)));
    List<StudentReport> reports = students.keySet().stream().map(this::report).toList();
    Map<Long, List<TopicScore>> byTopic = new LinkedHashMap<>();
    reports.forEach(
        r ->
            r.mastery()
                .forEach(t -> byTopic.computeIfAbsent(t.topicId(), k -> new ArrayList<>()).add(t)));
    List<ClassTopic> scores = new ArrayList<>();
    for (var e : byTopic.entrySet())
      scores.add(
          new ClassTopic(
              e.getKey(),
              e.getValue().get(0).topic(),
              e.getValue().stream().mapToDouble(TopicScore::scorePercent).average().orElse(0),
              e.getValue().size(),
              (int)
                  e.getValue().stream()
                      .filter(s -> s.masteryLevel() == MasteryLevel.NEEDS_PRACTICE)
                      .count(),
              (int)
                  reports.stream()
                      .filter(
                          r ->
                              r.gaps().stream()
                                  .anyMatch(
                                      g ->
                                          g.topicId().equals(e.getKey())
                                              && g.gapType() == GapType.MISCONCEPTION))
                      .count()));
    scores.sort(Comparator.comparingDouble(ClassTopic::average));
    return new TeacherDashboard(
        reports, scores, assessments.findByCreatedByIdAndArchivedFalseOrderByIdDesc(u.id).size());
  }

  public RecommendationView done(Long id, boolean done) {
    Recommendation r = found(recommendations.findById(id));
    if (!r.student.id.equals(access.current().id))
      throw new org.springframework.security.access.AccessDeniedException(
          "This recommendation belongs to another student");
    require(r.gap.status != Status.RESOLVED, "This recommendation is no longer active");
    r.status = done ? Status.DONE : Status.PENDING;
    return recommendationView(r);
  }
}
