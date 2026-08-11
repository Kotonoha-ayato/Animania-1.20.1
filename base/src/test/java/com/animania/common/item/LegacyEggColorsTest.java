package com.animania.common.item;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class LegacyEggColorsTest {
    @Test
    void resolvesExactRoleStrippedAndSpecialBreedColors() {
        assertEquals(new LegacyEggColors.Colors(3028024, 2304560), LegacyEggColors.forEntity("cow_angus"));
        assertEquals(new LegacyEggColors.Colors(15987699, 3944229), LegacyEggColors.forEntity("calf_friesian"));
        assertEquals(new LegacyEggColors.Colors(2039583, 4013373), LegacyEggColors.forEntity("ewe_friesian"));
        assertEquals(new LegacyEggColors.Colors(15987699, 1776411), LegacyEggColors.forEntity("lamb_dorper"));
        assertEquals(new LegacyEggColors.Colors(13948116, 8741209), LegacyEggColors.forEntity("ferret_grey"));
        assertNull(LegacyEggColors.forEntity("cow_random"));
        assertNull(LegacyEggColors.forEntity("dart_frog"));
        assertNull(LegacyEggColors.forEntity(null));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("specialBreedColorCases")
    void specialBreedColorsPreserveOneTwelveConstants(String id, int primary, int secondary) {
        assertEquals(new LegacyEggColors.Colors(primary, secondary), LegacyEggColors.forEntity(id));
    }

    private static Stream<Arguments> specialBreedColorCases() {
        return Stream.of(
                // Farm classes with additional legacy overrides.
                Arguments.of("calf_friesian", 15987699, 3944229),
                Arguments.of("cow_friesian", 15987699, 3944229),
                Arguments.of("bull_friesian", 15987699, 3944229),
                Arguments.of("calf_holstein", 15987699, 2236962),
                Arguments.of("cow_holstein", 15987699, 2236962),
                Arguments.of("bull_holstein", 15987699, 2236962),
                Arguments.of("calf_jersey", 12089918, 16775643),
                Arguments.of("cow_jersey", 12089918, 16775643),
                Arguments.of("bull_jersey", 12089918, 16775643),
                Arguments.of("calf_mooshroom", 12325394, 12627887),
                Arguments.of("cow_mooshroom", 12325394, 12627887),
                Arguments.of("bull_mooshroom", 12325394, 12627887),
                Arguments.of("kid_angora", 16776179, 13814191),
                Arguments.of("doe_angora", 16776179, 13814191),
                Arguments.of("buck_angora", 16776179, 13814191),
                // Cats&Dogs classes with legacy eye/variant overrides.
                Arguments.of("female_chihuahua", 16183788, 394500),
                Arguments.of("male_chihuahua", 16183788, 394500),
                Arguments.of("puppy_chihuahua", 16183788, 394500),
                Arguments.of("female_collie", 4206629, 16579836),
                Arguments.of("male_collie", 4206629, 16579836),
                Arguments.of("puppy_collie", 4206629, 16579836),
                Arguments.of("female_fox", 11361596, 2830613),
                Arguments.of("male_fox", 11361596, 2830613),
                Arguments.of("puppy_fox", 11361596, 2830613),
                Arguments.of("female_labrador", 12623223, 4270368),
                Arguments.of("male_labrador", 12623223, 4270368),
                Arguments.of("puppy_labrador", 12623223, 4270368),
                Arguments.of("female_poodle", 16118509, 11240027),
                Arguments.of("male_poodle", 16118509, 11240027),
                Arguments.of("puppy_poodle", 16118509, 11240027),
                Arguments.of("female_wolf", 12367536, 3288364),
                Arguments.of("male_wolf", 12367536, 3288364),
                Arguments.of("puppy_wolf", 12367536, 3288364),
                // Extra classes with exact non-role IDs.
                Arguments.of("ferret_grey", 13948116, 8741209),
                Arguments.of("ferret_white", 15395298, 16447993),
                Arguments.of("hedgehog", 10451558, 14337943),
                Arguments.of("hedgehog_albino", 12369084, 16777215)
        );
    }
}
