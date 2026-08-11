package com.animania.farm;

import com.animania.common.item.LegacyEggColors;
import com.animania.farm.client.model.FarmLegacyModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import org.junit.jupiter.api.Test;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;

class FarmRegistryTest {
    @Test
    void allLegacySoundEventsUseLegalModernIds() {
        assertEquals(96, FarmSoundCatalog.IDS.size());
        assertEquals(96, new HashSet<>(FarmSoundCatalog.IDS).size());
        assertTrue(FarmSoundCatalog.IDS.stream().allMatch(id -> id.equals(id.toLowerCase(java.util.Locale.ROOT))));
        assertTrue(FarmSoundCatalog.IDS.containsAll(java.util.List.of("crow1", "bullmoo8", "sheepliving7", "hitch", "unhitch")));
    }

    @Test
    void allPinnedAnimalIdsAreUniqueAndContentHasModernEntries() {
        assertFalse(FarmLegacyIds.ALL.isEmpty());
        assertEquals(FarmLegacyIds.ALL.size(), new HashSet<>(FarmLegacyIds.ALL).size());
        assertTrue(FarmLegacyIds.ALL.stream().anyMatch(id -> id.startsWith("cow_")));
    }

    @Test
    void everyAnimalEggHasItsExactLegacyTintPair() {
        FarmLegacyIds.ALL.stream().filter(id -> !FarmLegacyIds.isVehicle(id))
                .forEach(id -> assertNotNull(LegacyEggColors.forEntity(id), id));
        assertEquals(new LegacyEggColors.Colors(3028024, 2304560), LegacyEggColors.forEntity("bull_angus"));
        assertEquals(new LegacyEggColors.Colors(15987699, 1776411), LegacyEggColors.forEntity("lamb_dorper"));
        assertEquals(new LegacyEggColors.Colors(15987699, 3944229), LegacyEggColors.forEntity("calf_friesian"));
    }

    @Test
    void convertedModelProfilesNeverAnimateOneBoneAsBothLeftAndRight() {
        FarmLegacyIds.ALL.stream().filter(id -> !FarmLegacyIds.isVehicle(id)).forEach(id -> {
            var profile = FarmLegacyModelLayers.profile(id);
            var left = new HashSet<>(java.util.List.of(profile.leftLegs()));
            var right = new HashSet<>(java.util.List.of(profile.rightLegs()));
            left.retainAll(right);
            assertTrue(left.isEmpty(), id + " overlapping limbs " + left);
        });
        assertArrayEquals(new String[]{"back_leg__l", "front_leg__r"},
                FarmLegacyModelLayers.profile("buck_alpine").leftLegs());
        assertArrayEquals(new String[]{"leg0", "leg3"},
                FarmLegacyModelLayers.profile("bull_angus").leftLegs());
        assertArrayEquals(new String[]{"sac", "penis"},
                FarmLegacyModelLayers.profile("bull_angus").privateParts());
    }

    @Test
    void everyAnimalModelBakesGeometryAndEveryAnimationPathResolves() {
        FarmLegacyIds.ALL.stream().filter(id -> !FarmLegacyIds.isVehicle(id)).forEach(id -> {
            ModelPart root = FarmLegacyModelLayers.create(id).bakeRoot();
            assertTrue(root.getAllParts().anyMatch(part -> !part.isEmpty()), id + " baked with no cubes");
            assertProfilePaths(root, FarmLegacyModelLayers.profile(id), id);
        });
    }

    @Test
    void legacyColoredWoolPartsRemainDedicatedTintPasses() {
        for (String id : java.util.List.of("ewe_dorset", "ram_merino", "ewe_suffolk", "ram_friesian")) {
            var profile = FarmLegacyModelLayers.profile(id);
            assertTrue(profile.coloredParts().length >= 8, id + " lost ModelRendererColored wool parts");
            ModelPart root = FarmLegacyModelLayers.create(id).bakeRoot();
            for (String path : profile.coloredParts()) {
                assertTrue(hasPath(root, path), id + " colored part does not resolve: " + path);
            }
        }
    }

