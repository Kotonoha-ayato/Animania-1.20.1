package com.animania.catsdogs;

import com.animania.common.world.LegacyBiomeMatcher;
import com.mojang.serialization.Codec;
import net.minecraft.core.Holder;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.common.world.ModifiableBiomeInfo;

import java.util.List;

/** Config-backed 1.12 natural spawns: wolves, foxes and ocelots only. */
public final class CatsDogsSpawnBiomeModifier implements BiomeModifier {
    @Override
    public void modify(Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
        if (phase != Phase.ADD || !biome.is(BiomeTags.IS_OVERWORLD) || !bool(CatsDogsConfig.ENABLE_SPAWNS, true)) return;
        add(builder, biome, "female_wolf", "wolfBiomeTypes", CatsDogsConfig.SPAWN_PROBABILITY_DOGS, 5, CatsDogsConfig.NUMBER_DOG_FAMILIES);
        add(builder, biome, "female_fox", "foxBiomeTypes", CatsDogsConfig.SPAWN_PROBABILITY_DOGS, 5, CatsDogsConfig.NUMBER_DOG_FAMILIES);
        add(builder, biome, "queen_ocelot", "ocelotBiomeTypes", CatsDogsConfig.SPAWN_PROBABILITY_CATS, 4, CatsDogsConfig.NUMBER_CAT_FAMILIES);
    }

    private static void add(ModifiableBiomeInfo.BiomeInfo.Builder builder, Holder<Biome> biome, String id, String biomeKey,
                            ForgeConfigSpec.IntValue probability, int fallbackWeight, ForgeConfigSpec.IntValue families) {
        var configuredBiomes = CatsDogsConfig.BIOME_TYPES.get(biomeKey);
        if (configuredBiomes == null || !LegacyBiomeMatcher.matches(biome, list(configuredBiomes))) return;
        var entity = AnimaniaCatsDogs.ENTITIES.get(id);
        if (entity == null) return;
        builder.getMobSpawnSettings().addSpawn(MobCategory.CREATURE,
                new MobSpawnSettings.SpawnerData(entity.get(), integer(probability, fallbackWeight), 2,
                        Math.max(2, integer(families, 2))));
    }

    @Override
    public Codec<? extends BiomeModifier> codec() {
        return CatsDogsWorldgen.CONFIGURED_SPAWNS.get();
    }

    private static boolean bool(ForgeConfigSpec.BooleanValue value, boolean fallback) {
        try { return value.get(); } catch (RuntimeException ignored) { return fallback; }
    }

    private static int integer(ForgeConfigSpec.IntValue value, int fallback) {
        try { return value.get(); } catch (RuntimeException ignored) { return fallback; }
    }

    private static List<? extends String> list(ForgeConfigSpec.ConfigValue<List<? extends String>> value) {
        try { return value.get(); } catch (RuntimeException ignored) { return List.of(); }
    }
}
