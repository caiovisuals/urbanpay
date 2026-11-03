package com.urbanpay.core.identity.internal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.ResultActions;

import com.urbanpay.core.ApiTestSupport;

@TestPropertySource(properties = {
        "urbanpay.security.rate-limit.login-per-ip-per-minute=5",
        "urbanpay.security.rate-limit.register-per-ip-per-hour=2",
        "urbanpay.security.rate-limit.login-failures-per-email=3"})
class AuthRateLimitTests extends ApiTestSupport {

    @Test
    void blocksLoginAfterTooManyFailuresForTheSameEmailEvenFromDifferentIps() throws Exception {
        var user = register("10.0.0.1");

        // Ataque distribuído: cada tentativa vem de um IP diferente.
        for (int i = 0; i < 3; i++) {
            login(user.email(), "Errada#123", "10.1.0." + i).andExpect(status().isUnauthorized());
        }
        // A partir daqui nem a senha certa passa, e a resposta diz quando tentar de novo.
        login(user.email(), PASSWORD, "10.1.0.99")
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.code").value("too_many_login_attempts"));
    }

    @Test
    void successfulLoginResetsTheFailureCounter() throws Exception {
        var user = register("10.0.0.2");

        login(user.email(), "Errada#123", "10.2.0.1").andExpect(status().isUnauthorized());
        login(user.email(), "Errada#123", "10.2.0.2").andExpect(status().isUnauthorized());
        login(user.email(), PASSWORD, "10.2.0.3").andExpect(status().isOk());

        // Contador zerado: mais duas falhas ainda não bloqueiam.
        login(user.email(), "Errada#123", "10.2.0.4").andExpect(status().isUnauthorized());
        login(user.email(), "Errada#123", "10.2.0.5").andExpect(status().isUnauthorized());
        login(user.email(), PASSWORD, "10.2.0.6").andExpect(status().isOk());
    }

    @Test
    void limitsLoginAttemptsPerIpAcrossDifferentAccounts() throws Exception {
        // Credential stuffing: um IP testando vários e-mails diferentes.
        for (int i = 0; i < 5; i++) {
            login("vitima" + i + "@example.com", "Qualquer#1", "10.3.0.1").andExpect(status().isUnauthorized());
        }
        login("vitima9@example.com", "Qualquer#1", "10.3.0.1").andExpect(status().isTooManyRequests());
        // Outro IP não é afetado.
        login("vitima9@example.com", "Qualquer#1", "10.3.0.2").andExpect(status().isUnauthorized());
    }

    @Test
    void limitsRegistrationsPerIp() throws Exception {
        register("10.4.0.1");
        register("10.4.0.1");
        mvc.perform(post("/api/v1/auth/register").with(fromIp("10.4.0.1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"Bot","email":"bot@example.com","phone":"11999990000","cpf":"%s",
                                 "birthDate":"1990-01-01","password":"%s","pin":"%s"}
                                """.formatted(nextCpf(), PASSWORD, PIN)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("too_many_registrations"));
    }

    private ResultActions login(String email, String password, String ip) throws Exception {
        return mvc.perform(post("/api/v1/auth/login").with(fromIp(ip))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)));
    }
}