    @Test
    void legacySleepBedDefaultsMapBlockStrawToBaseStraw() {
        for (String family : java.util.List.of("chicken", "cow", "goat", "horse", "pig", "sheep")) {
            assertEquals("animania:straw", FarmConfig.BED_BLOCKS.get(family + "Bed").getDefault(), family);
            assertEquals("minecraft:grass_block", FarmConfig.BED_BLOCKS.get(family + "Bed2").getDefault(), family);
        }
    }

    @Test
    void legacyOptionalModFoodsRemainInExactDefaultOrder() {
        assertEquals(java.util.List.of("minecraft:wheat_seeds", "minecraft:melon_seeds", "minecraft:beetroot_seeds", "minecraft:pumpkin_seeds", "simplecorn:corncob", "biomesoplenty:turnip_seeds", "harvestcraft:cornitem"), FarmConfig.CHICKEN_FOOD.getDefault());
        assertEquals(java.util.List.of("minecraft:wheat", "simplecorn:corncob", "harvestcraft:barleyitem", "harvestcraft:oatsitem", "harvestcraft:ryeitem", "harvestcraft:cornitem"), FarmConfig.COW_FOOD.getDefault());
        assertEquals(java.util.List.of("minecraft:wheat", "minecraft:string", "minecraft:stick", "minecraft:apple", "simplecorn:corncob", "harvestcraft:barleyitem", "harvestcraft:oatsitem", "harvestcraft:ryeitem", "harvestcraft:cornitem"), FarmConfig.GOAT_FOOD.getDefault());
        assertEquals(java.util.List.of("minecraft:wheat", "harvestcraft:barleyitem", "harvestcraft:oatsitem", "harvestcraft:ryeitem", "minecraft:apple", "minecraft:carrot"), FarmConfig.HORSE_FOOD.getDefault());
        assertEquals(java.util.List.of("minecraft:wheat", "harvestcraft:barleyitem", "harvestcraft:oatsitem", "harvestcraft:ryeitem"), FarmConfig.SHEEP_FOOD.getDefault());
    }

    @Test
    void everyLegacyRawMeatRetainsExactFoodAndNauseaValues() {
        for (String id : java.util.List.of("raw_prime_steak", "raw_prime_beef", "raw_horse", "raw_prime_pork",
                "raw_prime_bacon", "raw_prime_chicken", "raw_chevon", "raw_prime_chevon", "raw_prime_mutton")) {
            assertSame(com.animania.common.item.LegacyRawFoodProfile.RAW,
                    com.animania.common.item.LegacyRawFoodProfile.forItemId(id), id);
        }
        var profile = com.animania.common.item.LegacyRawFoodProfile.RAW;
        assertEquals(1, profile.nutrition());
        assertEquals(1.0F, profile.saturation());
        assertEquals(200, profile.nauseaTicks());
        assertEquals(3, profile.nauseaAmplifier());
        assertEquals(1.0F, profile.effectProbability());
        assertNull(com.animania.common.item.LegacyRawFoodProfile.forItemId("cooked_prime_beef"));
    }

    private static void assertProfilePaths(ModelPart root, com.animania.client.model.LegacyAnimationProfile profile,
                                           String id) {
        java.util.stream.Stream.of(profile.heads(), profile.leftLegs(), profile.rightLegs(), profile.tails(),
                        profile.wings(), profile.bodies(), profile.privateParts(), profile.coloredParts())
                .flatMap(java.util.Arrays::stream)
                .forEach(path -> assertTrue(hasPath(root, path), id + " has missing animation bone " + path));
    }

    private static boolean hasPath(ModelPart root, String path) {
        ModelPart current = root;
        for (String segment : path.split("/")) {
            if (!current.hasChild(segment)) return false;
            current = current.getChild(segment);
        }
        return true;
    }
}
