package com.animania.common.world;

import net.minecraft.core.Holder;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.common.Tags;

import java.util.List;
import java.util.Locale;

/** Maps the 1.12 BiomeDictionary names to Forge 47 biome tags. */
public final class LegacyBiomeMatcher {
    private LegacyBiomeMatcher() { }

    public static boolean matches(Holder<Biome> biome, List<? extends String> configured) {
        if (biome == null || configured == null || configured.isEmpty()) return false;
        for (String raw : configured) {
            if (raw == null || raw.isBlank()) continue;
            if (matches(biome, raw.trim().toUpperCase(Locale.ROOT))) return true;
        }
        return false;
    }

    private static boolean matches(Holder<Biome> biome, String type) {
        TagKey<Biome> forge = switch (type) {
            case "HOT" -> Tags.Biomes.IS_HOT;
            case "COLD" -> Tags.Biomes.IS_COLD;
            case "SPARSE" -> Tags.Biomes.IS_SPARSE;
            case "DENSE" -> Tags.Biomes.IS_DENSE;
            case "WET" -> Tags.Biomes.IS_WET;
            case "DRY" -> Tags.Biomes.IS_DRY;
            case "CONIFEROUS" -> Tags.Biomes.IS_CONIFEROUS;
            case "SPOOKY" -> Tags.Biomes.IS_SPOOKY;
            case "DEAD" -> Tags.Biomes.IS_DEAD;
            case "LUSH" -> Tags.Biomes.IS_LUSH;
            case "MUSHROOM" -> Tags.Biomes.IS_MUSHROOM;
            case "MAGICAL" -> Tags.Biomes.IS_MAGICAL;
            case "RARE" -> Tags.Biomes.IS_RARE;
            case "PLATEAU" -> Tags.Biomes.IS_PLATEAU;
            case "MODIFIED" -> Tags.Biomes.IS_MODIFIED;
            case "WATER" -> Tags.Biomes.IS_WATER;
            case "DESERT" -> Tags.Biomes.IS_DESERT;
            case "PLAINS" -> Tags.Biomes.IS_PLAINS;
            case "SWAMP" -> Tags.Biomes.IS_SWAMP;
            case "SANDY" -> Tags.Biomes.IS_SANDY;
            case "SNOWY" -> Tags.Biomes.IS_SNOWY;
            case "WASTELAND" -> Tags.Biomes.IS_WASTELAND;
            case "MOUNTAIN" -> Tags.Biomes.IS_MOUNTAIN;
            default -> null;
        };
        if (forge != null && biome.is(forge)) return true;
        return switch (type) {
            case "OCEAN" -> biome.is(BiomeTags.IS_OCEAN);
            case "BEACH" -> biome.is(BiomeTags.IS_BEACH);
            case "RIVER" -> biome.is(BiomeTags.IS_RIVER);
            case "HILLS" -> biome.is(BiomeTags.IS_HILL);
            case "TAIGA" -> biome.is(BiomeTags.IS_TAIGA);
            case "JUNGLE" -> biome.is(BiomeTags.IS_JUNGLE);
            case "FOREST" -> biome.is(BiomeTags.IS_FOREST);
            case "SAVANNA" -> biome.is(BiomeTags.IS_SAVANNA);
            case "MESA" -> biome.is(BiomeTags.IS_BADLANDS);
            default -> false;
        };
    }
}
