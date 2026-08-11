package com.animania.api.data;

import com.animania.api.interfaces.AnimaniaType;

import java.util.Objects;

/**
 * Historical identity key used by addon egg/type maps.
 *
 * <p>Type comparison deliberately remains reference-based: legacy animal
 * types are enum constants, and changing this to name equality would alter
 * map behavior for third-party implementations.</p>
 */
public final class AnimalContainer {
    private final AnimaniaType type;
    private final EntityGender gender;

    public AnimalContainer(AnimaniaType animalType, EntityGender gender) {
        this.type = Objects.requireNonNull(animalType, "animalType");
        this.gender = Objects.requireNonNull(gender, "gender");
    }

    public AnimaniaType getType() { return type; }
    public EntityGender getGender() { return gender; }

    @Override public String toString() { return type + ":" + gender; }

    /** The 1.12 method was intentionally non-functional; retained for binary/source migration. */
    @Deprecated(forRemoval = false)
    public static AnimalContainer fromString(String ignored) { return null; }

    @Override public boolean equals(Object value) {
        return value instanceof AnimalContainer other && other.gender == gender && other.type == type;
    }

    @Override public int hashCode() { return gender.hashCode() + type.hashCode(); }
}
