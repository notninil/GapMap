package com.gapmap.repository;

import com.gapmap.entity.*;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {
  List<Recommendation> findByStudentIdOrderByOrderIndexAscIdAsc(Long id);
}
