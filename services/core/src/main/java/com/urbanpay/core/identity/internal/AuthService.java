package com.urbanpay.core.identity.internal;

import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.urbanpay.core.identity.UserRegistered;
import com.urbanpay.core.shared.DomainException;

@Service
class AuthService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokens;
    private final ApplicationEventPublisher events;
    private final Clock clock;
    /** Hash fixo usado quando o e-mail não existe, para o tempo de resposta não revelar isso. */
    private final String dummyHash;

    AuthService(UserRepository users, PasswordEncoder passwordEncoder, TokenService tokens,
                ApplicationEventPublisher events, Clock clock) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.tokens = tokens;
        this.events = events;
        this.clock = clock;
        this.dummyHash = passwordEncoder.encode("dummy-password-for-timing");
    }

    @Transactional
    TokenService.TokenPair register(RegisterRequest req) {
        RegistrationRules.validate(req, LocalDate.now(clock));

        var email = RegistrationRules.normalizeEmail(req.email());
        var cpf = RegistrationRules.normalizeDigits(req.cpf());
        if (users.existsByEmail(email) || users.existsByCpf(cpf)) {
            throw alreadyRegistered();
        }

        var user = new User(req.fullName().trim(), email, RegistrationRules.normalizeDigits(req.phone()), cpf,
                req.birthDate(), passwordEncoder.encode(req.password()), passwordEncoder.encode(req.pin()),
                clock.instant());
        try {
            users.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            // Corrida entre dois cadastros simultâneos: a constraint UNIQUE do banco decide.
            throw alreadyRegistered();
        }

        // Listener síncrono do ledger abre a conta na MESMA transação: ou cria tudo, ou nada.
        events.publishEvent(new UserRegistered(user.getId(), clock.instant()));
        return tokens.issueFor(user.getId());
    }

    @Transactional
    TokenService.TokenPair login(LoginRequest req) {
        var user = users.findByEmail(RegistrationRules.normalizeEmail(req.email()));
        var hash = user.map(User::getPasswordHash).orElse(dummyHash);
        boolean matches = passwordEncoder.matches(req.password(), hash);
        if (user.isEmpty() || !matches) {
            // Mensagem única: não revelamos se o e-mail existe (evita enumeração de usuários).
            throw new DomainException(HttpStatus.UNAUTHORIZED, "invalid_credentials", "E-mail ou senha inválidos");
        }
        return tokens.issueFor(user.get().getId());
    }

    @Transactional(readOnly = true)
    MeResponse me(UUID userId) {
        var user = users.findById(userId)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "user_not_found", "Usuário não encontrado"));
        return new MeResponse(user.getId(), user.getFullName(), user.getEmail(), user.getPhone(),
                maskCpf(user.getCpf()), user.getCreatedAt());
    }

    static String maskCpf(String cpf) {
        return "***." + cpf.substring(3, 6) + "." + cpf.substring(6, 9) + "-**";
    }

    private static DomainException alreadyRegistered() {
        // Mensagem genérica: não dizemos qual dado (e-mail ou CPF) já existe.
        return new DomainException(HttpStatus.CONFLICT, "already_registered",
                "Não foi possível concluir o cadastro com esses dados");
    }
}
