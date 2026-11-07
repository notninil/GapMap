package com.gapmap.service;

import static com.gapmap.service.AccessService.*;

import com.gapmap.entity.*;
import com.gapmap.repository.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalysisService {
  final AttemptRepository attempts;
  final double strong, developing;

  public AnalysisService(
      AttemptRepository a,
      @Value("${app.mastery.strong:80}") double s,
      @Value("${app.mastery.developing:50}") double d) {
    require(d >= 0 && s > d && s <= 100, "Invalid mastery thresholds");
    attempts = a;
    strong = s;
    developing = d;
  }

  public MasteryLevel classify(double score) {
    return score >= strong
        ? MasteryLevel.STRONG
        : score >= developing ? MasteryLevel.DEVELOPING : MasteryLevel.NEEDS_PRACTICE;
  }

  @Transactional
  public List<TopicResult> computeTopicScores(Long attemptId) {
    Attempt a = found(attempts.findById(attemptId));
    if (!a.topicResults.isEmpty()) return a.topicResults;
    Map<Topic, List<Response>> groups = new LinkedHashMap<>();
    a.responses.forEach(
        r -> groups.computeIfAbsent(r.question.topic, k -> new ArrayList<>()).add(r));
    for (var e : groups.entrySet()) {
      TopicResult t = new TopicResult();
      t.attempt = a;
      t.topic = e.getKey();
      t.scorePercent =
          100.0 * e.getValue().stream().filter(r -> r.correct).count() / e.getValue().size();
      t.masteryLevel = classify(t.scorePercent);
      a.topicResults.add(t);
    }
    a.totalScore =
        a.responses.isEmpty()
            ? 0
            : 100.0 * a.responses.stream().filter(r -> r.correct).count() / a.responses.size();
    attempts.save(a);
    return a.topicResults;
  }
}
