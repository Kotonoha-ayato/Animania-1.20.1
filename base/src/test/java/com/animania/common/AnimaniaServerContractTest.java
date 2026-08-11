package com.animania.common;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Server-side replacement coverage for old event/interaction handlers. */
class AnimaniaServerContractTest {
    @Test
    void serverHooksKeepSeedSpawnDamageAndAdvancementResponsibilities() throws Exception {
        String events = Files.readString(Path.of("src/main/java/com/animania/AnimaniaServerEvents.java"));
        String animal = Files.readString(Path.of("src/main/java/com/animania/common/entity/AnimaniaAnimalEntity.java"));
        assertTrue(events.contains("onSeedRightClick"));
        assertTrue(events.contains("onSpawnPlacement"));
        assertTrue(events.contains("onEntityJoin"));
        assertTrue(animal.contains("source.is(net.minecraft.world.damagesource.DamageTypes.STARVE)"));
        assertTrue(animal.contains("if (isPassenger()) return false"));
        assertTrue(animal.contains("FeedAnimalTrigger"));
    }
}
