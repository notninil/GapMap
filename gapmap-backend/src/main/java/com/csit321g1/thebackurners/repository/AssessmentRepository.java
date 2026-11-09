package com.csit321g1.thebackurners.repository;

import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import com.csit321g1.thebackurners.entity.*;

public interface AssessmentRepository extends JpaRepository<Assessment, Long> {
  List<Assessment> findByCreatedByIdAndArchivedFalseOrderByIdDesc(Long id);

  @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @Query("select a from Assessment a where a.id=:id")
  Optional<Assessment> lockById(@Param("id") Long id);
}
