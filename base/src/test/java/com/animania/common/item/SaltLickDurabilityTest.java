package com.animania.common.item;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class SaltLickDurabilityTest {
    @Test
    void convertsDamageAndRemainingUsesWithoutLosingState() {
        int maximum = 200;

        assertEquals(180, SaltLickDurability.remainingUses(20, maximum));
        assertEquals(75, SaltLickDurability.damageForRemainingUses(125, maximum));
        assertEquals(125, SaltLickDurability.remainingUses(
                SaltLickDurability.damageForRemainingUses(125, maximum), maximum));
    }

    @Test
    void clampsCorruptOrOutOfRangeValues() {
        int maximum = 200;

        assertEquals(maximum, SaltLickDurability.remainingUses(-10, maximum));
        assertEquals(0, SaltLickDurability.remainingUses(500, maximum));
        assertEquals(0, SaltLickDurability.damageForRemainingUses(500, maximum));
        assertEquals(maximum, SaltLickDurability.damageForRemainingUses(-10, maximum));
    }
}
