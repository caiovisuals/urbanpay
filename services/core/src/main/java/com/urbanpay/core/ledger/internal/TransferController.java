package com.urbanpay.core.ledger.internal;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;

@RestController
@RequestMapping("/api/v1/transfers")
class TransferController {

    static final String IDEMPOTENCY_KEY_PATTERN = "^[A-Za-z0-9_-]{8,100}$";

    private final TransferService transfers;

    TransferController(TransferService transfers) {
        this.transfers = transfers;
    }

    @PostMapping
    ResponseEntity<TransferResponse> create(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader("Idempotency-Key") @Pattern(regexp = IDEMPOTENCY_KEY_PATTERN) String idempotencyKey,
            @Valid @RequestBody TransferRequest req) {
        var result = transfers.transfer(UUID.fromString(jwt.getSubject()), idempotencyKey, req);
        return ResponseEntity.status(result.replayed() ? HttpStatus.OK : HttpStatus.CREATED)
                .header("Idempotent-Replayed", Boolean.toString(result.replayed()))
                .body(result.transfer());
    }

    @GetMapping("/{id}")
    TransferResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return transfers.find(id, UUID.fromString(jwt.getSubject()));
    }
}
