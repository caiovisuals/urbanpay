package com.urbanpay.core.ledger.internal;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import com.urbanpay.core.identity.PinVerifier;
import com.urbanpay.core.shared.DomainException;
import com.urbanpay.core.shared.Hashing;
import com.urbanpay.core.shared.Money;

@Service
class TransferService {

    private final PinVerifier pinVerifier;
    private final AccountRepository accounts;
    private final PostingRepository postings;
    private final LedgerEngine ledger;
    private final IdempotencyStore idempotency;
    private final TransactionTemplate tx;
    private final jakarta.persistence.EntityManager em;

    TransferService(PinVerifier pinVerifier, AccountRepository accounts, PostingRepository postings,
                    LedgerEngine ledger, IdempotencyStore idempotency, TransactionTemplate tx,
                    jakarta.persistence.EntityManager em) {
        this.pinVerifier = pinVerifier;
        this.accounts = accounts;
        this.postings = postings;
        this.ledger = ledger;
        this.idempotency = idempotency;
        this.tx = tx;
        this.em = em;
    }

    record Result(TransferResponse transfer, boolean replayed) {
    }

    Result transfer(UUID userId, String idempotencyKey, TransferRequest req) {
        var amount = Money.of(req.amount());
        var description = req.description() == null ? null : req.description().strip();

        // O PIN é verificado ANTES e FORA da transação do ledger: assim a tentativa errada
        // é registrada mesmo com a transferência abortada, e não seguramos duas conexões.
        pinVerifier.verify(userId, req.pin());

        var requestHash = Hashing.sha256Hex(String.join("|", "transfer", req.toAccountId().toString(),
                Long.toString(amount.cents()), String.valueOf(description)));

        return tx.execute(status -> {
            var existing = idempotency.claim(userId, idempotencyKey, requestHash);
            if (existing.isPresent()) {
                return new Result(load(existing.get(), userId), true);
            }

            var sourceId = accounts.findCustomerAccountId(userId)
                    .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "account_not_found", "Conta não encontrada"));
            if (sourceId.equals(req.toAccountId())) {
                throw new DomainException(HttpStatus.UNPROCESSABLE_CONTENT, "same_account",
                        "Não é possível transferir para a própria conta");
            }

            // Transferência só entre contas de cliente; contas de sistema (tesouraria) ficam de fora.
            if (accounts.findKind(req.toAccountId()).filter(k -> k == Account.Kind.CUSTOMER).isEmpty()) {
                throw new DomainException(HttpStatus.NOT_FOUND, "account_not_found", "Conta de destino não encontrada");
            }

            var posted = ledger.post(JournalEntry.Type.TRANSFER, description, userId, List.of(
                    new LedgerEngine.Leg(sourceId, amount.negate()),
                    new LedgerEngine.Leg(req.toAccountId(), amount)));
            idempotency.complete(userId, idempotencyKey, posted.entry().getId());

            return new Result(toResponse(posted.entry(), posted.postings()), false);
        });
    }

    /** Consulta uma transferência; só quem participou dela pode ver. */
    TransferResponse find(UUID transferId, UUID userId) {
        return tx.execute(status -> load(transferId, userId));
    }

    private TransferResponse load(UUID entryId, UUID userId) {
        var entry = em.find(JournalEntry.class, entryId);
        var notFound = new DomainException(HttpStatus.NOT_FOUND, "transfer_not_found", "Transferência não encontrada");
        if (entry == null || entry.getType() != JournalEntry.Type.TRANSFER) {
            throw notFound;
        }
        var legs = postings.findByEntryId(entryId);
        var userAccount = accounts.findCustomerAccountId(userId).orElse(null);
        if (legs.stream().noneMatch(p -> p.getAccountId().equals(userAccount))) {
            throw notFound;
        }
        return toResponse(entry, legs);
    }

    private static TransferResponse toResponse(JournalEntry entry, List<Posting> legs) {
        var debit = legs.stream().filter(p -> p.getAmount().cents() < 0).findFirst().orElseThrow();
        var credit = legs.stream().filter(p -> p.getAmount().cents() > 0).findFirst().orElseThrow();
        return new TransferResponse(entry.getId(), "COMPLETED", debit.getAccountId(), credit.getAccountId(),
                credit.getAmount().toString(), entry.getDescription(), entry.getCreatedAt());
    }
}
