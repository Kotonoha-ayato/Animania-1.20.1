package com.animania.extra;

import com.animania.common.item.LegacyEggColors;
import com.animania.extra.client.model.ExtraLegacyModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import org.junit.jupiter.api.Test;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;

class ExtraRegistryTest {
    @Test
    void allLegacySoundEventsUseLegalModernIds() {
        assertEquals(52, ExtraSoundCatalog.IDS.size());
        assertEquals(52, new HashSet<>(ExtraSoundCatalog.IDS).size());
        assertTrue(ExtraSoundCatalog.IDS.stream().allMatch(id -> id.equals(id.toLowerCase(java.util.Locale.ROOT))));
        assertTrue(ExtraSoundCatalog.IDS.containsAll(java.util.List.of(
                "hamsterdeath", "dartfrogliving4", "hedgehoghurt2", "peacock10")));
    }

    @Test
    void legacySleepBedDefaultsMapBlockStrawToBaseStraw() {
        for (String family : java.util.List.of("ferret", "hamster", "hedgehog", "peacock", "rabbit")) {
            assertEquals("animania:straw", ExtraConfig.BED_BLOCKS.get(family + "Bed").getDefault(), family);
        }
        assertEquals("", ExtraConfig.BED_BLOCKS.get("hamsterBed2").getDefault());
        for (String family : java.util.List.of("ferret", "hedgehog", "peacock", "rabbit")) {
            assertEquals("minecraft:grass_block", ExtraConfig.BED_BLOCKS.get(family + "Bed2").getDefault(), family);
        }
    }

    @Test
    void legacyFoodDefaultsPreserveOptionalModsAndRepairBrokenAnimaniaIds() {
        assertEquals(java.util.List.of("minecraft:mutton", "minecraft:egg", "animania_farm:brown_egg", "animania_extra:peacock_egg_blue", "animania_extra:peacock_egg_white", "animania_farm:raw_prime_mutton", "animania_extra:raw_prime_rabbit", "minecraft:rabbit", "minecraft:chicken", "animania_farm:raw_prime_chicken"), ExtraConfig.FERRET_FOOD.getDefault());
        assertEquals(java.util.List.of("animania_extra:hamster_food", "minecraft:wheat_seeds", "minecraft:melon_seeds", "minecraft:beetroot_seeds", "minecraft:pumpkin_seeds", "simplecorn:corncob", "biomesoplenty:turnip_seeds", "harvestcraft:cornitem", "minecraft:apple"), ExtraConfig.HAMSTER_FOOD.getDefault());
        assertEquals(java.util.List.of("minecraft:carrot", "minecraft:beetroot", "minecraft:egg", "animania_farm:brown_egg", "animania_extra:peacock_egg_blue", "animania_extra:peacock_egg_white", "animania_farm:raw_prime_mutton", "animania_extra:raw_prime_rabbit", "minecraft:rabbit", "minecraft:chicken", "animania_farm:raw_prime_chicken", "minecraft:apple"), ExtraConfig.HEDGEHOG_FOOD.getDefault());
        assertEquals(java.util.List.of("minecraft:wheat_seeds", "minecraft:melon_seeds", "minecraft:beetroot_seeds", "minecraft:pumpkin_seeds", "simplecorn:corncob", "biomesoplenty:turnip_seeds", "harvestcraft:cornitem"), ExtraConfig.PEACOCK_FOOD.getDefault());
    }
    @Test
    void allPinnedAnimalIdsAreUniqueAndHamsterFacilityIsRegistered() {
        assertFalse(ExtraLegacyIds.ALL.isEmpty());
        assertEquals(ExtraLegacyIds.ALL.size(), new HashSet<>(ExtraLegacyIds.ALL).size());
        assertTrue(ExtraLegacyIds.ALL.stream().anyMatch(id -> id.contains("hamster")));
    }

    @Test
    void everyLegacyRawExtraMeatRetainsExactFoodAndNauseaValues() {
        for (String id : java.util.List.of("raw_prime_rabbit", "raw_frog_legs", "raw_peacock", "raw_prime_peacock")) {
            assertSame(com.animania.common.item.LegacyRawFoodProfile.RAW,
                    com.animania.common.item.LegacyRawFoodProfile.forItemId(id), id);
        }
        var profile = com.animania.common.item.LegacyRawFoodProfile.RAW;
        assertEquals(1, profile.nutrition());
        assertEquals(1.0F, profile.saturation());
        assertEquals(200, profile.nauseaTicks());
        assertEquals(3, profile.nauseaAmplifier());
        assertEquals(1.0F, profile.effectProbability());
    }

