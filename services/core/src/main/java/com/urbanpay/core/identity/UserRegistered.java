package com.urbanpay.core.identity;

import java.time.Instant;
import java.util.UUID;

/**
 * Evento publicado quando um cliente conclui o cadastro.
 * O módulo ledger escuta este evento para abrir a conta corrente.
 */
public record UserRegistered(UUID userId, Instant occurredAt) {
}