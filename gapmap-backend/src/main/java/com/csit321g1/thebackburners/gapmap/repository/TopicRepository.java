package com.csit321g1.thebackburners.gapmap.repository;

import java.util.*;
import org.springframework.data.jpa.repository.*;

import com.csit321g1.thebackburners.gapmap.entity.*;

public interface TopicRepository extends JpaRepository<Topic, Long> {
  List<Topic> findByTeacherIdOrderById(Long id);
}
