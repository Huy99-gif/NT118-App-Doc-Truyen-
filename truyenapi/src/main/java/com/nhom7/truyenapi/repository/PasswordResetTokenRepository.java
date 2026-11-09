package com.nhom7.truyenapi.repository;

import com.nhom7.truyenapi.entity.PasswordResetToken;
import com.nhom7.truyenapi.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByToken(String token);
    List<PasswordResetToken> findAllByUserAndUsedFalse(User user);

    @Modifying(clearAutomatically = true)
    @Query("update PasswordResetToken t set t.used = true "
            + "where t.id = :id and t.used = false and t.expiresAt > :now")
    int markUsedIfValid(@Param("id") Long id, @Param("now") Instant now);
}
