package com.urbanpay.core.identity.internal;

import java.time.LocalDate;
import java.time.Period;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;

import com.urbanpay.core.shared.DomainException;

/** Regras de cadastro (KYC básico) que vão além da validação de formato do Bean Validation. */
final class RegistrationRules {

    private static final Pattern STRONG_PASSWORD =
            Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[\\W_]).{8,128}$");
    private static final Pattern PIN = Pattern.compile("^\\d{6}$");

    private RegistrationRules() {
    }

    static String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

    static String normalizeDigits(String value) {
        return value.replaceAll("\\D", "");
    }

    static void validate(RegisterRequest req, LocalDate today) {
        if (!STRONG_PASSWORD.matcher(req.password()).matches()) {
            throw invalid("weak_password",
                    "A senha deve ter de 8 a 128 caracteres, com maiúscula, minúscula, número e símbolo");
        }
        if (Period.between(req.birthDate(), today).getYears() < 18) {
            throw invalid("underage", "É necessário ter pelo menos 18 anos para abrir uma conta");
        }
        if (!isValidCpf(normalizeDigits(req.cpf()))) {
            throw invalid("invalid_cpf", "CPF inválido");
        }
        if (!isValidPin(req.pin())) {
            throw invalid("weak_pin", "O PIN deve ter 6 dígitos e não pode ser sequencial nem repetido");
        }
    }

    static boolean isValidPin(String pin) {
        if (pin == null || !PIN.matcher(pin).matches()) {
            return false;
        }
        boolean allSame = pin.chars().distinct().count() == 1;
        boolean ascending = true;
        boolean descending = true;
        for (int i = 1; i < pin.length(); i++) {
            int diff = pin.charAt(i) - pin.charAt(i - 1);
            ascending &= diff == 1;
            descending &= diff == -1;
        }
        return !allSame && !ascending && !descending;
    }

    static boolean isValidCpf(String cpf) {
        if (cpf.length() != 11 || cpf.chars().distinct().count() == 1) {
            return false;
        }
        return checkDigit(cpf, 9) == cpf.charAt(9) - '0' && checkDigit(cpf, 10) == cpf.charAt(10) - '0';
    }

    private static int checkDigit(String cpf, int length) {
        int sum = 0;
        for (int i = 0; i < length; i++) {
            sum += (cpf.charAt(i) - '0') * (length + 1 - i);
        }
        int rest = (sum * 10) % 11;
        return rest == 10 ? 0 : rest;
    }

    private static DomainException invalid(String code, String message) {
        return new DomainException(HttpStatus.UNPROCESSABLE_CONTENT, code, message);
    }
}
