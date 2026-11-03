package com.urbanpay.core.identity.internal;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1")
class AuthController {

    private final AuthService auth;
    private final TokenService tokens;

    AuthController(AuthService auth, TokenService tokens) {
        this.auth = auth;
        this.tokens = tokens;
    }

    @PostMapping("/auth/register")
    @ResponseStatus(HttpStatus.CREATED)
    TokenResponse register(@Valid @RequestBody RegisterRequest req, HttpServletRequest http) {
        return TokenResponse.from(auth.register(req, http.getRemoteAddr()));
    }

    @PostMapping("/auth/login")
    TokenResponse login(@Valid @RequestBody LoginRequest req, HttpServletRequest http) {
        return TokenResponse.from(auth.login(req, http.getRemoteAddr()));
    }

    @PostMapping("/auth/refresh")
    TokenResponse refresh(@Valid @RequestBody RefreshRequest req) {
        return TokenResponse.from(tokens.rotate(req.refreshToken()));
    }

    @PostMapping("/auth/logout")
    ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest req) {
        tokens.revoke(req.refreshToken());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    MeResponse me(@AuthenticationPrincipal Jwt jwt) {
        return auth.me(UUID.fromString(jwt.getSubject()));
    }
}