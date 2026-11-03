package com.urbanpay.core.identity.internal;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;

interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    /** Lock pessimista: dois refresh simultâneos com o mesmo token não podem ambos vencer. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from RefreshToken t where t.tokenHash = :hash")
    Optional<RefreshToken> findByHashForUpdate(String hash);

    @Query("select count(t) > 0 from RefreshToken t where t.familyId = :familyId "
            + "and t.revokedAt is null and t.expiresAt > :now")
    boolean isFamilyActive(UUID familyId, Instant now);

    @Modifying
    @Query("update RefreshToken t set t.revokedAt = :now, t.version = t.version + 1 "
            + "where t.familyId = :familyId and t.revokedAt is null")
    int revokeFamily(UUID familyId, Instant now);
}