package com.csit321g1.thebackurners.repository;

import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import com.csit321g1.thebackurners.entity.*;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
  @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @Query("select t from PasswordResetToken t join fetch t.user where t.tokenHash=:hash")
  Optional<PasswordResetToken> lockByTokenHash(@Param("hash") String hash);

  long countByUserAndCreatedAtAfter(User user, Instant since);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("update PasswordResetToken t set t.usedAt=:now where t.user=:user and t.usedAt is null")
  int invalidateOutstanding(@Param("user") User user, @Param("now") Instant now);

  @Modifying
  @Query("delete from PasswordResetToken t where t.expiresAt<:cutoff")
  int deleteExpiredBefore(@Param("cutoff") Instant cutoff);
}
