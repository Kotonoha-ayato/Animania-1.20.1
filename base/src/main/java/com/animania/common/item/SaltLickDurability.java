package com.animania.common.item;

/** Pure conversion between persisted salt-lick uses and the BlockItem damage value. */
public final class SaltLickDurability {
    private SaltLickDurability() {
    }

    public static int remainingUses(int damage, int maximum) {
        int safeMaximum = Math.max(1, maximum);
        return safeMaximum - clamp(damage, 0, safeMaximum);
    }

    public static int damageForRemainingUses(int uses, int maximum) {
        int safeMaximum = Math.max(1, maximum);
        return safeMaximum - clamp(uses, 0, safeMaximum);
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
