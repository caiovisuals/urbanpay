package com.urbanpay.core.ledger.internal;

import java.time.Clock;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.urbanpay.core.identity.UserRegistered;

/**
 * Abre a conta corrente quando um usuário se cadastra.
 * Listener síncrono = mesma transação do cadastro (atomicidade). Quando os módulos virarem
 * serviços separados, este evento passa a trafegar via outbox + Kafka (fase 3).
 */
@Component
class AccountOpener {

    private final AccountRepository accounts;
    private final Clock clock;

    AccountOpener(AccountRepository accounts, Clock clock) {
        this.accounts = accounts;
        this.clock = clock;
    }

    @EventListener
    void on(UserRegistered event) {
        accounts.save(Account.customer(event.userId(), clock.instant()));
    }
}
