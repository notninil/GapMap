package com.csit321g1.thebackurners.service;

import static com.csit321g1.thebackurners.service.AccessService.*;

import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.csit321g1.thebackurners.entity.*;
import com.csit321g1.thebackurners.repository.*;

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
