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
        ExtraLegacyIds.ALL.stream().filter(id -> !id.equals("dartfrog"))
                .forEach(id -> assertNotNull(LegacyEggColors.forEntity(id), id));
        assertNull(LegacyEggColors.forEntity("dartfrog"), "1.12 dart frogs explicitly disabled egg tinting");
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
