package com.urbanpay.core.ledger.internal;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface PostingRepository extends JpaRepository<Posting, Long> {

    List<Posting> findByEntryId(UUID entryId);

    /** Extrato com paginação por cursor (keyset): estável e rápido mesmo com milhões de linhas. */
    @Query("""
            select new com.urbanpay.core.ledger.internal.StatementLine(
                p.id, p.entryId, e.type, e.description, p.amountCents, p.balanceAfterCents, p.createdAt)
            from Posting p join JournalEntry e on e.id = p.entryId
            where p.accountId = :accountId and p.id < :before
            order by p.id desc
            """)
    List<StatementLine> statement(UUID accountId, long before, Limit limit);
}