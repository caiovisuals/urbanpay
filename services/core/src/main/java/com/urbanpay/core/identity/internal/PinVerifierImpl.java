package com.urbanpay.core.identity.internal;

import java.time.Clock;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.urbanpay.core.identity.PinVerifier;
import com.urbanpay.core.shared.DomainException;

@Service
class PinVerifierImpl implements PinVerifier {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final SecurityProperties.Pin props;
    private final Clock clock;

    PinVerifierImpl(UserRepository users, PasswordEncoder passwordEncoder, SecurityProperties props, Clock clock) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.props = props.pin();
        this.clock = clock;
    }

    /**
     * Roda em transação PRÓPRIA (REQUIRES_NEW) e sem rollback em erro de domínio:
     * a tentativa errada precisa ser contabilizada mesmo que a transferência seja abortada.
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = DomainException.class)
    public void verify(UUID userId, String pin) {
        var now = clock.instant();
        var user = users.findById(userId)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "user_not_found", "Usuário não encontrado"));

        if (user.isPinLocked(now)) {
            throw new DomainException(HttpStatus.LOCKED, "pin_locked",
                    "PIN bloqueado por excesso de tentativas. Tente novamente mais tarde.");
        }
        if (pin == null || !passwordEncoder.matches(pin, user.getPinHash())) {
            user.registerPinFailure(props.maxAttempts(), now.plus(props.lockDuration()));
            throw new DomainException(HttpStatus.UNPROCESSABLE_CONTENT, "invalid_pin", "PIN inválido");
        }
        user.registerPinSuccess();
    }
}