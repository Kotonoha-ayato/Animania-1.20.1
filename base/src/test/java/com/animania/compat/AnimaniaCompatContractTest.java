package com.animania.compat;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Contract checks for optional JEI/Jade/TOP integrations and shared probes. */
class AnimaniaCompatContractTest {
    @Test
    void optionalIntegrationsUseModernRegistrationEntrypoints() throws Exception {
        String jei = Files.readString(Path.of("src/main/java/com/animania/compat/jei/AnimaniaJeiPlugin.java"));
        String jade = Files.readString(Path.of("src/main/java/com/animania/compat/jade/AnimaniaJadePlugin.java"));
        String top = Files.readString(Path.of("src/main/java/com/animania/compat/top/AnimaniaTopProbeCompat.java"));
        assertTrue(jei.contains("@JeiPlugin"));
        assertTrue(jei.contains("registerRecipes"));
        assertTrue(jade.contains("@WailaPlugin"));
        assertTrue(jade.contains("registerEntityDataProvider"));
        assertTrue(top.contains("getTheOneProbe"));
        assertTrue(top.contains("registerEntityProvider"));
    }

    @Test
    void probeStateIncludesGenderParentAndCareFlags() throws Exception {
        String probe = Files.readString(Path.of("src/main/java/com/animania/compat/AnimaniaProbeComponents.java"));
        assertTrue(probe.contains("getGender"));
        assertTrue(probe.contains("parentUuid"));
        assertTrue(probe.contains("isPregnant"));
        assertTrue(probe.contains("isSterilized"));
        assertTrue(probe.contains("isSitting"));
    }
}
