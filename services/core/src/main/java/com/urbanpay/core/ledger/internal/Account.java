package com.urbanpay.core.ledger.internal;

import java.time.Instant;
import java.util.UUID;

import com.urbanpay.core.shared.Ids;
import com.urbanpay.core.shared.Money;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * Conta contábil. O saldo é materializado ({@code balance_cents}) por desempenho, mas é
 * sempre atualizado na mesma transação que grava os lançamentos (postings), então
 * saldo == soma dos postings da conta. O teste de invariantes verifica isso.
 */
@Entity
@Table(name = "accounts")
class Account {

    enum Kind { CUSTOMER, SYSTEM }

    @Id
    private UUID id;

    @Column(name = "owner_id")
    private UUID ownerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Kind kind;

    private String code;

    @Column(nullable = false)
    private String currency;

    @Column(name = "balance_cents", nullable = false)
    private long balanceCents;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Version
    private Long version;

    protected Account() {
    }

    static Account customer(UUID ownerId, Instant now) {
        var account = new Account();
        account.id = Ids.newId();
        account.ownerId = ownerId;
        account.kind = Kind.CUSTOMER;
        account.currency = "BRL";
        account.createdAt = now;
        return account;
    }

    /** Contas de cliente não podem ficar negativas; contas de sistema (tesouraria) podem. */
    boolean canApply(Money delta) {
        return kind == Kind.SYSTEM || balanceCents + delta.cents() >= 0;
    }

    Money apply(Money delta) {
        balanceCents = Math.addExact(balanceCents, delta.cents());
        return balance();
    }

    UUID getId() {
        return id;
    }

    UUID getOwnerId() {
        return ownerId;
    }

    Kind getKind() {
        return kind;
    }

    String getCurrency() {
        return currency;
    }

    Money balance() {
        return Money.ofCents(balanceCents);
    }
}