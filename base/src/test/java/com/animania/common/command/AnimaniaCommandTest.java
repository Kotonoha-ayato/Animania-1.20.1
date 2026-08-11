package com.animania.common.command;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Executable compatibility coverage for the guarded vanilla conversion command. */
class AnimaniaCommandTest {
    @Test
    void legacyFamiliesMapToModernVanillaCounterparts() {
        assertEquals("minecraft:cow", id(AnimaniaConversion.vanillaTypeIdFor(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("animania_farm", "cow_angus"))));
        assertEquals("minecraft:sheep", id(AnimaniaConversion.vanillaTypeIdFor(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("animania_farm", "lamb_dorper"))));
        assertEquals("minecraft:pig", id(AnimaniaConversion.vanillaTypeIdFor(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("animania_farm", "piglet_duroc"))));
        assertEquals("minecraft:chicken", id(AnimaniaConversion.vanillaTypeIdFor(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("animania_farm", "rooster_leghorn"))));
        assertEquals("minecraft:horse", id(AnimaniaConversion.vanillaTypeIdFor(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("animania_farm", "mare_draft"))));
        assertEquals("minecraft:rabbit", id(AnimaniaConversion.vanillaTypeIdFor(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("animania_extra", "doe_lop"))));
        assertEquals("minecraft:cat", id(AnimaniaConversion.vanillaTypeIdFor(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("animania_catsdogs", "queen_tabby"))));
        assertEquals("minecraft:wolf", id(AnimaniaConversion.vanillaTypeIdFor(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("animania_catsdogs", "male_collie"))));
        assertNull(AnimaniaConversion.vanillaTypeIdFor(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("minecraft", "cow")));
    }

    private static String id(net.minecraft.resources.ResourceLocation value) {
        return value == null ? null : value.toString();
    }

    @Test
    void commandRemainsAConfirmationGate() throws Exception {
        String source = java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/main/java/com/animania/common/command/AnimaniaCommand.java"));
        assertTrue(source.contains("CONFIRM_WINDOW_MILLIS"));
        assertTrue(source.contains("commands.animania.tovanilla.warning"));
        assertTrue(source.contains("entity.discard()"));
        assertTrue(source.contains("level.addFreshEntity(replacement)"));
    }
}
