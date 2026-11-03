package com.urbanpay.core.identity.internal;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.urbanpay.core.shared.Ids;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "users")
class User {

    @Id
    private UUID id;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String phone;

    @Column(nullable = false)
    private String cpf;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "pin_hash", nullable = false)
    private String pinHash;

    @Column(name = "pin_failed_attempts", nullable = false)
    private int pinFailedAttempts;

    @Column(name = "pin_locked_until")
    private Instant pinLockedUntil;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Version
    private Long version;

    protected User() {
    }

    User(String fullName, String email, String phone, String cpf, LocalDate birthDate,
         String passwordHash, String pinHash, Instant now) {
        this.id = Ids.newId();
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.cpf = cpf;
        this.birthDate = birthDate;
        this.passwordHash = passwordHash;
        this.pinHash = pinHash;
        this.createdAt = now;
    }

    boolean isPinLocked(Instant now) {
        return pinLockedUntil != null && pinLockedUntil.isAfter(now);
    }

    void registerPinFailure(int maxAttempts, Instant lockUntil) {
        pinFailedAttempts++;
        if (pinFailedAttempts >= maxAttempts) {
            pinLockedUntil = lockUntil;
            pinFailedAttempts = 0;
        }
    }

    void registerPinSuccess() {
        pinFailedAttempts = 0;
        pinLockedUntil = null;
    }

    UUID getId() {
        return id;
    }

    String getFullName() {
        return fullName;
    }

    String getEmail() {
        return email;
    }

    String getPhone() {
        return phone;
    }

    String getCpf() {
        return cpf;
    }

    LocalDate getBirthDate() {
        return birthDate;
    }

    String getPasswordHash() {
        return passwordHash;
    }

    String getPinHash() {
        return pinHash;
    }

    Instant getCreatedAt() {
        return createdAt;
    }
}
