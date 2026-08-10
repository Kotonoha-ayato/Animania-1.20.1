package com.animania.api.data;

/** Legacy enum retained for source compatibility; RANDOM resolves server-side. */
public enum EntityGender {
    MALE, FEMALE, CHILD, RANDOM, NONE;

    public AnimalGender modernDefault() {
        return switch (this) {
            case FEMALE -> AnimalGender.FEMALE;
            case MALE, NONE -> AnimalGender.MALE;
            case CHILD -> AnimalGender.CHILD;
            case RANDOM -> AnimalGender.CHILD;
        };
    }
}

