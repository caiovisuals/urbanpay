package com.urbanpay.core.identity.internal;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.urbanpay.core.shared.DomainException;
import com.urbanpay.core.shared.Hashing;
import com.urbanpay.core.shared.Ids;

@Service
class TokenService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final JwtEncoder encoder;
    private final RefreshTokenRepository refreshTokens;
    private final SecurityProperties.Jwt props;
    private final Clock clock;

    TokenService(JwtEncoder encoder, RefreshTokenRepository refreshTokens, SecurityProperties props, Clock clock) {
        this.encoder = encoder;
        this.refreshTokens = refreshTokens;
        this.props = props.jwt();
        this.clock = clock;
    }

    /** Emite um par novo (nova família de refresh tokens), usado no login/cadastro. */
    @Transactional
    TokenPair issueFor(UUID userId) {
        return issue(userId, Ids.newId());
    }

    /**
     * Rotação de refresh token: cada token só pode ser usado uma vez.
     * Reapresentar um token já usado indica roubo, e a família inteira é revogada
     * (o atacante e a vítima precisam logar de novo, mas o atacante perde o acesso).
     */
    @Transactional(noRollbackFor = DomainException.class)
    TokenPair rotate(String rawRefreshToken) {
        var now = clock.instant();
        var token = refreshTokens.findByHashForUpdate(Hashing.sha256Hex(rawRefreshToken))
                .orElseThrow(TokenService::invalidRefresh);

        if (token.isExpired(now)) {
            throw invalidRefresh();
        }
        if (token.isRevoked()) {
            // Requisições paralelas legítimas (ex.: duas abas/chamadas renovando juntas) chegam
            // com o mesmo token poucos segundos depois da rotação: toleramos, se a sessão segue ativa.
            if (token.wasRotatedWithin(props.refreshReuseGrace(), now)
                    && refreshTokens.isFamilyActive(token.getFamilyId(), now)) {
                return issue(token.getUserId(), token.getFamilyId());
            }
            refreshTokens.revokeFamily(token.getFamilyId(), now);
            throw invalidRefresh();
        }
        token.rotate(now);
        return issue(token.getUserId(), token.getFamilyId());
    }

    @Transactional(propagation = Propagation.REQUIRED)
    void revoke(String rawRefreshToken) {
        refreshTokens.findByHashForUpdate(Hashing.sha256Hex(rawRefreshToken))
                .ifPresent(t -> refreshTokens.revokeFamily(t.getFamilyId(), clock.instant()));
    }

    private TokenPair issue(UUID userId, UUID familyId) {
        var now = clock.instant();
        var accessExpiresAt = now.plus(props.accessTokenTtl());
        var claims = JwtClaimsSet.builder()
                .issuer(props.issuer())
                .audience(List.of(props.audience()))
                .subject(userId.toString())
                .issuedAt(now)
                .notBefore(now)
                .expiresAt(accessExpiresAt)
                .id(Ids.newId().toString())
                .claim("scope", "user")
                .build();
        var header = JwsHeader.with(SignatureAlgorithm.RS256).build();
        var accessToken = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

        var rawRefresh = randomToken();
        var refreshExpiresAt = now.plus(props.refreshTokenTtl());
        refreshTokens.save(new RefreshToken(userId, familyId, Hashing.sha256Hex(rawRefresh), refreshExpiresAt, now));

        return new TokenPair(accessToken, accessExpiresAt, rawRefresh, refreshExpiresAt);
    }

    private static String randomToken() {
        var bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static DomainException invalidRefresh() {
        return new DomainException(HttpStatus.UNAUTHORIZED, "invalid_refresh_token", "Sessão expirada. Faça login novamente.");
    }

    record TokenPair(String accessToken, Instant accessTokenExpiresAt,
                     String refreshToken, Instant refreshTokenExpiresAt) {
    }
}