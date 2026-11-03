package com.urbanpay.core.ledger.internal;

import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.urbanpay.core.shared.Money;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;

/** Só existe no perfil "dev": permite colocar dinheiro fictício na conta para testar. */
@Profile("dev")
@RestController
@RequestMapping("/api/v1/dev/deposits")
class DevDepositController {

    private final DepositService deposits;

    DevDepositController(DepositService deposits) {
        this.deposits = deposits;
    }

    @PostMapping
    AccountResponse deposit(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader("Idempotency-Key") @Pattern(regexp = TransferController.IDEMPOTENCY_KEY_PATTERN) String key,
            @Valid @RequestBody DepositRequest req) {
        return deposits.deposit(UUID.fromString(jwt.getSubject()), key, Money.of(req.amount()));
    }
}
