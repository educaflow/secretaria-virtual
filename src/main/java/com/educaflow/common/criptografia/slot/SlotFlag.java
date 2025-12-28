package com.educaflow.common.criptografia.slot;

import java.util.EnumSet;
import java.util.Set;

public enum SlotFlag {
    RNG(0x00000001L),
    WRITE_PROTECTED(0x00000002L),
    LOGIN_REQUIRED(0x00000004L),
    USER_PIN_INITIALIZED(0x00000008L),
    RESTORE_KEY_NOT_NEEDED(0x00000020L),
    CLOCK_ON_TOKEN(0x00000040L),
    PROTECTED_AUTHENTICATION_PATH(0x00000100L),
    DUAL_CRYPTO_OPERATIONS(0x00000200L),
    TOKEN_INITIALIZED(0x00000400L),
    SECONDARY_AUTHENTICATION(0x00000800L),
    USER_PIN_COUNT_LOW(0x00010000L),
    USER_PIN_FINAL_TRY(0x00020000L),
    USER_PIN_LOCKED(0x00040000L),
    USER_PIN_TO_BE_CHANGED(0x00080000L),
    SO_PIN_COUNT_LOW(0x00100000L),
    SO_PIN_FINAL_TRY(0x00200000L),
    SO_PIN_LOCKED(0x00400000L),
    SO_PIN_TO_BE_CHANGED(0x00800000L);

    private final long value;
    SlotFlag(long value) { this.value = value; }
    public long getValue() { return value; }

    public static Set<SlotFlag> fromMask(long mask) {
        Set<SlotFlag> set = EnumSet.noneOf(SlotFlag.class);
        for (SlotFlag f : values()) {
            if ((mask & f.value) != 0) set.add(f);
        }
        return set;
    }
}