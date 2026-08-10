package com.animania.farm;

import org.junit.jupiter.api.Test;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;

class FarmRegistryTest {
    @Test
    void allPinnedAnimalIdsAreUniqueAndContentHasModernEntries() {
        assertFalse(FarmLegacyIds.ALL.isEmpty());
        assertEquals(FarmLegacyIds.ALL.size(), new HashSet<>(FarmLegacyIds.ALL).size());
        assertTrue(FarmLegacyIds.ALL.stream().anyMatch(id -> id.startsWith("cow_")));
    }
}
