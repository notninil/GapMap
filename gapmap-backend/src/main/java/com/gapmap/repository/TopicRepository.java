package com.gapmap.repository;

import com.gapmap.entity.*;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface TopicRepository extends JpaRepository<Topic, Long> {
  List<Topic> findByTeacherIdOrderById(Long id);
}
