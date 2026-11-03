package com.urbanpay.core.identity.internal;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("urbanpay.security")
record SecurityProperties(Jwt jwt, Pin pin, RateLimit rateLimit) {

    record Jwt(String issuer, String audience, Duration accessTokenTtl, Duration refreshTokenTtl,
               Duration refreshReuseGrace,
               String privateKey, String publicKey) {
    }

    record Pin(int maxAttempts, Duration lockDuration) {
    }

    record RateLimit(int loginPerIpPerMinute, int registerPerIpPerHour,
                     int loginFailuresPerEmail, Duration loginFailureWindow) {
    }
}