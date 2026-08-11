package com.animania.client.manual;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** The legacy topic/page fields are intentionally client UI state, not world NBT. */
class ManualPersistenceContractTest {
    @Test
    void nativeManualKeepsTopicStateOutOfWorldPersistence() throws Exception {
        String screen = Files.readString(Path.of("src/main/java/com/animania/client/manual/ManualScreen.java"));
        assertTrue(screen.contains("private int page;"));
        assertTrue(!screen.contains("addAdditionalSaveData"));
        assertTrue(!screen.contains("CompoundTag"));
    }
}
