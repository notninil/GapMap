package com.csit321g1.thebackburners.gapmap.repository;

import java.util.*;
import org.springframework.data.jpa.repository.*;

import com.csit321g1.thebackburners.gapmap.entity.*;

public interface TopicResultRepository extends JpaRepository<TopicResult, Long> {
  List<TopicResult> findByAttemptStudentIdOrderByAttemptSubmittedAtDescAttemptIdDesc(Long id);
}
