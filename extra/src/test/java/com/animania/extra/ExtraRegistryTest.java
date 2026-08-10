package com.animania.extra;

import org.junit.jupiter.api.Test;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;

class ExtraRegistryTest {
    @Test
    void allPinnedAnimalIdsAreUniqueAndHamsterFacilityIsRegistered() {
        assertFalse(ExtraLegacyIds.ALL.isEmpty());
        assertEquals(ExtraLegacyIds.ALL.size(), new HashSet<>(ExtraLegacyIds.ALL).size());
        assertTrue(ExtraLegacyIds.ALL.stream().anyMatch(id -> id.contains("hamster")));
    }
}
