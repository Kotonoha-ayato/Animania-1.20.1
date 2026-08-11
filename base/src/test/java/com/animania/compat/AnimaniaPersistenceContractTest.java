package com.animania.compat;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Verifies that probe-visible legacy state is backed by the modern save paths. */
class AnimaniaPersistenceContractTest {
    @Test
    void animalProbeFieldsAreSavedByTheAuthoritativeEntity() throws Exception {
        String entity = Files.readString(Path.of("src/main/java/com/animania/common/entity/AnimaniaAnimalEntity.java"));
        for (String key : new String[]{"AnimaniaHunger", "AnimaniaThirst", "AnimaniaSleeping", "AnimaniaPregnant",
                "AnimaniaSterilized", "MateUUID", "ParentUUID"}) {
            assertTrue(entity.contains("\"" + key + "\""), "missing persisted probe key " + key);
        }
        assertTrue(entity.contains("addAdditionalSaveData"));
        assertTrue(entity.contains("readAdditionalSaveData"));
    }

    @Test
    void facilityProbeFieldsUseBlockEntitySaveAndCapabilitySync() throws Exception {
        String storage = Files.readString(Path.of("src/main/java/com/animania/common/block/AnimaniaStorageBlockEntity.java"));
        assertTrue(storage.contains("saveAdditional"));
        assertTrue(storage.contains("load"));
        assertTrue(storage.contains("fluidCapability"));
        assertTrue(storage.contains("itemCapability"));
    }
}
