package com.gapmap.repository;

import com.gapmap.entity.*;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface ClassRoomRepository extends JpaRepository<ClassRoom, Long> {
  List<ClassRoom> findByTeacherId(Long id);

  List<ClassRoom> findByStudentsId(Long id);

  Optional<ClassRoom> findByJoinCode(String code);
}
