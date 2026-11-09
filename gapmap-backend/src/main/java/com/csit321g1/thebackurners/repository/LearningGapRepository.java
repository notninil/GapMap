package com.csit321g1.thebackurners.repository;

import java.util.*;
import org.springframework.data.jpa.repository.*;

import com.csit321g1.thebackurners.entity.*;

public interface LearningGapRepository extends JpaRepository<LearningGap, Long> {
  List<LearningGap> findByStudentIdOrderByIdDesc(Long id);
}
