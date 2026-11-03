package com.urbanpay.core.identity.internal;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class RegistrationRulesTest {

    @ParameterizedTest
    @ValueSource(strings = {"52998224725", "11144477735"})
    void acceptsValidCpf(String cpf) {
        assertThat(RegistrationRules.isValidCpf(cpf)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"52998224724", "11111111111", "123", "00000000000"})
    void rejectsInvalidCpf(String cpf) {
        assertThat(RegistrationRules.isValidCpf(cpf)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"123456", "654321", "000000", "12345", "abcdef", "1234567"})
    void rejectsWeakPins(String pin) {
        assertThat(RegistrationRules.isValidPin(pin)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"482915", "135790", "112233"})
    void acceptsReasonablePins(String pin) {
        assertThat(RegistrationRules.isValidPin(pin)).isTrue();
    }

    @org.junit.jupiter.api.Test
    void masksCpf() {
        assertThat(AuthService.maskCpf("52998224725")).isEqualTo("***.982.247-**");
    }
}