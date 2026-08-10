package com.animania.api;

import com.animania.api.data.AnimalAge;
import com.animania.api.data.AnimalGender;
import com.animania.api.data.AnimalSnapshot;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AnimalSnapshotTest {
    @Test
    void clampsPublicStateAndKeepsStableDefaults() {
        AnimalSnapshot snapshot = new AnimalSnapshot(new ResourceLocation("animania_farm", "cow_angus"),
                AnimalGender.FEMALE, AnimalAge.ADULT, "", -10, 200, false, true, false);
        assertEquals("default", snapshot.variant());
        assertEquals(0, snapshot.hunger());
        assertEquals(100, snapshot.thirst());
        assertTrue(snapshot.pregnant());
    }
}

