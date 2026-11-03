package com.urbanpay.core.identity.internal;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.jayway.jsonpath.JsonPath;
import com.urbanpay.core.ApiTestSupport;

class AuthFlowTests extends ApiTestSupport {

    @Autowired
    JdbcClient jdbc;

    @Test
    void registerOpensAccountAndReturnsUsableToken() throws Exception {
        var user = register();

        mvc.perform(get("/api/v1/me").header("Authorization", user.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(user.email()))
                .andExpect(jsonPath("$.cpfMasked").value(containsString("***")))
                .andExpect(content().string(not(containsString("passwordHash"))));

        mvc.perform(get("/api/v1/accounts/me").header("Authorization", user.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value("0.00"))
                .andExpect(jsonPath("$.currency").value("BRL"));
    }

    @Test
    void duplicateEmailIsRejectedWithGenericMessage() throws Exception {
        var user = register();
        var body = """
                {"fullName":"Outra Pessoa","email":"%s","phone":"11988887777","cpf":"%s",
                 "birthDate":"1990-01-01","password":"%s","pin":"%s"}
                """.formatted(user.email().toUpperCase(), nextCpf(), PASSWORD, PIN);
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("already_registered"));
    }

    @Test
    void rejectsUnderageWeakPasswordAndWeakPin() throws Exception {
        var template = """
                {"fullName":"Fulano","email":"x-%s@example.com","phone":"11988887777","cpf":"%s",
                 "birthDate":"%s","password":"%s","pin":"%s"}
                """;
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(template.formatted(1, nextCpf(), java.time.LocalDate.now().minusYears(17), PASSWORD, PIN)))
                .andExpect(status().isUnprocessableContent()).andExpect(jsonPath("$.code").value("underage"));
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(template.formatted(2, nextCpf(), "1990-01-01", "fraca", PIN)))
                .andExpect(status().isUnprocessableContent()).andExpect(jsonPath("$.code").value("weak_password"));
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(template.formatted(3, nextCpf(), "1990-01-01", PASSWORD, "123456")))
                .andExpect(status().isUnprocessableContent()).andExpect(jsonPath("$.code").value("weak_pin"));
    }

    @Test
    void loginDoesNotRevealWhetherEmailExists() throws Exception {
        var user = register();
        var wrongPassword = """
                {"email":"%s","password":"Errada#123"}""".formatted(user.email());
        var unknownEmail = """
                {"email":"ninguem@example.com","password":"Errada#123"}""";

        for (var body : new String[] {wrongPassword, unknownEmail}) {
            mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("invalid_credentials"));
        }

        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}""".formatted(user.email(), PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString());
    }

    @Test
    void refreshTokenRotatesAndReuseAfterGraceRevokesTheWholeFamily() throws Exception {
        var user = register();

        var rotated = refresh(user.refreshToken(), 200);
        String newRefresh = JsonPath.read(rotated, "$.refreshToken");
        assertThat(newRefresh).isNotEqualTo(user.refreshToken());

        // Simula o token antigo sendo reapresentado bem depois da rotação (ex.: foi roubado).
        jdbc.sql("UPDATE refresh_tokens SET rotated_at = now() - interval '5 minutes' WHERE rotated_at IS NOT NULL "
                + "AND user_id = :uid").param("uid", user.userId()).update();

        // Rejeita e revoga a família...
        refresh(user.refreshToken(), 401);
        // ...inclusive o token novo, que estava válido.
        refresh(newRefresh, 401);
    }

    @Test
    void parallelRefreshWithinGraceWindowKeepsTheSessionAlive() throws Exception {
        var user = register();

        String first = JsonPath.read(refresh(user.refreshToken(), 200), "$.refreshToken");
        // Segunda chamada paralela com o MESMO token, logo em seguida: tolerada.
        String second = JsonPath.read(refresh(user.refreshToken(), 200), "$.refreshToken");

        refresh(first, 200);
        refresh(second, 200);
    }

    @Test
    void reuseWithinGraceIsRejectedAfterLogout() throws Exception {
        var user = register();
        String rotated = JsonPath.read(refresh(user.refreshToken(), 200), "$.refreshToken");
        mvc.perform(post("/api/v1/auth/logout").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"%s\"}".formatted(rotated)))
                .andExpect(status().isNoContent());

        refresh(user.refreshToken(), 401);
    }

    @Test
    void logoutRevokesRefreshToken() throws Exception {
        var user = register();
        mvc.perform(post("/api/v1/auth/logout").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"%s\"}".formatted(user.refreshToken())))
                .andExpect(status().isNoContent());
        refresh(user.refreshToken(), 401);
    }

    @Test
    void protectedEndpointsRequireValidToken() throws Exception {
        mvc.perform(get("/api/v1/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/me").header("Authorization", "Bearer forjado.token.aqui"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void publishesOnlyThePublicKey() throws Exception {
        mvc.perform(get("/.well-known/jwks.json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keys[0].kty").value("RSA"))
                .andExpect(jsonPath("$.keys[0].n").isString())
                .andExpect(jsonPath("$.keys[0].d").doesNotExist());
    }

    private String refresh(String token, int expectedStatus) throws Exception {
        return mvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"%s\"}".formatted(token)))
                .andExpect(status().is(expectedStatus))
                .andReturn().getResponse().getContentAsString();
    }
}