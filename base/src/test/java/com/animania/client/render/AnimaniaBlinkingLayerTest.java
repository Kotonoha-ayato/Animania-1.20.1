package com.animania.client.render;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AnimaniaBlinkingLayerTest {
    @Test
    void dogUsesItsSingleLegacyBlinkOverlay() {
        ResourceLocation[] textures = AnimaniaBlinkingLayer.texturesFor(
                ResourceLocation.fromNamespaceAndPath("animania_catsdogs", "male_collie"));

        assertEquals(1, textures.length);
        assertEquals("animania_catsdogs:textures/entity/dogs/blink_collie.png", textures[0].toString());
    }

    @Test
    void catKeepsItsSeparateLeftAndRightBlinkLayers() {
        ResourceLocation[] textures = AnimaniaBlinkingLayer.texturesFor(
                ResourceLocation.fromNamespaceAndPath("animania_catsdogs", "queen_tabby"));

        assertEquals(2, textures.length);
        assertEquals("animania_catsdogs:textures/entity/cats/blink_1_left.png", textures[0].toString());
        assertEquals("animania_catsdogs:textures/entity/cats/blink_1_right.png", textures[1].toString());
    }
}
