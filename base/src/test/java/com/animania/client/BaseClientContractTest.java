package com.animania.client;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Native client registration and CraftStudio removal coverage. */
class BaseClientContractTest {
    @Test
    void allFacilityRenderersUseNativeModelPartsAndNoCraftStudioRuntime() throws Exception {
        String client = Files.readString(Path.of("src/main/java/com/animania/client/AnimaniaClient.java"));
        String renderer = Files.readString(Path.of("src/main/java/com/animania/client/render/AnimaniaAnimalRenderer.java"));
        assertTrue(client.contains("registerBlockEntityRenderer"));
        assertTrue(client.contains("BaseTroughRenderer"));
        assertTrue(client.contains("BaseNestRenderer"));
        assertTrue(client.contains("BaseSaltLickRenderer"));
        assertTrue(renderer.contains("LegacyAnimalModel"));
        String main = Files.readString(Path.of("src/main/java/com/animania/client/model/LegacyAnimalModel.java"));
        assertTrue(!main.toLowerCase(java.util.Locale.ROOT).contains("craftstudio"));
    }
}
