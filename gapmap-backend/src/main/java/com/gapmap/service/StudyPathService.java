package com.gapmap.service;

import com.gapmap.entity.*;
import com.gapmap.repository.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudyPathService {
  final PrerequisiteService prerequisites;
  final LearningGapRepository gaps;
  final LearningMaterialRepository materials;
  final RecommendationRepository recommendations;
  final boolean includeDeveloping;

  public StudyPathService(
      PrerequisiteService p,
      LearningGapRepository g,
      LearningMaterialRepository m,
      RecommendationRepository r,
      @Value("${app.gaps.include-developing:true}") boolean include) {
    prerequisites = p;
    gaps = g;
    materials = m;
    recommendations = r;
    includeDeveloping = include;
  }

  public List<Topic> orderedTopics(Collection<Topic> topics) {
    Map<Long, Topic> selected = new LinkedHashMap<>();
    topics.forEach(t -> selected.put(t.id, t));
    List<Topic> ordered = new ArrayList<>();
    Set<Long> visited = new HashSet<>();
    for (Topic t : topics) visit(t, selected, visited, ordered);
    return ordered;
  }

  private void visit(Topic t, Map<Long, Topic> selected, Set<Long> visited, List<Topic> ordered) {
    if (!visited.add(t.id)) return;
    for (Topic p : t.prerequisites) visit(p, selected, visited, ordered);
    if (selected.containsKey(t.id)) ordered.add(t);
  }

  @Transactional
  public List<Recommendation> generateStudyPath(Long studentId) {
    Map<Long, TopicResult> latest = prerequisites.latest(studentId);
    var old = gaps.findByStudentIdOrderByIdDesc(studentId);
    old.forEach(g -> g.status = Status.RESOLVED);
    var existing = recommendations.findByStudentIdOrderByOrderIndexAscIdAsc(studentId);
    Map<String, Recommendation> reusable = new HashMap<>();
    for (var r : existing) {
      reusable.put(r.gap.topic.id + ":" + r.gap.gapType + ":" + r.material.id, r);
      if (r.status == Status.PENDING) r.status = Status.RESOLVED;
    }
    List<LearningGap> active = new ArrayList<>();
    Map<Long, String> rootReasons = new HashMap<>();
    Set<Long> roots = new HashSet<>();
    for (var r : latest.values())
      if (r.masteryLevel == MasteryLevel.NEEDS_PRACTICE
          || (includeDeveloping && r.masteryLevel == MasteryLevel.DEVELOPING))
        for (Topic root : prerequisites.roots(r.topic, latest)) {
          roots.add(root.id);
          rootReasons.merge(root.id, r.topic.name, (a, b) -> a + ", " + b);
        }
    for (var r : latest.values()) {
      if (roots.contains(r.topic.id)) active.add(gap(r, GapType.ROOT, old));
      else if (r.masteryLevel == MasteryLevel.NEEDS_PRACTICE
          || (includeDeveloping && r.masteryLevel == MasteryLevel.DEVELOPING))
        active.add(gap(r, GapType.DIRECT, old));
      if (r.attempt.responses.stream()
          .anyMatch(
              x ->
                  x.question.topic.id.equals(r.topic.id)
                      && MisconceptionService.isMisconception(x)))
        active.add(gap(r, GapType.MISCONCEPTION, old));
    }
    List<Topic> order = orderedTopics(active.stream().map(g -> g.topic).toList());
    Map<Long, Integer> rank = new HashMap<>();
    for (int i = 0; i < order.size(); i++) rank.put(order.get(i).id, i);
    active.sort(
        Comparator.comparingInt(
                (LearningGap g) ->
                    g.gapType == GapType.ROOT ? 0 : g.gapType == GapType.DIRECT ? 1 : 2)
            .thenComparingInt(g -> rank.get(g.topic.id)));
    List<Recommendation> path = new ArrayList<>();
    int index = 1;
    for (var g : active) {
      TopicResult score = latest.get(g.topic.id);
      String explanation =
          g.gapType == GapType.MISCONCEPTION
              ? "An incorrect, very confident answer suggests a possible misconception in "
                  + g.topic.name
                  + ". Review the explanation and check your understanding."
              : "You scored "
                  + Math.round(score.scorePercent)
                  + "% in "
                  + g.topic.name
                  + (g.gapType == GapType.ROOT
                      ? ", a prerequisite of " + rootReasons.get(g.topic.id)
                      : ". Strengthen this topic before moving forward")
                  + ".";
      for (var m : materials.findByTopicIdAndArchivedFalseOrderById(g.topic.id)) {
        String key = g.topic.id + ":" + g.gapType + ":" + m.id;
        Recommendation r = reusable.get(key);
        if (r == null) {
          r = new Recommendation();
          r.student = g.student;
          r.material = m;
          r.status = Status.PENDING;
        } else if (r.status == Status.RESOLVED) r.status = Status.PENDING;
        r.gap = g;
        r.explanation = explanation;
        r.orderIndex = index++;
        path.add(recommendations.save(r));
      }
    }
    return path;
  }

  private LearningGap gap(TopicResult r, GapType type, List<LearningGap> old) {
    LearningGap g =
        old.stream()
            .filter(x -> x.topic.id.equals(r.topic.id) && x.gapType == type)
            .findFirst()
            .orElseGet(LearningGap::new);
    g.student = r.attempt.student;
    g.topic = r.topic;
    g.attempt = r.attempt;
    g.gapType = type;
    g.status = Status.PENDING;
    return gaps.save(g);
  }
}
