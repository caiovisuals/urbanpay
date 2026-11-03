package com.urbanpay.core.identity.internal;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;

/** Publica apenas a chave PÚBLICA, para que outros serviços validem os tokens. */
@RestController
class JwksController {

    private final JWKSet publicJwks;

    JwksController(RSAKey rsaKey) {
        this.publicJwks = new JWKSet(rsaKey.toPublicJWK());
    }

    @GetMapping("/.well-known/jwks.json")
    Map<String, Object> jwks() {
        return publicJwks.toJSONObject();
    }
}