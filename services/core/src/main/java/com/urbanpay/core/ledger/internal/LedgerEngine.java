package com.urbanpay.core.ledger.internal;

import java.time.Clock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.urbanpay.core.shared.DomainException;
import com.urbanpay.core.shared.Money;

import jakarta.persistence.EntityManager;

/**
 * Único ponto do sistema que movimenta dinheiro. Todo produto (Pix, cartão, porquinho,
 * empréstimo) vai virar uma chamada a {@link #post}, com as pernas que fizerem sentido.
 */
@Component
class LedgerEngine {

    private final AccountRepository accounts;
    private final EntityManager em;
    private final Clock clock;

    LedgerEngine(AccountRepository accounts, EntityManager em, Clock clock) {
        this.accounts = accounts;
        this.em = em;
        this.clock = clock;
    }

    record Leg(UUID accountId, Money amount) {
    }

    record Posted(JournalEntry entry, List<Posting> postings) {
    }

    @Transactional(propagation = Propagation.MANDATORY)
    Posted post(JournalEntry.Type type, String description, UUID initiatedBy, List<Leg> legs) {
        long sum = legs.stream().mapToLong(l -> l.amount().cents()).sum();
        if (legs.size() < 2 || sum != 0 || legs.stream().anyMatch(l -> l.amount().cents() == 0)) {
            // Erro de programação, não do usuário: o trigger do banco também barraria.
            throw new IllegalStateException("Unbalanced journal entry: " + legs);
        }

        var ids = legs.stream().map(Leg::accountId).collect(Collectors.toSet());
        var locked = accounts.lockAllOrdered(ids).stream()
                .collect(Collectors.toMap(Account::getId, Function.identity()));
        if (locked.size() != ids.size()) {
            throw new DomainException(HttpStatus.NOT_FOUND, "account_not_found", "Conta não encontrada");
        }

        // Valida tudo antes de aplicar qualquer perna (considerando várias pernas na mesma conta).
        var deltas = new HashMap<UUID, Money>();
        legs.forEach(l -> deltas.merge(l.accountId(), l.amount(), Money::plus));
        deltas.forEach((accountId, delta) -> {
            if (!locked.get(accountId).canApply(delta)) {
                throw new DomainException(HttpStatus.UNPROCESSABLE_CONTENT, "insufficient_funds", "Saldo insuficiente");
            }
        });

        var now = clock.instant();
        var entry = new JournalEntry(type, description, initiatedBy, now);
        em.persist(entry);

        var postings = new ArrayList<Posting>(legs.size());
        for (var leg : legs) {
            var balanceAfter = locked.get(leg.accountId()).apply(leg.amount());
            var posting = new Posting(entry.getId(), leg.accountId(), leg.amount(), balanceAfter, now);
            em.persist(posting);
            postings.add(posting);
        }
        return new Posted(entry, postings);
    }
}