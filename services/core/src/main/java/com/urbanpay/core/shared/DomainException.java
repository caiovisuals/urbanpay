package com.urbanpay.core.shared;

import org.springframework.http.HttpStatus;

/**
 * Erro de regra de negócio. Vira uma resposta RFC 9457 (application/problem+json)
 * com um {@code code} estável que o front pode usar para decidir o que mostrar.
 */
public class DomainException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public DomainException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus status() {
        return status;
    }

    public String code() {
        return code;
    }
}