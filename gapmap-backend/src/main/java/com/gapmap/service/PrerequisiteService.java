package com.gapmap.service;

import static com.gapmap.service.AccessService.*;

import com.gapmap.entity.*;
import com.gapmap.repository.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PrerequisiteService {
  final TopicRepository topics;
  final TopicResultRepository results;

  public PrerequisiteService(TopicRepository t, TopicResultRepository r) {
    topics = t;
    results = r;
  }

  public Map<Long, TopicResult> latest(Long studentId) {
    Map<Long, TopicResult> map = new LinkedHashMap<>();
    results
        .findByAttemptStudentIdOrderByAttemptSubmittedAtDescAttemptIdDesc(studentId)
        .forEach(r -> map.putIfAbsent(r.topic.id, r));
    return map;
  }

  @Transactional(readOnly = true)
  public List<Topic> findRootGaps(Long studentId, Long topicId) {
    return roots(found(topics.findById(topicId)), latest(studentId));
  }

  public List<Topic> roots(Topic topic, Map<Long, TopicResult> latest) {
    LinkedHashMap<Long, Topic> found = new LinkedHashMap<>();
    walk(topic, latest, new HashSet<>(), found, false);
    return new ArrayList<>(found.values());
  }

  private boolean walk(
      Topic t,
      Map<Long, TopicResult> latest,
      Set<Long> path,
      Map<Long, Topic> found,
      boolean include) {
    if (!path.add(t.id)) throw new IllegalStateException("Cyclic prerequisite graph");
    boolean below = false;
    for (Topic p : t.prerequisites) below = walk(p, latest, path, found, true) || below;
    path.remove(t.id);
    TopicResult r = latest.get(t.id);
    boolean weak = include && r != null && r.masteryLevel == MasteryLevel.NEEDS_PRACTICE;
    if (weak && !below) found.putIfAbsent(t.id, t);
    return below || weak;
  }
}
