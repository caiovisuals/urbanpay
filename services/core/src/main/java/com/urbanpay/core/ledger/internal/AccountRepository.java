package com.urbanpay.core.ledger.internal;

import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

interface AccountRepository extends JpaRepository<Account, UUID> {

    @Query("select a from Account a where a.ownerId = :ownerId and a.kind = com.urbanpay.core.ledger.internal.Account.Kind.CUSTOMER")
    Optional<Account> findCustomerAccount(UUID ownerId);

    /**
     * Só o id, sem carregar a entidade: se a entidade entrasse no contexto de persistência
     * antes do SELECT FOR UPDATE, o Hibernate devolveria a instância já carregada (com saldo
     * possivelmente desatualizado) em vez do estado lido sob lock.
     */
    @Query("select a.id from Account a where a.ownerId = :ownerId and a.kind = com.urbanpay.core.ledger.internal.Account.Kind.CUSTOMER")
    Optional<UUID> findCustomerAccountId(UUID ownerId);

    @Query("select a.kind from Account a where a.id = :id")
    Optional<Account.Kind> findKind(UUID id);

    /**
     * SELECT ... FOR UPDATE ordenado por id. Travar sempre na mesma ordem evita deadlock
     * quando A->B e B->A acontecem ao mesmo tempo.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.id in :ids order by a.id")
    List<Account> lockAllOrdered(Collection<UUID> ids);
}