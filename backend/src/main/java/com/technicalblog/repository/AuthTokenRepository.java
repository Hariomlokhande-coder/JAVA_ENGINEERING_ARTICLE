package com.technicalblog.repository;

import com.technicalblog.entity.AuthToken;
import com.technicalblog.entity.AuthTokenType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AuthTokenRepository extends JpaRepository<AuthToken, Long> {

    Optional<AuthToken> findByTokenHashAndType(String tokenHash, AuthTokenType type);

    /** Housekeeping: tokens that can no longer be used are not worth keeping. */
    @Modifying
    @Query("delete from AuthToken t where t.expiresAt < :cutoff or t.usedAt is not null")
    int deleteExpiredOrUsed(@Param("cutoff") java.time.Instant cutoff);

    /** Older tokens of the same kind stop working as soon as a new one is issued. */
    @Modifying
    @Query("delete from AuthToken t where t.user.id = :userId and t.type = :type")
    void deleteByUserIdAndType(@Param("userId") Long userId, @Param("type") AuthTokenType type);
}
