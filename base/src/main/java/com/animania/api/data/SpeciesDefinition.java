package com.animania.api.data;

import net.minecraft.resources.ResourceLocation;

/** Stable descriptor for an Animania species/variant registration. */
public record SpeciesDefinition(ResourceLocation id, String family, AnimalGender defaultGender, float adultWidth,
                                float adultHeight, int gestationTicks) implements com.animania.api.interfaces.AnimaniaType {
    public SpeciesDefinition {
        if (id == null || family == null || family.isBlank()) {
            throw new IllegalArgumentException("Species id and family are required");
        }
        adultWidth = Math.max(0.1f, adultWidth);
        adultHeight = Math.max(0.1f, adultHeight);
        gestationTicks = Math.max(1, gestationTicks);
    }

    @Override
    public String getTypeName() {
        return id.toString();
    }
}
