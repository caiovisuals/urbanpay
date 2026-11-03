package com.urbanpay.core.identity;

import java.util.UUID;

/**
 * API pública do módulo identity para confirmar operações sensíveis (transferências)
 * com o PIN transacional. Após N tentativas erradas o PIN fica bloqueado temporariamente.
 */
public interface PinVerifier {

    /** Lança {@link com.urbanpay.core.shared.DomainException} se o PIN for inválido ou estiver bloqueado. */
    void verify(UUID userId, String pin);
}