    @Test
    void everyTintedAnimalEggHasItsExactLegacyTintPair() {
        ExtraLegacyIds.ALL.forEach(id -> assertNotNull(LegacyEggColors.forEntity(id), id));
        // The pinned 1.12 model references egg_frog_dart, but that PNG is
        // absent from the source tree. The modern port uses deterministic
        // tinted layers instead of silently substituting the manual icon.
        assertEquals(new LegacyEggColors.Colors(1728436, 15914571), LegacyEggColors.forEntity("dartfrog"));
        assertEquals(new LegacyEggColors.Colors(0, 16777215), LegacyEggColors.forEntity("doe_dutch"));
        assertEquals(new LegacyEggColors.Colors(2446225, 4361491), LegacyEggColors.forEntity("peacock_blue"));
    }

    @Test
    void convertedModelProfilesNeverAnimateOneBoneAsBothLeftAndRight() {
        ExtraLegacyIds.ALL.forEach(id -> {
            var profile = ExtraLegacyModelLayers.profile(id);
            var left = new HashSet<>(java.util.List.of(profile.leftLegs()));
            var right = new HashSet<>(java.util.List.of(profile.rightLegs()));
            left.retainAll(right);
            assertTrue(left.isEmpty(), id + " overlapping limbs " + left);
        });
        var rabbit = ExtraLegacyModelLayers.profile("buck_lop");
        assertFalse(java.util.Arrays.equals(rabbit.leftLegs(), rabbit.rightLegs()));
    }

    @Test
    void specialAnimalGaitsMatchThePinnedOneTwelveModels() {
        var rabbits = java.util.List.of(
                "buck_chinchilla", "doe_chinchilla", "kit_chinchilla",
                "buck_cottontail", "doe_cottontail", "kit_cottontail",
                "buck_dutch", "doe_dutch", "kit_dutch",
                "buck_havana", "doe_havana", "kit_havana",
                "buck_jack", "doe_jack", "kit_jack",
                "buck_lop", "doe_lop", "kit_lop",
                "buck_new_zealand", "doe_new_zealand", "kit_new_zealand",
                "buck_rex", "doe_rex", "kit_rex");
        for (String id : rabbits) {
            var profile = ExtraLegacyModelLayers.profile(id);
            assertArrayEquals(new String[]{"back_leg_l1", "back_leg_l2", "back_leg_r1", "back_leg_r2"}, profile.leftLegs(), id);
            assertArrayEquals(new String[]{"leg_l1", "leg_r1"}, profile.rightLegs(), id);
        }

        for (String id : java.util.List.of("ferret_grey", "ferret_white")) {
            var profile = ExtraLegacyModelLayers.profile(id);
            assertArrayEquals(new String[]{"paw_l_f", "paw_r_b"}, profile.leftLegs(), id);
            assertArrayEquals(new String[]{"paw_r_f", "paw_l_b"}, profile.rightLegs(), id);
        }

        var hamster = ExtraLegacyModelLayers.profile("hamster");
        assertArrayEquals(new String[]{"hamster_leg_back_right", "hamster_leg_front_left"}, hamster.leftLegs());
        assertArrayEquals(new String[]{"hamster_leg_back_left", "hamster_leg_front_right"}, hamster.rightLegs());

        for (String id : java.util.List.of("frog", "dartfrog", "toad")) {
            var profile = ExtraLegacyModelLayers.profile(id);
            assertArrayEquals(new String[]{}, profile.leftLegs(), id);
            assertArrayEquals(new String[]{}, profile.rightLegs(), id);
        }
    }

    @Test
    void everyAnimalModelBakesGeometryAndEveryAnimationPathResolves() {
        ExtraLegacyIds.ALL.forEach(id -> {
            ModelPart root = ExtraLegacyModelLayers.create(id).bakeRoot();
            assertTrue(root.getAllParts().anyMatch(part -> !part.isEmpty()), id + " baked with no cubes");
            assertProfilePaths(root, ExtraLegacyModelLayers.profile(id), id);
        });
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
