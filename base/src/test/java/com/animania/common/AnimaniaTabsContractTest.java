package com.animania.common;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AnimaniaTabsContractTest {
    @Test
    void baseAndEachAddonExposeAStableCreativeTab() throws Exception {
        String base = Files.readString(Path.of("src/main/java/com/animania/common/AnimaniaTabs.java"));
        String farm = Files.readString(Path.of("../farm/src/main/java/com/animania/farm/FarmTab.java"));
        String extra = Files.readString(Path.of("../extra/src/main/java/com/animania/extra/ExtraTab.java"));
        String cats = Files.readString(Path.of("../catsdogs/src/main/java/com/animania/catsdogs/CatsDogsTab.java"));
        assertTrue(base.contains("CreativeModeTab"));
        assertTrue(farm.contains("DeferredRegister"));
        assertTrue(extra.contains("DeferredRegister"));
        assertTrue(cats.contains("DeferredRegister"));
    }
}
