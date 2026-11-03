package com.urbanpay.core.identity.internal;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

record RegisterRequest(
        @NotBlank @Size(max = 150) String fullName,
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Pattern(regexp = "^\\+?[\\d ()-]{10,20}$", message = "telefone inválido") String phone,
        @NotBlank @Size(max = 14) String cpf,
        @NotNull @Past LocalDate birthDate,
        @NotBlank @Size(max = 128) String password,
        @NotBlank String pin) {

    @Override
    public String toString() {
        // Evita vazar senha/PIN/CPF se o objeto for logado por engano.
        return "RegisterRequest[email=" + email + "]";
    }
}

record LoginRequest(@NotBlank @Email String email, @NotBlank @Size(max = 128) String password) {

    @Override
    public String toString() {
        return "LoginRequest[email=" + email + "]";
    }
}

record RefreshRequest(@NotBlank String refreshToken) {

    @Override
    public String toString() {
        return "RefreshRequest[***]";
    }
}

record TokenResponse(String tokenType, String accessToken, Instant accessTokenExpiresAt,
                     String refreshToken, Instant refreshTokenExpiresAt) {

    static TokenResponse from(TokenService.TokenPair pair) {
        return new TokenResponse("Bearer", pair.accessToken(), pair.accessTokenExpiresAt(),
                pair.refreshToken(), pair.refreshTokenExpiresAt());
    }
}

record MeResponse(UUID id, String fullName, String email, String phone, String cpfMasked, Instant createdAt) {
}
