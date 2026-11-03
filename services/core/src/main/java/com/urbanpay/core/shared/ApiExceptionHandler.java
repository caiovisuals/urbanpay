package com.urbanpay.core.shared;

import java.net.URI;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/** Todas as respostas de erro seguem o formato Problem Details (RFC 9457). */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);
    private static final String TYPE_PREFIX = "https://urbanpay.dev/problems/";

    @ExceptionHandler(DomainException.class)
    ProblemDetail handleDomain(DomainException ex) {
        var problem = ProblemDetail.forStatusAndDetail(ex.status(), ex.getMessage());
        problem.setType(URI.create(TYPE_PREFIX + ex.code()));
        problem.setTitle(ex.code());
        problem.setProperty("code", ex.code());
        return problem;
    }

    @ExceptionHandler(TooManyRequestsException.class)
    ResponseEntity<ProblemDetail> handleTooManyRequests(TooManyRequestsException ex) {
        return ResponseEntity.status(ex.status())
                .header(HttpHeaders.RETRY_AFTER, Long.toString(ex.retryAfterSeconds()))
                .body(handleDomain(ex));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
        return handleDomain(new DomainException(HttpStatus.BAD_REQUEST, "invalid_request", ex.getMessage()));
    }

    /** Conflito de versão (@Version): outra requisição alterou o mesmo registro ao mesmo tempo. */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    ProblemDetail handleOptimisticLock(OptimisticLockingFailureException ex) {
        return handleDomain(new DomainException(HttpStatus.CONFLICT, "concurrent_modification",
                "A operação conflitou com outra simultânea. Tente novamente."));
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception ex) {
        // Detalhes internos ficam no log, nunca na resposta.
        log.error("Unexpected error", ex);
        return handleDomain(new DomainException(HttpStatus.INTERNAL_SERVER_ERROR, "internal_error",
                "Erro inesperado. Tente novamente."));
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, String> fields = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(e -> e.getField(),
                        e -> String.valueOf(e.getDefaultMessage()), (a, b) -> a));
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Dados inválidos");
        problem.setType(URI.create(TYPE_PREFIX + "validation_error"));
        problem.setTitle("validation_error");
        problem.setProperty("code", "validation_error");
        problem.setProperty("fields", fields);
        return ResponseEntity.badRequest().body(problem);
    }
}
