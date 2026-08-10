package com.animania.extra;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ExtraConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue ENABLE_SPAWNS;
    public static final ForgeConfigSpec.BooleanValue REPLACE_VANILLA_RABBITS;
    public static final ForgeConfigSpec.BooleanValue SPAWN_ANIMANIA_RODENTS;
    public static final ForgeConfigSpec.BooleanValue SPAWN_ANIMANIA_PEACOCKS;
    public static final ForgeConfigSpec.BooleanValue SPAWN_ANIMANIA_AMPHIBIANS;
    public static final ForgeConfigSpec.BooleanValue SPAWN_ANIMANIA_RABBITS;
    public static final ForgeConfigSpec.IntValue SPAWN_WEIGHT;
    public static final ForgeConfigSpec.IntValue SPAWN_PROBABILITY_HEDGEHOGS;
    public static final ForgeConfigSpec.IntValue SPAWN_PROBABILITY_FERRETS;
    public static final ForgeConfigSpec.IntValue SPAWN_PROBABILITY_HAMSTERS;
    public static final ForgeConfigSpec.IntValue SPAWN_PROBABILITY_PEACOCKS;
    public static final ForgeConfigSpec.IntValue SPAWN_PROBABILITY_AMPHIBIANS;
    public static final ForgeConfigSpec.IntValue SPAWN_PROBABILITY_RABBITS;
    public static final ForgeConfigSpec.IntValue NUMBER_RABBIT_FAMILIES;
    public static final ForgeConfigSpec.IntValue SPAWN_LIMIT_HEDGEHOGS;
    public static final ForgeConfigSpec.IntValue SPAWN_LIMIT_FERRETS;
    public static final ForgeConfigSpec.IntValue SPAWN_LIMIT_HAMSTERS;
    public static final ForgeConfigSpec.IntValue SPAWN_LIMIT_PEACOCKS;
    public static final ForgeConfigSpec.IntValue SPAWN_LIMIT_AMPHIBIANS;
    public static final ForgeConfigSpec.IntValue SPAWN_LIMIT_RABBITS;
    public static final ForgeConfigSpec.IntValue HAMSTER_WHEEL_CAPACITY;
    public static final ForgeConfigSpec.IntValue HAMSTER_WHEEL_GENERATION;
    public static final ForgeConfigSpec.IntValue HAMSTER_WHEEL_USE_TIME;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> FERRET_FOOD;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> HAMSTER_FOOD;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> HEDGEHOG_FOOD;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> PEACOCK_FOOD;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> RABBIT_FOOD;
    public static final Map<String, ForgeConfigSpec.ConfigValue<String>> BED_BLOCKS;
    public static final Map<String, ForgeConfigSpec.ConfigValue<List<? extends String>>> BIOME_TYPES;
    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("extra");
        ENABLE_SPAWNS = builder.define("enableNaturalSpawns", true);
        REPLACE_VANILLA_RABBITS = builder.define("replaceVanillaRabbits", true);
        SPAWN_ANIMANIA_RODENTS = builder.define("spawnAnimaniaRodents", true);
        SPAWN_ANIMANIA_PEACOCKS = builder.define("spawnAnimaniaPeacocks", true);
        SPAWN_ANIMANIA_AMPHIBIANS = builder.define("spawnAnimaniaAmphibians", true);
        SPAWN_ANIMANIA_RABBITS = builder.define("spawnAnimaniaRabbits", true);
        SPAWN_WEIGHT = builder.defineInRange("spawnWeight", 5, 1, 100);
        SPAWN_PROBABILITY_HEDGEHOGS = builder.defineInRange("spawnProbabilityHedgehogs", 8, 1, 100);
        SPAWN_PROBABILITY_FERRETS = builder.defineInRange("spawnProbabilityFerrets", 8, 1, 100);
        SPAWN_PROBABILITY_HAMSTERS = builder.defineInRange("spawnProbabilityHamsters", 8, 1, 100);
        SPAWN_PROBABILITY_PEACOCKS = builder.defineInRange("spawnProbabilityPeacocks", 8, 1, 100);
        SPAWN_PROBABILITY_AMPHIBIANS = builder.defineInRange("spawnProbabilityAmphibians", 8, 1, 100);
        SPAWN_PROBABILITY_RABBITS = builder.defineInRange("spawnProbabilityRabbits", 8, 1, 100);
        NUMBER_RABBIT_FAMILIES = builder.defineInRange("numberRabbitFamilies", 2, 1, 32);
        SPAWN_LIMIT_HEDGEHOGS = builder.defineInRange("spawnLimitHedgehogs", 40, 1, 256);
        SPAWN_LIMIT_FERRETS = builder.defineInRange("spawnLimitFerrets", 40, 1, 256);
        SPAWN_LIMIT_HAMSTERS = builder.defineInRange("spawnLimitHamsters", 40, 1, 256);
        SPAWN_LIMIT_PEACOCKS = builder.defineInRange("spawnLimitPeacocks", 40, 1, 256);
        SPAWN_LIMIT_AMPHIBIANS = builder.defineInRange("spawnLimitAmphibians", 40, 1, 256);
        SPAWN_LIMIT_RABBITS = builder.defineInRange("spawnLimitRabbits", 40, 1, 256);
        HAMSTER_WHEEL_CAPACITY = builder.defineInRange("hamsterWheelCapacity", 200000, 1000, 10000000);
        HAMSTER_WHEEL_GENERATION = builder.defineInRange("hamsterWheelGeneration", 20, 1, 10000);
        HAMSTER_WHEEL_USE_TIME = builder.defineInRange("hamsterWheelUseTime", 2000, 20, 240000);
        FERRET_FOOD = builder.defineList("ferretFood", List.of("minecraft:mutton", "minecraft:egg", "animania_farm:brown_egg", "animania_extra:peacock_egg_blue", "animania_extra:peacock_egg_white", "minecraft:rabbit", "minecraft:chicken"), value -> value instanceof String);
        HAMSTER_FOOD = builder.defineList("hamsterFood", List.of("animania_extra:hamster_food", "minecraft:wheat_seeds", "minecraft:melon_seeds", "minecraft:beetroot_seeds", "minecraft:pumpkin_seeds", "minecraft:apple"), value -> value instanceof String);
        HEDGEHOG_FOOD = builder.defineList("hedgehogFood", List.of("minecraft:carrot", "minecraft:beetroot", "minecraft:egg", "animania_farm:brown_egg", "minecraft:rabbit", "minecraft:chicken", "minecraft:apple"), value -> value instanceof String);
        PEACOCK_FOOD = builder.defineList("peacockFood", List.of("minecraft:wheat_seeds", "minecraft:melon_seeds", "minecraft:beetroot_seeds", "minecraft:pumpkin_seeds"), value -> value instanceof String);
        RABBIT_FOOD = builder.defineList("rabbitFood", List.of("minecraft:wheat", "minecraft:carrot", "minecraft:beetroot", "minecraft:apple"), value -> value instanceof String);
        Map<String, ForgeConfigSpec.ConfigValue<String>> beds = new LinkedHashMap<>();
        beds.put("ferretBed", builder.define("ferretBed", "animania_farm:animania_wool"));
        beds.put("ferretBed2", builder.define("ferretBed2", "minecraft:grass_block"));
        beds.put("hamsterBed", builder.define("hamsterBed", "animania_farm:animania_wool"));
        beds.put("hamsterBed2", builder.define("hamsterBed2", ""));
        beds.put("hedgehogBed", builder.define("hedgehogBed", "animania_farm:animania_wool"));
        beds.put("hedgehogBed2", builder.define("hedgehogBed2", "minecraft:grass_block"));
        beds.put("peacockBed", builder.define("peacockBed", "animania_farm:animania_wool"));
        beds.put("peacockBed2", builder.define("peacockBed2", "minecraft:grass_block"));
        beds.put("rabbitBed", builder.define("rabbitBed", "animania_farm:animania_wool"));
        beds.put("rabbitBed2", builder.define("rabbitBed2", "minecraft:grass_block"));
        BED_BLOCKS = Map.copyOf(beds);
        Map<String, ForgeConfigSpec.ConfigValue<List<? extends String>>> biomes = new LinkedHashMap<>();
        defineBiome(biomes, builder, "toadBiomeTypes", List.of("SWAMP", "FOREST"));
        defineBiome(biomes, builder, "frogBiomeTypes", List.of("SWAMP", "RIVER"));
        defineBiome(biomes, builder, "dartFrogBiomeTypes", List.of("JUNGLE", "FOREST"));
        defineBiome(biomes, builder, "hamsterBiomeTypes", List.of("BEACH", "SANDY"));
        defineBiome(biomes, builder, "ferretGrayBiomeTypes", List.of("SAVANNA"));
        defineBiome(biomes, builder, "ferretWhiteBiomeTypes", List.of("SAVANNA"));
        defineBiome(biomes, builder, "hedgehogBiomeTypes", List.of("FOREST"));
        defineBiome(biomes, builder, "hedgehogAlbinoBiomeTypes", List.of("SWAMP"));
        defineBiome(biomes, builder, "rabbitCottontailBiomeTypes", List.of("FOREST"));
        defineBiome(biomes, builder, "rabbitChinchillaBiomeTypes", List.of("SAVANNA"));
        defineBiome(biomes, builder, "rabbitDutchBiomeTypes", List.of("PLAINS"));
        defineBiome(biomes, builder, "rabbitHavanaBiomeTypes", List.of("MOUNTAIN", "HILLS"));
        defineBiome(biomes, builder, "rabbitJackBiomeTypes", List.of("SAVANNA", "SANDY"));
        defineBiome(biomes, builder, "rabbitNewZealandBiomeTypes", List.of("FOREST"));
        defineBiome(biomes, builder, "rabbitRexBiomeTypes", List.of("SAVANNA"));
        defineBiome(biomes, builder, "rabbitLopBiomeTypes", List.of("PLAINS", "FOREST"));
        defineBiome(biomes, builder, "peafowlCharcoalBiomeTypes", List.of("SWAMP", "JUNGLE"));
        defineBiome(biomes, builder, "peafowlOpalBiomeTypes", List.of("SWAMP", "JUNGLE"));
        defineBiome(biomes, builder, "peafowlPeachBiomeTypes", List.of("SWAMP", "JUNGLE"));
        defineBiome(biomes, builder, "peafowlPurpleBiomeTypes", List.of("SWAMP", "JUNGLE"));
        defineBiome(biomes, builder, "peafowlTaupeBiomeTypes", List.of("SWAMP", "JUNGLE"));
        defineBiome(biomes, builder, "peafowlBlueBiomeTypes", List.of("SWAMP", "JUNGLE"));
        defineBiome(biomes, builder, "peafowlWhiteBiomeTypes", List.of("SWAMP", "JUNGLE"));
        BIOME_TYPES = Map.copyOf(biomes);
        builder.pop();
        SPEC = builder.build();
    }
    private ExtraConfig() { }

    private static void defineBiome(Map<String, ForgeConfigSpec.ConfigValue<List<? extends String>>> values,
                                    ForgeConfigSpec.Builder builder, String key, List<String> defaults) {
        values.put(key, builder.defineList(key, defaults, value -> value instanceof String));
    }

    public static boolean matchesConfiguredFood(List<? extends String> configured, ItemStack stack) {
        if (stack == null || stack.isEmpty() || configured == null) return false;
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        String registryId = id == null ? "" : id.toString();
        for (String raw : configured) {
            if (raw == null) continue;
            String value = raw.trim().toLowerCase(java.util.Locale.ROOT);
            if (value.equals(registryId)) return true;
            if (value.equals("minecraft:seeds") && stack.is(net.minecraft.tags.ItemTags.VILLAGER_PLANTABLE_SEEDS)) return true;
        }
        return false;
    }

    public static boolean matchesSpeciesFood(ResourceLocation entityId, ItemStack stack) {
        if (entityId == null) return false;
        String path = entityId.getPath();
        try {
            if (path.equals("hamster")) return matchesConfiguredFood(HAMSTER_FOOD.get(), stack);
            if (path.startsWith("ferret_")) return matchesConfiguredFood(FERRET_FOOD.get(), stack);
            if (path.startsWith("hedgehog")) return matchesConfiguredFood(HEDGEHOG_FOOD.get(), stack);
            if (path.startsWith("peacock_") || path.startsWith("peahen_") || path.startsWith("peachick_")) return matchesConfiguredFood(PEACOCK_FOOD.get(), stack);
            if (path.startsWith("doe_") || path.startsWith("buck_") || path.startsWith("kit_")) return matchesConfiguredFood(RABBIT_FOOD.get(), stack);
        } catch (IllegalStateException ignored) {
            return false;
        }
        return false;
    }
}
