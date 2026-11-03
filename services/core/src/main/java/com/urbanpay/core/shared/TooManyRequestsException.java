package com.urbanpay.core.shared;

import java.time.Duration;

import org.springframework.http.HttpStatus;

/** 429 com o cabeçalho Retry-After, para o cliente saber quando pode tentar de novo. */
public class TooManyRequestsException extends DomainException {

    private final Duration retryAfter;

    public TooManyRequestsException(String code, String message, Duration retryAfter) {
        super(HttpStatus.TOO_MANY_REQUESTS, code, message);
        this.retryAfter = retryAfter;
    }

    public long retryAfterSeconds() {
        // Arredonda para cima: "0 segundos" faria o cliente tentar de novo imediatamente.
        return Math.max(1, (retryAfter.toMillis() + 999) / 1000);
    }
}