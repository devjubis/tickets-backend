package com.devjubis.tickets.shared.domain;

import java.security.SecureRandom;
import java.util.UUID;

public final class UuidV7 {

    private static final SecureRandom RANDOM = new SecureRandom();

    private static final int RANDOM_BYTE_COUNT = 10;
    private static final int RANDOM_B_OFFSET = 2;
    private static final int TIMESTAMP_SHIFT = 16;
    private static final int BYTE_SHIFT = 8;
    private static final long TIMESTAMP_MASK = 0xFFFF_FFFF_FFFFL;
    private static final long VERSION_7_FLAG = 0x7000L;
    private static final long RANDOM_A_HIGH_MASK = 0x0FL;
    private static final long BYTE_MASK = 0xFFL;
    private static final long VARIANT_CLEAR_MASK = 0x3FFF_FFFF_FFFF_FFFFL;
    private static final long VARIANT_RFC_FLAG = 0x8000_0000_0000_0000L;

    private UuidV7() {
    }

    public static UUID generate() {
        long timestampMillis = System.currentTimeMillis();
        byte[] randomBytes = new byte[RANDOM_BYTE_COUNT];
        RANDOM.nextBytes(randomBytes);

        long mostSignificantBits = ((timestampMillis & TIMESTAMP_MASK) << TIMESTAMP_SHIFT)
                | VERSION_7_FLAG
                | ((randomBytes[0] & RANDOM_A_HIGH_MASK) << BYTE_SHIFT)
                | (randomBytes[1] & BYTE_MASK);

        long leastSignificantBits = 0L;
        for (int index = RANDOM_B_OFFSET; index < RANDOM_BYTE_COUNT; index++) {
            leastSignificantBits = (leastSignificantBits << BYTE_SHIFT) | (randomBytes[index] & BYTE_MASK);
        }
        leastSignificantBits = (leastSignificantBits & VARIANT_CLEAR_MASK) | VARIANT_RFC_FLAG;

        return new UUID(mostSignificantBits, leastSignificantBits);
    }
}
