package com.animania.api.interfaces;

import com.animania.api.data.SpeciesDefinition;

/** Compatibility facade retaining the historical package name. */
public interface IAnimaniaAnimal extends com.animania.api.IAnimaniaAnimal {
    default AnimaniaType getAnimalType() {
        return new AnimaniaType() {
            @Override
            public String getTypeName() {
                return snapshot().type().toString();
            }
        };
    }
}

