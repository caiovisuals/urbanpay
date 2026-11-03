package com.urbanpay.core.ledger.internal;

import java.time.Instant;
import java.util.UUID;

record StatementLine(long postingId, UUID entryId, JournalEntry.Type type, String description,
                     long amountCents, long balanceAfterCents, Instant createdAt) {
}
