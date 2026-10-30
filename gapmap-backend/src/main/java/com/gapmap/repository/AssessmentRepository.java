package com.gapmap.repository;

import com.gapmap.entity.*;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface AssessmentRepository extends JpaRepository<Assessment, Long> {
  List<Assessment> findByCreatedByIdAndArchivedFalseOrderByIdDesc(Long id);

  @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @Query("select a from Assessment a where a.id=:id")
  Optional<Assessment> lockById(@Param("id") Long id);
}
