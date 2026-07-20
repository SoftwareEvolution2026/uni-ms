package com.uni.ms.identity.infrastructure;

import com.uni.ms.identity.domain.RefreshToken;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT token FROM RefreshToken token JOIN FETCH token.user "
            + "WHERE token.tokenHash = :tokenHash")
    Optional<RefreshToken> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

    @Modifying
    @Query("UPDATE RefreshToken token SET token.revoked = true, token.revokedAt = CURRENT_TIMESTAMP "
            + "WHERE token.user.id = :userId AND token.revoked = false")
    int revokeAllActiveForUser(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE RefreshToken token SET token.revoked = true, token.revokedAt = CURRENT_TIMESTAMP "
            + "WHERE token.familyId = :familyId AND token.revoked = false")
    int revokeActiveFamily(@Param("familyId") UUID familyId);
}
