package com.csit321g1.thebackurners.repository;

import java.util.*;
import org.springframework.data.jpa.repository.*;

import com.csit321g1.thebackurners.entity.*;

public interface ClassRoomRepository extends JpaRepository<ClassRoom, Long> {
  List<ClassRoom> findByTeacherId(Long id);

  List<ClassRoom> findByStudentsId(Long id);

  Optional<ClassRoom> findByJoinCode(String code);
}
