package com.urbanpay.core.ledger.internal;

import java.util.UUID;

import org.springframework.data.domain.Limit;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.urbanpay.core.shared.DomainException;
import com.urbanpay.core.shared.Money;

@RestController
@RequestMapping("/api/v1/accounts")
class AccountController {

    private static final int MAX_PAGE = 100;

    private final AccountRepository accounts;
    private final PostingRepository postings;

    AccountController(AccountRepository accounts, PostingRepository postings) {
        this.accounts = accounts;
        this.postings = postings;
    }

    @GetMapping("/me")
    @Transactional(readOnly = true)
    AccountResponse myAccount(@AuthenticationPrincipal Jwt jwt) {
        var account = myAccountOrThrow(jwt);
        return new AccountResponse(account.getId(), account.getCurrency(), account.balance().toString());
    }

    @GetMapping("/me/statement")
    @Transactional(readOnly = true)
    StatementResponse statement(@AuthenticationPrincipal Jwt jwt,
                                @RequestParam(required = false) Long cursor,
                                @RequestParam(defaultValue = "20") int limit) {
        var account = myAccountOrThrow(jwt);
        int size = Math.clamp(limit, 1, MAX_PAGE);
        var lines = postings.statement(account.getId(), cursor == null ? Long.MAX_VALUE : cursor, Limit.of(size));
        var items = lines.stream().map(l -> new StatementResponse.Item(l.postingId(), l.entryId(), l.type().name(),
                l.description(), Money.ofCents(l.amountCents()).toString(),
                Money.ofCents(l.balanceAfterCents()).toString(), l.createdAt())).toList();
        Long next = lines.size() == size ? lines.getLast().postingId() : null;
        return new StatementResponse(items, next);
    }

    private Account myAccountOrThrow(Jwt jwt) {
        return accounts.findCustomerAccount(UUID.fromString(jwt.getSubject()))
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "account_not_found", "Conta não encontrada"));
    }
}