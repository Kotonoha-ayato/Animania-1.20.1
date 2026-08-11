package com.animania.farm;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class FarmConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue ENABLE_SPAWNS;
    public static final ForgeConfigSpec.BooleanValue ENABLE_VEHICLES;
    public static final ForgeConfigSpec.IntValue SPAWN_WEIGHT;
    public static final ForgeConfigSpec.IntValue SPAWN_PROBABILITY_COWS;
    public static final ForgeConfigSpec.IntValue SPAWN_PROBABILITY_HORSES;
    public static final ForgeConfigSpec.IntValue SPAWN_PROBABILITY_PIGS;
    public static final ForgeConfigSpec.IntValue SPAWN_PROBABILITY_CHICKENS;
    public static final ForgeConfigSpec.IntValue SPAWN_PROBABILITY_GOATS;
    public static final ForgeConfigSpec.IntValue SPAWN_PROBABILITY_SHEEP;
    public static final ForgeConfigSpec.IntValue HIVE_WILD_HONEY_RATE;
    public static final ForgeConfigSpec.IntValue HIVE_PLAYER_HONEY_RATE;
    public static final ForgeConfigSpec.IntValue HIVE_CAPACITY;
    // 1.12 farm knobs retained as first-class 1.20.1 Forge settings.  The
    // modern implementations consume the values where the vanilla API has a
    // direct equivalent; the remaining values are exposed to datapack/addon
    // integrations instead of being silently discarded by migration.
    public static final ForgeConfigSpec.BooleanValue ALLOW_EGG_THROWING;
    public static final ForgeConfigSpec.IntValue CHEESE_MATURITY_TIME;
    public static final ForgeConfigSpec.BooleanValue COWS_MILKABLE_AT_SPAWN;
    public static final ForgeConfigSpec.BooleanValue SLEEP_ALLOWED_WAGON;
    public static final ForgeConfigSpec.BooleanValue DISABLE_ROLLING_VEHICLES;
    public static final ForgeConfigSpec.BooleanValue DISABLE_SALT_CREATION;
    public static final ForgeConfigSpec.IntValue SALT_CREATION_AMOUNT;
    public static final ForgeConfigSpec.BooleanValue CHICKENS_DROP_EGGS;
    public static final ForgeConfigSpec.BooleanValue HIVE_SPAWNING;
    public static final ForgeConfigSpec.IntValue HIVE_SPAWNING_FREQUENCY;
    public static final ForgeConfigSpec.BooleanValue ROOSTERS_FIGHT;
    public static final ForgeConfigSpec.BooleanValue REPLACE_VANILLA_COWS;
    public static final ForgeConfigSpec.BooleanValue REPLACE_VANILLA_PIGS;
    public static final ForgeConfigSpec.BooleanValue REPLACE_VANILLA_CHICKENS;
    public static final ForgeConfigSpec.BooleanValue REPLACE_VANILLA_SHEEP;
    public static final ForgeConfigSpec.BooleanValue REPLACE_VANILLA_HORSES;
    public static final ForgeConfigSpec.BooleanValue SPAWN_ANIMANIA_CHICKENS;
    public static final ForgeConfigSpec.BooleanValue SPAWN_ANIMANIA_COWS;
    public static final ForgeConfigSpec.BooleanValue SPAWN_ANIMANIA_PIGS;
    public static final ForgeConfigSpec.BooleanValue SPAWN_ANIMANIA_HORSES;
    public static final ForgeConfigSpec.BooleanValue SPAWN_ANIMANIA_GOATS;
    public static final ForgeConfigSpec.BooleanValue SPAWN_ANIMANIA_SHEEP;
    public static final ForgeConfigSpec.IntValue NUMBER_COW_FAMILIES;
    public static final ForgeConfigSpec.IntValue NUMBER_PIG_FAMILIES;
    public static final ForgeConfigSpec.IntValue NUMBER_CHICKEN_FAMILIES;
    public static final ForgeConfigSpec.IntValue NUMBER_HORSE_FAMILIES;
    public static final ForgeConfigSpec.IntValue NUMBER_GOAT_FAMILIES;
    public static final ForgeConfigSpec.IntValue NUMBER_SHEEP_FAMILIES;
    public static final ForgeConfigSpec.IntValue SPAWN_LIMIT_COWS;
    public static final ForgeConfigSpec.IntValue SPAWN_LIMIT_PIGS;
    public static final ForgeConfigSpec.IntValue SPAWN_LIMIT_CHICKENS;
    public static final ForgeConfigSpec.IntValue SPAWN_LIMIT_HORSES;
    public static final ForgeConfigSpec.IntValue SPAWN_LIMIT_GOATS;
    public static final ForgeConfigSpec.IntValue SPAWN_LIMIT_SHEEP;
    /** Preferred/backup sleeping blocks, retained for addon and AI queries. */
    public static final Map<String, ForgeConfigSpec.ConfigValue<String>> BED_BLOCKS;
    /** 1.12 BiomeDictionary lists, represented as modern string lists. */
    public static final Map<String, ForgeConfigSpec.ConfigValue<List<? extends String>>> BIOME_TYPES;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> CHICKEN_FOOD;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> COW_FOOD;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> GOAT_FOOD;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> HORSE_FOOD;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> SHEEP_FOOD;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> PIG_FOOD;
    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("farm");
        ENABLE_SPAWNS = builder.define("enableNaturalSpawns", true);
        ENABLE_VEHICLES = builder.define("enableVehicles", true);
        SPAWN_WEIGHT = builder.defineInRange("spawnWeight", 8, 1, 100);
        SPAWN_PROBABILITY_COWS = builder.defineInRange("spawnProbabilityCows", 9, 1, 100);
        SPAWN_PROBABILITY_HORSES = builder.defineInRange("spawnProbabilityHorses", 8, 1, 100);
        SPAWN_PROBABILITY_PIGS = builder.defineInRange("spawnProbabilityPigs", 9, 1, 100);
        SPAWN_PROBABILITY_CHICKENS = builder.defineInRange("spawnProbabilityChickens", 9, 1, 100);
        SPAWN_PROBABILITY_GOATS = builder.defineInRange("spawnProbabilityGoats", 8, 1, 100);
        SPAWN_PROBABILITY_SHEEP = builder.defineInRange("spawnProbabilitySheep", 8, 1, 100);
        ALLOW_EGG_THROWING = builder.define("allowEggThrowing", false);
        CHEESE_MATURITY_TIME = builder.defineInRange("cheeseMaturityTime", 24000, 20, 240000);
        COWS_MILKABLE_AT_SPAWN = builder.define("cowsMilkableAtSpawn", false);
        SLEEP_ALLOWED_WAGON = builder.define("sleepAllowedWagon", true);
        DISABLE_ROLLING_VEHICLES = builder.define("disableRollingVehicles", false);
        DISABLE_SALT_CREATION = builder.define("disableSaltCreation", false);
        SALT_CREATION_AMOUNT = builder.defineInRange("saltCreationAmount", 16, 0, 64);
        CHICKENS_DROP_EGGS = builder.define("chickensDropEggs", false);
        HIVE_SPAWNING = builder.define("hiveSpawning", true);
        HIVE_SPAWNING_FREQUENCY = builder.defineInRange("hiveSpawningFrequency", 3, 1, 10);
        ROOSTERS_FIGHT = builder.define("roostersFight", false);
        REPLACE_VANILLA_COWS = builder.define("replaceVanillaCows", true);
        REPLACE_VANILLA_PIGS = builder.define("replaceVanillaPigs", true);
        REPLACE_VANILLA_CHICKENS = builder.define("replaceVanillaChickens", true);
        REPLACE_VANILLA_SHEEP = builder.define("replaceVanillaSheep", true);
        REPLACE_VANILLA_HORSES = builder.define("replaceVanillaHorses", false);
        SPAWN_ANIMANIA_CHICKENS = builder.define("spawnAnimaniaChickens", true);
        SPAWN_ANIMANIA_COWS = builder.define("spawnAnimaniaCows", true);
        SPAWN_ANIMANIA_PIGS = builder.define("spawnAnimaniaPigs", true);
        SPAWN_ANIMANIA_HORSES = builder.define("spawnAnimaniaHorses", true);
        SPAWN_ANIMANIA_GOATS = builder.define("spawnAnimaniaGoats", true);
        SPAWN_ANIMANIA_SHEEP = builder.define("spawnAnimaniaSheep", true);
        NUMBER_COW_FAMILIES = builder.defineInRange("numberCowFamilies", 2, 1, 32);
        NUMBER_PIG_FAMILIES = builder.defineInRange("numberPigFamilies", 2, 1, 32);
        NUMBER_CHICKEN_FAMILIES = builder.defineInRange("numberChickenFamilies", 2, 1, 32);
        NUMBER_HORSE_FAMILIES = builder.defineInRange("numberHorseFamilies", 2, 1, 32);
        NUMBER_GOAT_FAMILIES = builder.defineInRange("numberGoatFamilies", 1, 1, 32);
        NUMBER_SHEEP_FAMILIES = builder.defineInRange("numberSheepFamilies", 3, 1, 32);
        SPAWN_LIMIT_COWS = builder.defineInRange("spawnLimitCows", 40, 1, 256);
        SPAWN_LIMIT_PIGS = builder.defineInRange("spawnLimitPigs", 40, 1, 256);
        SPAWN_LIMIT_CHICKENS = builder.defineInRange("spawnLimitChickens", 40, 1, 256);
        SPAWN_LIMIT_HORSES = builder.defineInRange("spawnLimitHorses", 40, 1, 256);
        SPAWN_LIMIT_GOATS = builder.defineInRange("spawnLimitGoats", 40, 1, 256);
        SPAWN_LIMIT_SHEEP = builder.defineInRange("spawnLimitSheep", 40, 1, 256);
        HIVE_WILD_HONEY_RATE = builder.comment("Ticks between honey production for wild hives").defineInRange("hiveWildHoneyRate", 700, 20, 240000);
        HIVE_PLAYER_HONEY_RATE = builder.comment("Ticks between honey production for player-made hives").defineInRange("hivePlayerHoneyRate", 450, 20, 240000);
        HIVE_CAPACITY = builder.defineInRange("hiveCapacity", 5000, 1000, 100000);
        CHICKEN_FOOD = builder.defineList("chickenFood", List.of("minecraft:wheat_seeds", "minecraft:melon_seeds", "minecraft:beetroot_seeds", "minecraft:pumpkin_seeds", "simplecorn:corncob", "biomesoplenty:turnip_seeds", "harvestcraft:cornitem"), value -> value instanceof String);
        COW_FOOD = builder.defineList("cowFood", List.of("minecraft:wheat", "simplecorn:corncob", "harvestcraft:barleyitem", "harvestcraft:oatsitem", "harvestcraft:ryeitem", "harvestcraft:cornitem"), value -> value instanceof String);
        GOAT_FOOD = builder.defineList("goatFood", List.of("minecraft:wheat", "minecraft:string", "minecraft:stick", "minecraft:apple", "simplecorn:corncob", "harvestcraft:barleyitem", "harvestcraft:oatsitem", "harvestcraft:ryeitem", "harvestcraft:cornitem"), value -> value instanceof String);
        HORSE_FOOD = builder.defineList("horseFood", List.of("minecraft:wheat", "harvestcraft:barleyitem", "harvestcraft:oatsitem", "harvestcraft:ryeitem", "minecraft:apple", "minecraft:carrot"), value -> value instanceof String);
        SHEEP_FOOD = builder.defineList("sheepFood", List.of("minecraft:wheat", "harvestcraft:barleyitem", "harvestcraft:oatsitem", "harvestcraft:ryeitem"), value -> value instanceof String);
        PIG_FOOD = builder.defineList("pigFood", List.of("minecraft:carrot", "minecraft:beetroot", "minecraft:potato", "minecraft:poisonous_potato", "minecraft:bread"), value -> value instanceof String);
        Map<String, ForgeConfigSpec.ConfigValue<String>> beds = new LinkedHashMap<>();
        beds.put("chickenBed", builder.define("chickenBed", "animania:straw"));
        beds.put("chickenBed2", builder.define("chickenBed2", "minecraft:grass_block"));
        beds.put("cowBed", builder.define("cowBed", "animania:straw"));
        beds.put("cowBed2", builder.define("cowBed2", "minecraft:grass_block"));
        beds.put("goatBed", builder.define("goatBed", "animania:straw"));
        beds.put("goatBed2", builder.define("goatBed2", "minecraft:grass_block"));
        beds.put("horseBed", builder.define("horseBed", "animania:straw"));
        beds.put("horseBed2", builder.define("horseBed2", "minecraft:grass_block"));
        beds.put("pigBed", builder.define("pigBed", "animania:straw"));
        beds.put("pigBed2", builder.define("pigBed2", "minecraft:grass_block"));
        beds.put("sheepBed", builder.define("sheepBed", "animania:straw"));
        beds.put("sheepBed2", builder.define("sheepBed2", "minecraft:grass_block"));
        BED_BLOCKS = Map.copyOf(beds);
        Map<String, ForgeConfigSpec.ConfigValue<List<? extends String>>> biomes = new LinkedHashMap<>();
        defineBiome(biomes, builder, "hiveValidBiomeTypes", List.of("JUNGLE", "CONIFEROUS", "SWAMP", "FOREST", "PLAINS"));
        defineBiome(biomes, builder, "chickenPlymouthRockBiomeTypes", List.of("MOUNTAIN"));
        defineBiome(biomes, builder, "chickenLeghornBiomeTypes", List.of("PLAINS"));
        defineBiome(biomes, builder, "chickenOrpingtonBiomeTypes", List.of("JUNGLE", "SWAMP"));
        defineBiome(biomes, builder, "chickenWyandotteBiomeTypes", List.of("FOREST"));
        defineBiome(biomes, builder, "chickenRhodeIslandRedBiomeTypes", List.of("FOREST"));
        defineBiome(biomes, builder, "cowHolsteinBiomeTypes", List.of("FOREST"));
        defineBiome(biomes, builder, "cowFriesianBiomeTypes", List.of("PLAINS"));
        defineBiome(biomes, builder, "cowAngusBiomeTypes", List.of("JUNGLE", "MESA", "SWAMP"));
        defineBiome(biomes, builder, "cowHerefordBiomeTypes", List.of("MOUNTAIN", "HILLS"));
        defineBiome(biomes, builder, "cowHighlandBiomeTypes", List.of("MOUNTAIN", "HILLS"));
        defineBiome(biomes, builder, "cowJerseyBiomeTypes", List.of("WASTELAND", "SWAMP"));
        defineBiome(biomes, builder, "cowLonghornBiomeTypes", List.of("SAVANNA"));
        defineBiome(biomes, builder, "cowMooshroomBiomeTypes", List.of("MUSHROOM", "MAGICAL"));
        defineBiome(biomes, builder, "draftHorseBiomeTypes", List.of("PLAINS", "SAVANNA", "MESA"));
        defineBiome(biomes, builder, "pigYorkshireBiomeTypes", List.of("PLAINS"));
        defineBiome(biomes, builder, "pigOldSpotBiomeTypes", List.of("FOREST"));
        defineBiome(biomes, builder, "pigLargeBlackBiomeTypes", List.of("SWAMP", "DENSE"));
        defineBiome(biomes, builder, "pigLargeWhiteBiomeTypes", List.of("FOREST"));
        defineBiome(biomes, builder, "pigDurocBiomeTypes", List.of("JUNGLE"));
        defineBiome(biomes, builder, "pigHampshireBiomeTypes", List.of("MOUNTAIN", "HILLS"));
        defineBiome(biomes, builder, "goatAlpineBiomeTypes", List.of("MOUNTAIN", "HILLS"));
        defineBiome(biomes, builder, "goatAngoraBiomeTypes", List.of("PLAINS"));
        defineBiome(biomes, builder, "goatFaintingBiomeTypes", List.of("PLAINS"));
        defineBiome(biomes, builder, "goatKikoBiomeTypes", List.of("MOUNTAIN", "HILLS"));
        defineBiome(biomes, builder, "goatKinderBiomeTypes", List.of("SAVANNA", "MESA"));
        defineBiome(biomes, builder, "goatNigerianDwarfBiomeTypes", List.of("SANDY"));
        defineBiome(biomes, builder, "goatPygmyBiomeTypes", List.of("SAVANNA", "MESA"));
        defineBiome(biomes, builder, "sheepDorsetBiomeTypes", List.of("HILLS"));
        defineBiome(biomes, builder, "sheepFriesianBiomeTypes", List.of("PLAINS"));
        defineBiome(biomes, builder, "sheepJacobBiomeTypes", List.of("FOREST"));
        defineBiome(biomes, builder, "sheepMerinoBiomeTypes", List.of("PLAINS"));
        defineBiome(biomes, builder, "sheepSuffolkBiomeTypes", List.of("SAVANNA", "MESA"));
        defineBiome(biomes, builder, "sheepDorperBiomeTypes", List.of("SAVANNA"));
        BIOME_TYPES = Map.copyOf(biomes);
        builder.pop();
        SPEC = builder.build();
    }
    private FarmConfig() { }

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
            if (path.startsWith("hen_") || path.startsWith("rooster_") || path.startsWith("chick_")) return matchesConfiguredFood(CHICKEN_FOOD.get(), stack);
            if (path.startsWith("cow_") || path.startsWith("bull_") || path.startsWith("calf_")) return matchesConfiguredFood(COW_FOOD.get(), stack);
            if (path.startsWith("doe_") || path.startsWith("buck_") || path.startsWith("kid_")) return matchesConfiguredFood(GOAT_FOOD.get(), stack);
            if (path.startsWith("mare_") || path.startsWith("stallion_") || path.startsWith("foal_")) return matchesConfiguredFood(HORSE_FOOD.get(), stack);
            if (path.startsWith("ewe_") || path.startsWith("ram_") || path.startsWith("lamb_")) return matchesConfiguredFood(SHEEP_FOOD.get(), stack);
            if (path.startsWith("sow_") || path.startsWith("hog_") || path.startsWith("piglet_")) return matchesConfiguredFood(PIG_FOOD.get(), stack);
        } catch (IllegalStateException ignored) {
            return false;
        }
        return false;
    }
}
