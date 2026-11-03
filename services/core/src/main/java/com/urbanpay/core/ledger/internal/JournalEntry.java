package com.urbanpay.core.ledger.internal;

import java.time.Instant;
import java.util.UUID;

import com.urbanpay.core.shared.Ids;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Cabeçalho de um lançamento contábil. Imutável (o banco bloqueia UPDATE/DELETE). */
@Entity
@Table(name = "journal_entries")
class JournalEntry {

    enum Type { DEPOSIT, TRANSFER }

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Type type;

    private String description;

    @Column(name = "initiated_by")
    private UUID initiatedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected JournalEntry() {
    }

    JournalEntry(Type type, String description, UUID initiatedBy, Instant now) {
        this.id = Ids.newId();
        this.type = type;
        this.description = description;
        this.initiatedBy = initiatedBy;
        this.createdAt = now;
    }

    UUID getId() {
        return id;
    }

    Type getType() {
        return type;
    }

    String getDescription() {
        return description;
    }

    UUID getInitiatedBy() {
        return initiatedBy;
    }

    Instant getCreatedAt() {
        return createdAt;
    }
}