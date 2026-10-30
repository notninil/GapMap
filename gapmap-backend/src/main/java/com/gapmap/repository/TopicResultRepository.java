package com.gapmap.repository;

import com.gapmap.entity.*;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface TopicResultRepository extends JpaRepository<TopicResult, Long> {
  List<TopicResult> findByAttemptStudentIdOrderByAttemptSubmittedAtDescAttemptIdDesc(Long id);
}
