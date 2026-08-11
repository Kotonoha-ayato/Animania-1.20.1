package com.animania.common.config;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AnimaniaFoodOverrideTest {
    @Test
    void parsesLegacyFoodOverrideSyntax() {
        var parsed = AnimaniaConfig.parseFoodValueOverride(" animania_farm:truffle ( 7 , 1.25 ) ").orElseThrow();
        assertEquals(new ResourceLocation("animania_farm", "truffle"), parsed.itemId());
        assertEquals(7, parsed.nutrition());
        assertEquals(1.25F, parsed.saturationModifier());
    }

    @Test
    void rejectsMalformedOrUnsafeOverrides() {
        assertTrue(AnimaniaConfig.parseFoodValueOverride("animania_farm:truffle").isEmpty());
        assertTrue(AnimaniaConfig.parseFoodValueOverride("animania_farm:truffle(abc,1)").isEmpty());
        assertTrue(AnimaniaConfig.parseFoodValueOverride("animania_farm:truffle(-1,1)").isEmpty());
        assertTrue(AnimaniaConfig.parseFoodValueOverride("animania_farm:truffle(1,-0.5)").isEmpty());
        assertTrue(AnimaniaConfig.parseFoodValueOverride("not an id(1,1)").isEmpty());
    }

    @Test
    void troughDefaultsRetainEveryLegacyOptionalFoodInOrder() {
        assertEquals(java.util.List.of("minecraft:wheat", "simplecorn:corncob", "harvestcraft:barleyitem",
                "harvestcraft:oatsitem", "harvestcraft:ryeitem", "harvestcraft:cornitem", "minecraft:apple",
                "minecraft:carrot", "minecraft:beetroot", "minecraft:potato", "minecraft:poisonous_potato",
                "minecraft:wheat_seeds", "minecraft:melon_seeds", "minecraft:beetroot_seeds",
                "minecraft:pumpkin_seeds", "biomesoplenty:turnip_seeds", "minecraft:egg",
                "animania_farm:brown_egg", "listAllbeefraw", "minecraft:fish"), AnimaniaConfig.TROUGH_FOOD.getDefault());
    }
}
