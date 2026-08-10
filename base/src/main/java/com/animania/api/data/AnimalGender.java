package com.animania.api.data;

public enum AnimalGender {
    MALE,
    FEMALE,
    CHILD;

    public boolean isAdult() {
        return this != CHILD;
    }
}

