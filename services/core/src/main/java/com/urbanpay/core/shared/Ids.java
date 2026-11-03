package com.urbanpay.core.shared;

import java.security.SecureRandom;
import java.util.UUID;

/**
 * Gera UUIDv7 (RFC 9562): ordenado pelo tempo, o que mantém os índices B-tree
 * do Postgres compactos (inserções sempre no fim), ao contrário do UUIDv4 aleatório.
 */
public final class Ids {

    private static final SecureRandom RANDOM = new SecureRandom();

    private Ids() {
    }

    public static UUID newId() {
        long timestamp = System.currentTimeMillis();
        long randA = RANDOM.nextInt(1 << 12);
        long randB = RANDOM.nextLong();

        long msb = (timestamp << 16) | (0x7L << 12) | randA;
        long lsb = (randB & 0x3FFF_FFFF_FFFF_FFFFL) | 0x8000_0000_0000_0000L;
        return new UUID(msb, lsb);
    }
}
