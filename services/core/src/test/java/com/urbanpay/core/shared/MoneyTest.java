package com.urbanpay.core.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class MoneyTest {

    @Test
    void convertsDecimalToCentsWithoutFloatingPointErrors() {
        var sum = Money.of(new BigDecimal("0.1")).plus(Money.of(new BigDecimal("0.2")));
        assertThat(sum).isEqualTo(Money.of(new BigDecimal("0.30")));
        assertThat(sum.cents()).isEqualTo(30);
        assertThat(sum.toString()).isEqualTo("0.30");
    }

    @Test
    void rejectsMoreThanTwoDecimalPlaces() {
        assertThatThrownBy(() -> Money.of(new BigDecimal("10.001")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void acceptsTrailingZeros() {
        assertThat(Money.of(new BigDecimal("10.5000")).cents()).isEqualTo(1050);
    }

    @Test
    void detectsOverflow() {
        assertThatThrownBy(() -> Money.ofCents(Long.MAX_VALUE).plus(Money.ofCents(1)))
                .isInstanceOf(ArithmeticException.class);
    }
}
