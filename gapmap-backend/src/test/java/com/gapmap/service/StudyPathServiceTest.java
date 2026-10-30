package com.gapmap.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.gapmap.entity.*;
import com.gapmap.repository.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class StudyPathServiceTest {
  Topic topic(long id, String name, Topic... p) {
    Topic t = new Topic();
    t.id = id;
    t.name = name;
    t.prerequisites.addAll(List.of(p));
    return t;
  }

  @Test
  void rootLessonsPrecedeDirectAndMisconceptionLessons() {
    var pre = mock(PrerequisiteService.class);
    var gaps = mock(LearningGapRepository.class);
    var materials = mock(LearningMaterialRepository.class);
    var recs = mock(RecommendationRepository.class);
    var service = new StudyPathService(pre, gaps, materials, recs, true);
    Topic loops = topic(1, "Loops"), arrays = topic(2, "Arrays", loops);
    User student = new User();
    student.id = 7L;
    Attempt a = new Attempt();
    a.id = 1L;
    a.student = student;
    Response response = new Response();
    response.question = new Question();
    response.question.topic = arrays;
    response.confidenceLevel = ConfidenceLevel.VERY_SURE;
    response.correct = false;
    a.responses.add(response);
    Map<Long, TopicResult> latest = new LinkedHashMap<>();
    for (Topic t : List.of(arrays, loops)) {
      TopicResult r = new TopicResult();
      r.topic = t;
      r.attempt = a;
      r.masteryLevel = MasteryLevel.NEEDS_PRACTICE;
      r.scorePercent = 40;
      latest.put(t.id, r);
      LearningMaterial m = new LearningMaterial();
      m.id = t.id;
      m.topic = t;
      when(materials.findByTopicIdAndArchivedFalseOrderById(t.id)).thenReturn(List.of(m));
    }
    when(pre.latest(7L)).thenReturn(latest);
    when(pre.roots(eq(arrays), anyMap())).thenReturn(List.of(loops));
    when(pre.roots(eq(loops), anyMap())).thenReturn(List.of());
    when(gaps.findByStudentIdOrderByIdDesc(7L)).thenReturn(List.of());
    when(recs.findByStudentIdOrderByOrderIndexAscIdAsc(7L)).thenReturn(List.of());
    when(gaps.save(any())).thenAnswer(i -> i.getArgument(0));
    when(recs.save(any())).thenAnswer(i -> i.getArgument(0));
    var path = service.generateStudyPath(7L);
    assertEquals(3, path.size());
    assertEquals(GapType.ROOT, path.get(0).gap.gapType);
    assertEquals(loops, path.get(0).gap.topic);
    assertTrue(path.get(0).explanation.contains("prerequisite of Arrays"));
    assertEquals(GapType.DIRECT, path.get(1).gap.gapType);
    assertEquals(GapType.MISCONCEPTION, path.get(2).gap.gapType);
    assertEquals(3, path.get(2).orderIndex);
  }

  @Test
  void topologicalOrderTraversesUnselectedIntermediateTopics() {
    var s = new StudyPathService(null, null, null, null, true);
    Topic v = topic(1, "Variables"), l = topic(2, "Loops", v), a = topic(3, "Arrays", l);
    assertEquals(List.of(v, a), s.orderedTopics(List.of(a, v)));
  }

  @Test
  void completedTopicsResolveOldRecommendations() {
    var pre = mock(PrerequisiteService.class);
    var gaps = mock(LearningGapRepository.class);
    var recs = mock(RecommendationRepository.class);
    LearningGap g = new LearningGap();
    g.topic = topic(1, "Loops");
    g.gapType = GapType.DIRECT;
    Recommendation r = new Recommendation();
    r.gap = g;
    r.material = new LearningMaterial();
    r.material.id = 1L;
    when(pre.latest(1L)).thenReturn(Map.of());
    when(gaps.findByStudentIdOrderByIdDesc(1L)).thenReturn(List.of(g));
    when(recs.findByStudentIdOrderByOrderIndexAscIdAsc(1L)).thenReturn(List.of(r));
    assertTrue(
        new StudyPathService(pre, gaps, mock(LearningMaterialRepository.class), recs, true)
            .generateStudyPath(1L)
            .isEmpty());
    assertEquals(Status.RESOLVED, g.status);
    assertEquals(Status.RESOLVED, r.status);
  }
}
