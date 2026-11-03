package com.urbanpay.core.identity.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;

import com.urbanpay.core.TestcontainersConfiguration;

/**
 * Sobe o Tomcat de verdade (o MockMvc não processa X-Forwarded-For) para provar que o
 * rate limit usa o IP do usuário repassado pelo BFF, e não o IP do próprio BFF.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "urbanpay.security.rate-limit.login-per-ip-per-minute=3",
        "urbanpay.security.rate-limit.register-per-ip-per-hour=100"})
@Import(TestcontainersConfiguration.class)
class ForwardedClientIpTests {

    @LocalServerPort
    int port;

    private final HttpClient http = HttpClient.newHttpClient();

    @Test
    void eachForwardedClientIpHasItsOwnLimit() throws Exception {
        for (int i = 0; i < 3; i++) {
            assertThat(login("203.0.113.10", "a" + i + "@example.com")).isEqualTo(401);
        }
        assertThat(login("203.0.113.10", "a9@example.com")).isEqualTo(429);

        // Mesmo BFF (127.0.0.1), outro usuário final: não é afetado pelo bloqueio do primeiro.
        assertThat(login("198.51.100.20", "b@example.com")).isEqualTo(401);
    }

    private int login(String forwardedFor, String email) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/v1/auth/login"))
                .header("Content-Type", "application/json")
                .header("X-Forwarded-For", forwardedFor)
                .POST(HttpRequest.BodyPublishers.ofString(
                        "{\"email\":\"" + email + "\",\"password\":\"Qualquer#1\"}"))
                .build();
        return http.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
    }
}