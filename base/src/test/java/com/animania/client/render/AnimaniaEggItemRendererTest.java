package com.animania.client.render;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Regression coverage for the native replacement of the 1.12 animated egg renderer. */
class AnimaniaEggItemRendererTest {
    @Test
    void forgeItemExtensionUsesTheNativeEntityPreviewRenderer() throws Exception {
        String item = Files.readString(Path.of("src/main/java/com/animania/common/item/AnimaniaEntityEggItem.java"));
        String renderer = Files.readString(Path.of("src/main/java/com/animania/client/render/AnimaniaEggItemRenderer.java"));
        assertTrue(item.contains("initializeClient"));
        assertTrue(item.contains("FANCY_EGGS"));
        assertTrue(item.contains("BlockEntityWithoutLevelRenderer"));
        assertTrue(renderer.contains("createPreview"));
        assertTrue(renderer.contains("renderByItem"));
        assertTrue(renderer.contains("FANCY_EGGS_ROTATE"));
    }

    @Test
    void shippingEggModelsDoNotFallBackToVanillaStone() throws Exception {
        String random = Files.readString(Path.of("src/main/resources/assets/animania/models/item/entity_egg_random.json"));
        String fancy = Files.readString(Path.of("src/main/resources/assets/animania/models/item/fancy_egg.json"));
        assertTrue(random.contains("animania:item/egg_random"));
        assertTrue(fancy.contains("animania:item/egg_random"));
        assertFalse(random.contains("minecraft:block/stone"));
        assertFalse(fancy.contains("minecraft:block/stone"));
    }
}
