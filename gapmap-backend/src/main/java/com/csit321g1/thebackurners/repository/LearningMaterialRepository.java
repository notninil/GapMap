package com.csit321g1.thebackurners.repository;

import java.util.*;
import org.springframework.data.jpa.repository.*;

import com.csit321g1.thebackurners.entity.*;

public interface LearningMaterialRepository extends JpaRepository<LearningMaterial, Long> {
  List<LearningMaterial> findByTopicIdAndArchivedFalseOrderById(Long id);
}
