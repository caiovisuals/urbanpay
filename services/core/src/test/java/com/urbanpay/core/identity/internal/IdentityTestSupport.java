package com.urbanpay.core.identity.internal;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.context.ApplicationContext;

/** Ponte para testes de outros módulos criarem usuários sem passar pela camada HTTP. */
public final class IdentityTestSupport {

    private IdentityTestSupport() {
    }

    public static UUID registerUser(ApplicationContext ctx, String cpf, String pin) {
        var email = "prop-" + UUID.randomUUID() + "@example.com";
        ctx.getBean(AuthService.class).register(new RegisterRequest("Teste Propriedade", email, "11999990000",
                cpf, LocalDate.of(1990, 1, 1), "S3nh@Forte!", pin), "127.0.0.1");
        return ctx.getBean(UserRepository.class).findByEmail(email).orElseThrow().getId();
    }
}
