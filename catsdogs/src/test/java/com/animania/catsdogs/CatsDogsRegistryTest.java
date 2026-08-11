package com.animania.catsdogs;

import com.animania.common.item.LegacyEggColors;
import com.animania.catsdogs.client.model.CatsDogsLegacyModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import org.junit.jupiter.api.Test;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;

class CatsDogsRegistryTest {
    @Test
    void legacyPetSleepBedDefaultsArePreserved() {
        assertEquals("animania_catsdogs:cat_bed_1", CatsDogsConfig.CAT_BED.getDefault());
        assertEquals("animania_catsdogs:cat_bed_2", CatsDogsConfig.CAT_BED2.getDefault());
        assertEquals("animania_catsdogs:dog_pillow", CatsDogsConfig.DOG_BED.getDefault());
        assertEquals("animania:straw", CatsDogsConfig.DOG_BED2.getDefault());
    }
    @Test
    void legacyPetFoodAliasesAndAddonIdArePreserved() {
        assertEquals(java.util.List.of("minecraft:fish"), CatsDogsConfig.CAT_FOOD.getDefault());
        assertEquals(java.util.List.of("listAllbeefraw"), CatsDogsConfig.DOG_FOOD.getDefault());
        assertEquals(java.util.List.of("minecraft:fish", "listAllbeefraw", "animania_extra:hamster_food"), CatsDogsConfig.PET_BOWL_FOOD.getDefault());
    }
    @Test
    void allPinnedAnimalIdsAreUniqueAndPetFacilitiesArePresent() {
        assertFalse(CatsDogsLegacyIds.ALL.isEmpty());
        assertEquals(CatsDogsLegacyIds.ALL.size(), new HashSet<>(CatsDogsLegacyIds.ALL).size());
        assertTrue(CatsDogsLegacyIds.ALL.stream().anyMatch(id -> id.startsWith("female_")));
    }

    @Test
    void everyPetEggHasItsExactLegacyTintPair() {
        CatsDogsLegacyIds.ALL.forEach(id -> assertNotNull(LegacyEggColors.forEntity(id), id));
        assertEquals(new LegacyEggColors.Colors(7434609, 0), LegacyEggColors.forEntity("tom_american_shorthair"));
        assertEquals(new LegacyEggColors.Colors(2170912, 15658734), LegacyEggColors.forEntity("puppy_husky"));
    }

    @Test
    void convertedModelProfilesNeverAnimateOneBoneAsBothLeftAndRight() {
        CatsDogsLegacyIds.ALL.forEach(id -> {
            var profile = CatsDogsLegacyModelLayers.profile(id);
            var left = new HashSet<>(java.util.List.of(profile.leftLegs()));
            var right = new HashSet<>(java.util.List.of(profile.rightLegs()));
            left.retainAll(right);
            assertTrue(left.isEmpty(), id + " overlapping limbs " + left);
        });
        var dog = CatsDogsLegacyModelLayers.profile("male_husky");
        assertFalse(java.util.Arrays.equals(dog.leftLegs(), dog.rightLegs()));
    }

    @Test
    void everyPetModelBakesGeometryAndEveryAnimationPathResolves() {
        CatsDogsLegacyIds.ALL.forEach(id -> {
            ModelPart root = CatsDogsLegacyModelLayers.create(id).bakeRoot();
            assertTrue(root.getAllParts().anyMatch(part -> !part.isEmpty()), id + " baked with no cubes");
            assertProfilePaths(root, CatsDogsLegacyModelLayers.profile(id), id);
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
