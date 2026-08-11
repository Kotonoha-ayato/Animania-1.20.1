package com.animania.common.loot;

import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Deterministic tests for fed/watered/gender predicates and count functions. */
class AnimaniaLootRulesTest {
    @Test
    void addMorePreservesItemAndAppliesInclusiveCountRange() {
        assertEquals(4, AnimaniaLootRules.addMoreCount(2, RandomSource.create(4L), 2, 2));
        assertEquals(2, AnimaniaLootRules.addMoreCount(2, RandomSource.create(4L), 0, 0));
    }
}
