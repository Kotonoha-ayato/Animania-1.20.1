package com.animania.extra;

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
import java.util.Locale;

/** Reproduces the 1.12 Extra spawn table while evaluating the modern config at datapack load. */
public final class ExtraSpawnBiomeModifier implements BiomeModifier {
    @Override
    public void modify(Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
        if (phase != Phase.ADD || !biome.is(BiomeTags.IS_OVERWORLD) || !bool(ExtraConfig.ENABLE_SPAWNS, true)) return;
        for (var entry : AnimaniaExtra.ENTITIES.entrySet()) {
            SpawnRule rule = rule(entry.getKey());
            if (rule == null || !rule.enabled()) continue;
            var configuredBiomes = ExtraConfig.BIOME_TYPES.get(rule.biomeKey());
            if (configuredBiomes == null || !LegacyBiomeMatcher.matches(biome, list(configuredBiomes))) continue;
            builder.getMobSpawnSettings().addSpawn(MobCategory.CREATURE,
                    new MobSpawnSettings.SpawnerData(entry.getValue().get(), rule.weight(), rule.minimum(), rule.maximum()));
        }
    }

    @Override
    public Codec<? extends BiomeModifier> codec() {
        return ExtraWorldgen.CONFIGURED_SPAWNS.get();
    }

    private static SpawnRule rule(String id) {
        if (id.equals("hamster")) return new SpawnRule("hamsterBiomeTypes", "rodent", integer(ExtraConfig.SPAWN_PROBABILITY_HAMSTERS, 8), 1, 3);
        if (id.equals("ferret_grey")) return new SpawnRule("ferretGrayBiomeTypes", "rodent", integer(ExtraConfig.SPAWN_PROBABILITY_FERRETS, 8), 1, 3);
        if (id.equals("ferret_white")) return new SpawnRule("ferretWhiteBiomeTypes", "rodent", integer(ExtraConfig.SPAWN_PROBABILITY_FERRETS, 8), 1, 3);
        if (id.equals("hedgehog")) return new SpawnRule("hedgehogBiomeTypes", "rodent", integer(ExtraConfig.SPAWN_PROBABILITY_HEDGEHOGS, 8), 1, 3);
        if (id.equals("hedgehog_albino")) return new SpawnRule("hedgehogAlbinoBiomeTypes", "rodent", integer(ExtraConfig.SPAWN_PROBABILITY_HEDGEHOGS, 8), 1, 3);
        if (id.equals("toad") || id.equals("frog") || id.equals("dartfrog")) {
            String key = id.equals("dartfrog") ? "dartFrogBiomeTypes" : id + "BiomeTypes";
            return new SpawnRule(key, "amphibian", integer(ExtraConfig.SPAWN_PROBABILITY_AMPHIBIANS, 8) + 10, 2, 2);
        }
        if (id.startsWith("doe_")) {
            String breed = id.substring(4);
            int weight = integer(ExtraConfig.SPAWN_PROBABILITY_RABBITS, 8);
            if (breed.equals("dutch") || breed.equals("lop")) weight = Math.max(1, weight / 2);
            return new SpawnRule("rabbit" + pascal(breed) + "BiomeTypes", "rabbit", weight, 2,
                    Math.max(2, integer(ExtraConfig.NUMBER_RABBIT_FAMILIES, 2)));
        }
        if (id.startsWith("peacock_") || id.startsWith("peahen_") || id.startsWith("peachick_")) {
            String breed = id.substring(id.indexOf('_') + 1);
            int weight = integer(ExtraConfig.SPAWN_PROBABILITY_PEACOCKS, 8);
            if (id.startsWith("peachick_")) weight = Math.max(1, weight / 2);
            int minimum = breed.equals("blue") || breed.equals("white") ? 2 : 1;
            return new SpawnRule("peafowl" + pascal(breed) + "BiomeTypes", "peafowl", weight, minimum,
                    Math.max(minimum, integer(ExtraConfig.NUMBER_RABBIT_FAMILIES, 2)));
        }
        return null;
    }

    private record SpawnRule(String biomeKey, String family, int weight, int minimum, int maximum) {
        boolean enabled() {
            return switch (family) {
                case "rodent" -> bool(ExtraConfig.SPAWN_ANIMANIA_RODENTS, true);
                case "amphibian" -> bool(ExtraConfig.SPAWN_ANIMANIA_AMPHIBIANS, true);
                case "rabbit" -> bool(ExtraConfig.SPAWN_ANIMANIA_RABBITS, true);
                case "peafowl" -> bool(ExtraConfig.SPAWN_ANIMANIA_PEACOCKS, true);
                default -> false;
            };
        }
    }

    private static String pascal(String value) {
        StringBuilder result = new StringBuilder();
        for (String part : value.split("_")) {
            if (!part.isEmpty()) result.append(part.substring(0, 1).toUpperCase(Locale.ROOT)).append(part.substring(1));
        }
        return result.toString();
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
