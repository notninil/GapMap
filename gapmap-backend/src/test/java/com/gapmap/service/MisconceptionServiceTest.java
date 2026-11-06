package com.gapmap.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.gapmap.entity.*;
import com.gapmap.repository.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class MisconceptionServiceTest {
  @Test
  void onlyIncorrectHighConfidenceAnswersAreFlagged() {
    var repo = mock(AttemptRepository.class);
    Attempt a = new Attempt();
    for (boolean correct : new boolean[] {true, false})
      for (var level : ConfidenceLevel.values()) {
        Response r = new Response();
        r.correct = correct;
        r.confidenceLevel = level;
        a.responses.add(r);
      }
    when(repo.findById(1L)).thenReturn(Optional.of(a));
    var result = new MisconceptionService(repo).detectMisconceptions(1L);
    assertEquals(1, result.size());
    assertFalse(result.get(0).correct);
    assertEquals(ConfidenceLevel.VERY_SURE, result.get(0).confidenceLevel);
  }

  @Test
  void lowConfidenceWrongAnswerIsNotMisconception() {
    Response r = new Response();
    r.correct = false;
    r.confidenceLevel = ConfidenceLevel.NOT_SURE;
    assertFalse(MisconceptionService.isMisconception(r));
  }
}
