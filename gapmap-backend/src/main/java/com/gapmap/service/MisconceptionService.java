package com.gapmap.service;

import static com.gapmap.service.AccessService.*;

import com.gapmap.entity.*;
import com.gapmap.repository.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MisconceptionService {
  final AttemptRepository attempts;

  public MisconceptionService(AttemptRepository a) {
    attempts = a;
  }

  public static boolean isMisconception(Response r) {
    return !r.correct && r.confidenceLevel == ConfidenceLevel.VERY_SURE;
  }

  @Transactional(readOnly = true)
  public List<Response> detectMisconceptions(Long attemptId) {
    return found(attempts.findById(attemptId)).responses.stream()
        .filter(MisconceptionService::isMisconception)
        .toList();
  }
}
