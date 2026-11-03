package com.urbanpay.core.ledger.internal;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.urbanpay.core.shared.DomainException;
import com.urbanpay.core.shared.Hashing;
import com.urbanpay.core.shared.Money;

/**
 * Depósito simulado: o dinheiro sai da conta de sistema TREASURY e entra na conta do cliente.
 * Em produção isso seria disparado pela liquidação de um Pix recebido, boleto pago, etc.
 */
@Service
class DepositService {

    /** Criada na migration V2. */
    static final UUID TREASURY_ID = UUID.fromString("00000000-0000-7000-8000-000000000001");

    private final AccountRepository accounts;
    private final LedgerEngine ledger;
    private final IdempotencyStore idempotency;

    DepositService(AccountRepository accounts, LedgerEngine ledger, IdempotencyStore idempotency) {
        this.accounts = accounts;
        this.ledger = ledger;
        this.idempotency = idempotency;
    }

    @Transactional
    AccountResponse deposit(UUID userId, String idempotencyKey, Money amount) {
        var hash = Hashing.sha256Hex("deposit|" + amount.cents());
        var accountId = accounts.findCustomerAccountId(userId)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "account_not_found", "Conta não encontrada"));

        if (idempotency.claim(userId, idempotencyKey, hash).isEmpty()) {
            var posted = ledger.post(JournalEntry.Type.DEPOSIT, "Depósito", userId, List.of(
                    new LedgerEngine.Leg(TREASURY_ID, amount.negate()),
                    new LedgerEngine.Leg(accountId, amount)));
            idempotency.complete(userId, idempotencyKey, posted.entry().getId());
        }
        var account = accounts.findById(accountId).orElseThrow();
        return new AccountResponse(account.getId(), account.getCurrency(), account.balance().toString());
    }
}
