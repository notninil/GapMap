package com.gapmap.repository;

import com.gapmap.entity.*;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface LearningMaterialRepository extends JpaRepository<LearningMaterial, Long> {
  List<LearningMaterial> findByTopicIdAndArchivedFalseOrderById(Long id);
}
