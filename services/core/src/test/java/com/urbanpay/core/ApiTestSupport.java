package com.urbanpay.core;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.jayway.jsonpath.JsonPath;

/**
 * Todos os testes compartilham o IP 127.0.0.1 do MockMvc, então os limites por IP ficam altos
 * aqui. O rate limit é testado em {@code AuthRateLimitTests}, com limites baixos.
 */
@SpringBootTest(properties = {
        "urbanpay.security.rate-limit.login-per-ip-per-minute=100000",
        "urbanpay.security.rate-limit.register-per-ip-per-hour=100000"})
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Import(TestcontainersConfiguration.class)
public abstract class ApiTestSupport {

    /** CPFs válidos gerados para não colidir entre testes. */
    private static final AtomicInteger CPF_SEQ = new AtomicInteger(100_000_000);

    public static final String PASSWORD = "S3nh@Forte!";
    public static final String PIN = "482915";

    @Autowired
    protected MockMvc mvc;

    protected record TestUser(String email, String accessToken, String refreshToken, UUID userId) {

        public String bearer() {
            return "Bearer " + accessToken;
        }
    }

    protected TestUser register() throws Exception {
        return register("127.0.0.1");
    }

    protected TestUser register(String clientIp) throws Exception {
        var email = "user-" + UUID.randomUUID() + "@example.com";
        var body = """
                {"fullName":"Fulano de Tal","email":"%s","phone":"+55 11 99999-0000",
                 "cpf":"%s","birthDate":"1990-05-10","password":"%s","pin":"%s"}
                """.formatted(email, nextCpf(), PASSWORD, PIN);
        var json = mvc.perform(post("/api/v1/auth/register").with(fromIp(clientIp))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String access = JsonPath.read(json, "$.accessToken");
        String refresh = JsonPath.read(json, "$.refreshToken");
        var subject = new String(java.util.Base64.getUrlDecoder().decode(access.split("\\.")[1]));
        UUID userId = UUID.fromString(JsonPath.read(subject, "$.sub"));
        return new TestUser(email, access, refresh, userId);
    }

    protected static RequestPostProcessor fromIp(String ip) {
        return request -> {
            request.setRemoteAddr(ip);
            return request;
        };
    }

    public static String nextCpf() {
        var base = String.valueOf(CPF_SEQ.getAndIncrement());
        return base + digit(base, 10) + digit(base + digit(base, 10), 11);
    }

    private static int digit(String partial, int weight) {
        int sum = 0;
        for (int i = 0; i < partial.length(); i++) {
            sum += (partial.charAt(i) - '0') * (weight - i);
        }
        int rest = (sum * 10) % 11;
        return rest == 10 ? 0 : rest;
    }

    protected static String newKey() {
        return "key-" + UUID.randomUUID();
    }
}
