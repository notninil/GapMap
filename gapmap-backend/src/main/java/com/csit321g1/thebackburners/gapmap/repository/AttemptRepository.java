package com.csit321g1.thebackburners.gapmap.repository;

import java.util.*;
import org.springframework.data.jpa.repository.*;

import com.csit321g1.thebackburners.gapmap.entity.*;

public interface AttemptRepository extends JpaRepository<Attempt, Long> {
  List<Attempt> findByStudentIdOrderBySubmittedAtAscIdAsc(Long id);

  Optional<Attempt> findByStudentIdAndSubmissionKey(Long id, String key);

  boolean existsByAssessmentId(Long id);
}
