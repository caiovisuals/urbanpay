package com.urbanpay.core.identity.internal;

import java.time.Duration;
import java.util.function.Supplier;

import org.springframework.stereotype.Component;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.urbanpay.core.shared.TooManyRequestsException;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;

/**
 * Rate limiting de autenticação com token bucket (Bucket4j).
 * <ul>
 *   <li>Por IP: limita tentativas de login e de cadastro, com ou sem sucesso.</li>
 *   <li>Por e-mail: limita FALHAS de login; um login bem-sucedido zera o contador.</li>
 * </ul>
 * Os buckets ficam em memória (Caffeine), o que vale para uma instância. Com várias
 * réplicas, troque por um {@code ProxyManager} distribuído do Bucket4j (Redis), mantendo
 * esta mesma classe como fachada.
 */
@Component
class AuthThrottle {

    private final SecurityProperties.RateLimit limits;
    private final Cache<String, Bucket> buckets = Caffeine.newBuilder()
            .maximumSize(200_000)
            .expireAfterAccess(Duration.ofHours(2))
            .build();

    AuthThrottle(SecurityProperties props) {
        this.limits = props.rateLimit();
    }

    void checkLogin(String ip, String email) {
        consumeOrThrow("login-ip:" + ip,
                () -> bucket(limits.loginPerIpPerMinute(), Duration.ofMinutes(1)),
                "too_many_login_attempts", "Muitas tentativas de login. Aguarde e tente novamente.");

        var failures = failureBucket(email);
        var probe = failures.estimateAbilityToConsume(1);
        if (!probe.canBeConsumed()) {
            throw new TooManyRequestsException("too_many_login_attempts",
                    "Muitas tentativas de login. Aguarde e tente novamente.",
                    Duration.ofNanos(probe.getNanosToWaitForRefill()));
        }
    }

    void loginFailed(String email) {
        failureBucket(email).tryConsume(1);
    }

    void loginSucceeded(String email) {
        buckets.invalidate("login-email:" + email);
    }

    void checkRegister(String ip) {
        consumeOrThrow("register-ip:" + ip,
                () -> bucket(limits.registerPerIpPerHour(), Duration.ofHours(1)),
                "too_many_registrations", "Muitos cadastros a partir desta rede. Tente mais tarde.");
    }

    private Bucket failureBucket(String email) {
        return buckets.get("login-email:" + email,
                k -> bucket(limits.loginFailuresPerEmail(), limits.loginFailureWindow()));
    }

    private void consumeOrThrow(String key, Supplier<Bucket> factory, String code, String message) {
        var probe = buckets.get(key, k -> factory.get()).tryConsumeAndReturnRemaining(1);
        if (!probe.isConsumed()) {
            throw new TooManyRequestsException(code, message, Duration.ofNanos(probe.getNanosToWaitForRefill()));
        }
    }

    /** Capacidade cheia no início; recarrega aos poucos ao longo da janela (refill "greedy"). */
    private static Bucket bucket(int capacity, Duration window) {
        return Bucket.builder()
                .addLimit(Bandwidth.builder().capacity(capacity).refillGreedy(capacity, window).build())
                .build();
    }
}