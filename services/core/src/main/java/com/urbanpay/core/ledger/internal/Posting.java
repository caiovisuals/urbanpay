package com.urbanpay.core.ledger.internal;

import java.time.Instant;
import java.util.UUID;

import com.urbanpay.core.shared.Money;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Uma "perna" do lançamento: valor negativo = débito (sai da conta), positivo = crédito.
 * A soma das pernas de um lançamento é sempre zero (garantido por trigger no COMMIT).
 */
@Entity
@Table(name = "postings")
class Posting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entry_id", nullable = false)
    private UUID entryId;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "amount_cents", nullable = false)
    private long amountCents;

    @Column(name = "balance_after_cents", nullable = false)
    private long balanceAfterCents;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Posting() {
    }

    Posting(UUID entryId, UUID accountId, Money amount, Money balanceAfter, Instant now) {
        this.entryId = entryId;
        this.accountId = accountId;
        this.amountCents = amount.cents();
        this.balanceAfterCents = balanceAfter.cents();
        this.createdAt = now;
    }

    UUID getAccountId() {
        return accountId;
    }

    Money getBalanceAfter() {
        return Money.ofCents(balanceAfterCents);
    }

    Money getAmount() {
        return Money.ofCents(amountCents);
    }
}
