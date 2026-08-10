package com.animania.api.data;

import net.minecraft.resources.ResourceLocation;

/** Immutable public view of the state that addons and compatibility layers may inspect. */
public record AnimalSnapshot(
        ResourceLocation type,
        AnimalGender gender,
        AnimalAge age,
        String variant,
        int hunger,
        int thirst,
        boolean sleeping,
        boolean pregnant,
        boolean sterilized) {
    public AnimalSnapshot {
        variant = variant == null || variant.isBlank() ? "default" : variant;
        hunger = Math.max(0, Math.min(100, hunger));
        thirst = Math.max(0, Math.min(100, thirst));
    }
}

