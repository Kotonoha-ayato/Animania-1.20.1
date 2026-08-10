package com.animania.catsdogs;

import org.junit.jupiter.api.Test;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;

class CatsDogsRegistryTest {
    @Test
    void allPinnedAnimalIdsAreUniqueAndPetFacilitiesArePresent() {
        assertFalse(CatsDogsLegacyIds.ALL.isEmpty());
        assertEquals(CatsDogsLegacyIds.ALL.size(), new HashSet<>(CatsDogsLegacyIds.ALL).size());
        assertTrue(CatsDogsLegacyIds.ALL.stream().anyMatch(id -> id.startsWith("female_")));
    }
}
