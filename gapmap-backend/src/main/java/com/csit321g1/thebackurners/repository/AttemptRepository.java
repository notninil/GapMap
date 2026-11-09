package com.csit321g1.thebackurners.repository;

import java.util.*;
import org.springframework.data.jpa.repository.*;

import com.csit321g1.thebackurners.entity.*;

public interface AttemptRepository extends JpaRepository<Attempt, Long> {
  List<Attempt> findByStudentIdOrderBySubmittedAtAscIdAsc(Long id);

  Optional<Attempt> findByStudentIdAndSubmissionKey(Long id, String key);

  boolean existsByAssessmentId(Long id);
}
