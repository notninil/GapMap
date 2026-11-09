package com.csit321g1.thebackurners.repository;

import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import com.csit321g1.thebackurners.entity.*;

public interface UserRepository extends JpaRepository<User, Long> {
  Optional<User> findByEmail(String email);

  @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @Query("select u from User u where u.id=:id")
  Optional<User> lockById(@Param("id") Long id);
}
