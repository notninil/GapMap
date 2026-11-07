package com.gapmap.repository;

import com.gapmap.entity.*;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface AttemptRepository extends JpaRepository<Attempt, Long> {
  List<Attempt> findByStudentIdOrderBySubmittedAtAscIdAsc(Long id);

  Optional<Attempt> findByStudentIdAndSubmissionKey(Long id, String key);

  boolean existsByAssessmentId(Long id);
}
