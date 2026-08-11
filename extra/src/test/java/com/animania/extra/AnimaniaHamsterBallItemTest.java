package com.animania.extra;

import net.minecraft.world.item.DyeColor;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class AnimaniaHamsterBallItemTest {
    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void legacyMetadataOrderIsPreserved() {
        assertEquals(DyeColor.BLACK, AnimaniaHamsterBallItem.legacyDye(0));
        assertEquals(DyeColor.RED, AnimaniaHamsterBallItem.legacyDye(1));
        assertEquals(DyeColor.LIGHT_GRAY, AnimaniaHamsterBallItem.legacyDye(7));
        assertEquals(DyeColor.WHITE, AnimaniaHamsterBallItem.legacyDye(15));
    }
}
