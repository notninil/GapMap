package com.gapmap.repository;

import com.gapmap.entity.*;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface LearningGapRepository extends JpaRepository<LearningGap, Long> {
  List<LearningGap> findByStudentIdOrderByIdDesc(Long id);
}
