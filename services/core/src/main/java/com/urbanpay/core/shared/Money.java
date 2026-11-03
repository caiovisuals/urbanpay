package com.urbanpay.core.shared;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Valor monetário em BRL representado em centavos ({@code long}).
 * Nunca usamos {@code double}/{@code float} para dinheiro: 0.1 + 0.2 != 0.3.
 */
public record Money(long cents) implements Comparable<Money> {

    public static final Money ZERO = new Money(0);

    public static Money ofCents(long cents) {
        return new Money(cents);
    }

    /** Converte um decimal (ex.: 10.50) exigindo no máximo 2 casas, sem arredondar silenciosamente. */
    public static Money of(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("amount is required");
        }
        if (amount.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("amount must have at most 2 decimal places");
        }
        return new Money(amount.movePointRight(2).longValueExact());
    }

    public BigDecimal toDecimal() {
        return BigDecimal.valueOf(cents, 2).setScale(2, RoundingMode.UNNECESSARY);
    }

    public Money plus(Money other) {
        return new Money(Math.addExact(cents, other.cents));
    }

    public Money minus(Money other) {
        return new Money(Math.subtractExact(cents, other.cents));
    }

    public Money negate() {
        return new Money(Math.negateExact(cents));
    }

    public boolean isPositive() {
        return cents > 0;
    }

    @Override
    public int compareTo(Money other) {
        return Long.compare(cents, other.cents);
    }

    /** Formato usado na API: string decimal ("10.50"), para o cliente JS não converter em float. */
    @Override
    public String toString() {
        return toDecimal().toPlainString();
    }
}