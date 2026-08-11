package com.animania.client.config;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Config screen and Forge extension-point regression coverage. */
class AnimaniaConfigScreenTest {
    @Test
    void forgeRegistersAClientConfigScreenForTheModernSpec() throws Exception {
        String entry = Files.readString(Path.of("src/main/java/com/animania/Animania.java"));
        String screen = Files.readString(Path.of("src/main/java/com/animania/client/config/AnimaniaConfigScreen.java"));
        assertTrue(entry.contains("ConfigScreenHandler.ConfigScreenFactory"));
        assertTrue(entry.contains("AnimaniaConfigScreen::new"));
        assertTrue(screen.contains("AnimaniaConfig.FANCY_EGGS"));
        assertTrue(screen.contains("Button.builder"));
    }
}
