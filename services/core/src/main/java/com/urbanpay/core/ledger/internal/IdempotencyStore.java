package com.urbanpay.core.ledger.internal;

import java.time.Clock;
import java.sql.Timestamp;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.urbanpay.core.shared.DomainException;

/**
 * Idempotência no estilo Stripe ({@code Idempotency-Key}). A chave é reservada com
 * {@code INSERT ... ON CONFLICT DO NOTHING} dentro da transação da operação:
 * <ul>
 *   <li>Requisição repetida depois do sucesso: devolve o recurso já criado.</li>
 *   <li>Duas requisições simultâneas com a mesma chave: a segunda espera o lock do índice
 *       único até a primeira terminar, e então recebe o mesmo resultado.</li>
 *   <li>Se a operação falhar, a reserva sofre rollback junto e a chave pode ser reusada.</li>
 * </ul>
 */
@Component
class IdempotencyStore {

    private final JdbcClient jdbc;
    private final Clock clock;

    IdempotencyStore(JdbcClient jdbc, Clock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    /** @return o id do recurso já criado com esta chave, ou vazio se a chave foi reservada agora. */
    @Transactional(propagation = Propagation.MANDATORY)
    Optional<UUID> claim(UUID ownerId, String key, String requestHash) {
        int inserted = jdbc.sql("""
                        INSERT INTO idempotency_keys (owner_id, idem_key, request_hash, created_at)
                        VALUES (:owner, :key, :hash, :now)
                        ON CONFLICT (owner_id, idem_key) DO NOTHING
                        """)
                .param("owner", ownerId).param("key", key).param("hash", requestHash)
                .param("now", Timestamp.from(clock.instant()))
                .update();
        if (inserted == 1) {
            return Optional.empty();
        }

        var existing = jdbc.sql("SELECT request_hash, resource_id FROM idempotency_keys WHERE owner_id = :owner AND idem_key = :key")
                .param("owner", ownerId).param("key", key)
                .query((rs, n) -> new Existing(rs.getString(1), rs.getObject(2, UUID.class)))
                .single();
        if (!existing.requestHash().equals(requestHash)) {
            throw new DomainException(HttpStatus.UNPROCESSABLE_CONTENT, "idempotency_key_reused",
                    "Esta Idempotency-Key já foi usada com outra requisição");
        }
        return Optional.of(existing.resourceId());
    }

    @Transactional(propagation = Propagation.MANDATORY)
    void complete(UUID ownerId, String key, UUID resourceId) {
        jdbc.sql("UPDATE idempotency_keys SET resource_id = :rid WHERE owner_id = :owner AND idem_key = :key")
                .param("rid", resourceId).param("owner", ownerId).param("key", key)
                .update();
    }

    private record Existing(String requestHash, UUID resourceId) {
    }
}
