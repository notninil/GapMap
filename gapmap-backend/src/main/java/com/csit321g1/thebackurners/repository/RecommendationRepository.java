package com.csit321g1.thebackurners.repository;

import java.util.*;
import org.springframework.data.jpa.repository.*;

import com.csit321g1.thebackurners.entity.*;

public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {
  List<Recommendation> findByStudentIdOrderByOrderIndexAscIdAsc(Long id);
}
