package com.gapmap.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.gapmap.entity.*;
import com.gapmap.repository.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class PrerequisiteServiceTest {
  Topic topic(long id, Topic... p) {
    Topic t = new Topic();
    t.id = id;
    t.prerequisites.addAll(List.of(p));
    return t;
  }

  TopicResult score(Topic t, MasteryLevel m) {
    TopicResult r = new TopicResult();
    r.topic = t;
    r.masteryLevel = m;
    return r;
  }

  @Test
  void weakLoopsIsRootOfArrays() {
    var tr = mock(TopicRepository.class);
    var rr = mock(TopicResultRepository.class);
    Topic variables = topic(1), loops = topic(2, variables), arrays = topic(3, loops);
    when(tr.findById(3L)).thenReturn(Optional.of(arrays));
    when(rr.findByAttemptStudentIdOrderByAttemptSubmittedAtDescAttemptIdDesc(9L))
        .thenReturn(
            List.of(
                score(variables, MasteryLevel.STRONG),
                score(loops, MasteryLevel.NEEDS_PRACTICE),
                score(arrays, MasteryLevel.NEEDS_PRACTICE)));
    assertEquals(List.of(loops), new PrerequisiteService(tr, rr).findRootGaps(9L, 3L));
  }

  @Test
  void deepestWeakPrerequisiteWins() {
    var s = new PrerequisiteService(mock(TopicRepository.class), mock(TopicResultRepository.class));
    Topic v = topic(1), l = topic(2, v), a = topic(3, l);
    assertEquals(
        List.of(v),
        s.roots(
            a,
            Map.of(
                1L,
                score(v, MasteryLevel.NEEDS_PRACTICE),
                2L,
                score(l, MasteryLevel.NEEDS_PRACTICE))));
  }

  @Test
  void unknownPrerequisitesAreNotAssumedWeak() {
    var s = new PrerequisiteService(mock(TopicRepository.class), mock(TopicResultRepository.class));
    assertTrue(s.roots(topic(2, topic(1)), Map.of()).isEmpty());
  }

  @Test
  void branchesReturnAllRootsWithoutDuplicates() {
    var s = new PrerequisiteService(mock(TopicRepository.class), mock(TopicResultRepository.class));
    Topic a = topic(1), b = topic(2), c = topic(3, a), d = topic(4, a, b), e = topic(5, c, d);
    assertEquals(
        List.of(a, b),
        s.roots(
            e,
            Map.of(
                1L,
                score(a, MasteryLevel.NEEDS_PRACTICE),
                2L,
                score(b, MasteryLevel.NEEDS_PRACTICE))));
  }

  @Test
  void cyclesAreRejected() {
    Topic a = topic(1), b = topic(2, a);
    a.prerequisites.add(b);
    assertTrue(CatalogService.reaches(a, 2L, new HashSet<>()));
    assertThrows(
        IllegalStateException.class,
        () ->
            new PrerequisiteService(mock(TopicRepository.class), mock(TopicResultRepository.class))
                .roots(a, Map.of()));
  }

  @Test
  void newestScoreReplacesOldWeakScore() {
    var repo = mock(TopicResultRepository.class);
    Topic t = topic(1);
    when(repo.findByAttemptStudentIdOrderByAttemptSubmittedAtDescAttemptIdDesc(9L))
        .thenReturn(List.of(score(t, MasteryLevel.STRONG), score(t, MasteryLevel.NEEDS_PRACTICE)));
    assertEquals(
        MasteryLevel.STRONG,
        new PrerequisiteService(mock(TopicRepository.class), repo).latest(9L).get(1L).masteryLevel);
  }
}
