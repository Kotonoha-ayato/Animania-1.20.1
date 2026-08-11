package com.animania.api;

import com.animania.api.data.AnimalContainer;
import com.animania.api.data.EntityGender;
import com.animania.api.interfaces.AnimaniaType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AnimalContainerTest {
    @Test
    void preservesLegacyIdentityEqualityHashAndStringContract() {
        AnimaniaType type = () -> "angus";
        AnimalContainer first = new AnimalContainer(type, EntityGender.FEMALE);
        AnimalContainer same = new AnimalContainer(type, EntityGender.FEMALE);
        AnimalContainer distinctInstance = new AnimalContainer(() -> "angus", EntityGender.FEMALE);
        assertSame(type, first.getType());
        assertEquals(EntityGender.FEMALE, first.getGender());
        assertEquals(first, same);
        assertEquals(first.hashCode(), same.hashCode());
        assertNotEquals(first, distinctInstance);
        assertEquals(type + ":FEMALE", first.toString());
        assertNull(AnimalContainer.fromString("legacy:input"));
    }

    @Test
    void rejectsNullKeysInsteadOfCreatingUnusableMapEntries() {
        AnimaniaType type = () -> "angus";
        assertThrows(NullPointerException.class, () -> new AnimalContainer(null, EntityGender.MALE));
        assertThrows(NullPointerException.class, () -> new AnimalContainer(type, null));
    }
}
