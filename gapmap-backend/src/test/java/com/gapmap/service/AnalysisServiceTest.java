package com.gapmap.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.gapmap.entity.*;
import com.gapmap.repository.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class AnalysisServiceTest {
  @Test
  void scoresArePerTopicNotOverall() {
    var repo = mock(AttemptRepository.class);
    var service = new AnalysisService(repo, 80, 50);
    Attempt a = new Attempt();
    a.id = 1L;
    Topic loops = new Topic();
    loops.id = 1L;
    Topic arrays = new Topic();
    arrays.id = 2L;
    for (int i = 0; i < 5; i++) {
      Response r = new Response();
      r.question = new Question();
      r.question.topic = i < 3 ? loops : arrays;
      r.correct = i < 2;
      a.responses.add(r);
    }
    when(repo.findById(1L)).thenReturn(Optional.of(a));
    var scores = service.computeTopicScores(1L);
    assertEquals(2, scores.size());
    assertEquals(66.6667, scores.get(0).scorePercent, .001);
    assertEquals(MasteryLevel.DEVELOPING, scores.get(0).masteryLevel);
    assertEquals(0, scores.get(1).scorePercent);
    assertEquals(40, a.totalScore);
    assertEquals(2, service.computeTopicScores(1L).size());
  }

  @Test
  void boundariesAreInclusive() {
    var s = new AnalysisService(mock(AttemptRepository.class), 80, 50);
    assertEquals(MasteryLevel.STRONG, s.classify(80));
    assertEquals(MasteryLevel.DEVELOPING, s.classify(79.99));
    assertEquals(MasteryLevel.DEVELOPING, s.classify(50));
    assertEquals(MasteryLevel.NEEDS_PRACTICE, s.classify(49.99));
  }

  @Test
  void thresholdsAreConfigurable() {
    var s = new AnalysisService(mock(AttemptRepository.class), 90, 60);
    assertEquals(MasteryLevel.DEVELOPING, s.classify(80));
    assertEquals(MasteryLevel.NEEDS_PRACTICE, s.classify(59));
  }

  @Test
  void invalidThresholdsFailFast() {
    assertThrows(
        RuntimeException.class, () -> new AnalysisService(mock(AttemptRepository.class), 40, 50));
  }
}
