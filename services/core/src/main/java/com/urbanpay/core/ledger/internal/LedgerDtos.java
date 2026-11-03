package com.urbanpay.core.ledger.internal;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

record TransferRequest(
        @NotNull UUID toAccountId,
        @NotNull @DecimalMin(value = "0.01") @DecimalMax("1000000.00") @Digits(integer = 9, fraction = 2) BigDecimal amount,
        @Size(max = 140) String description,
        @NotBlank String pin) {

    @Override
    public String toString() {
        return "TransferRequest[to=" + toAccountId + ", amount=" + amount + "]";
    }
}

record DepositRequest(
        @NotNull @DecimalMin(value = "0.01") @DecimalMax("1000000.00") @Digits(integer = 9, fraction = 2) BigDecimal amount) {
}

/** Valores monetários trafegam como string decimal ("10.50") para o JS não converter em float. */
record TransferResponse(UUID id, String status, UUID fromAccountId, UUID toAccountId, String amount,
                        String description, Instant createdAt) {
}

record AccountResponse(UUID id, String currency, String balance) {
}

record StatementResponse(List<Item> items, Long nextCursor) {

    record Item(long id, UUID entryId, String type, String description, String amount, String balanceAfter,
                Instant createdAt) {
    }
}