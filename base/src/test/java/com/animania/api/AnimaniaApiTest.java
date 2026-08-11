package com.animania.api;

import com.animania.api.data.AnimalGender;
import com.animania.api.data.SpeciesDefinition;
import com.animania.api.interfaces.IAnimaniaAnimal;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AnimaniaApiTest {
    @Test
    void speciesRegistrationAndAddonQueryAreStable() {
        ResourceLocation id = new ResourceLocation("test_addon", "test_animal");
        AnimaniaApi.registerSpecies(new SpeciesDefinition(id, "test", AnimalGender.FEMALE, 0.8f, 1.0f, 120));
        assertTrue(AnimaniaApi.species(id).isPresent());
        assertEquals(AnimalGender.FEMALE, AnimaniaApi.species(id).orElseThrow().defaultGender());
        assertTrue(AnimaniaApi.speciesIds().contains(id));
        assertTrue(AnimaniaApi.hasSpecies(id));
        assertTrue(AnimaniaApi.speciesForAddon("test_addon").stream().anyMatch(species -> species.id().equals(id)));
        assertFalse(AnimaniaApi.isAnimaniaAnimal(new Object()));
    }

    @Test
    void rootApiImplementationsAreRecognisedWithoutLegacyPackage() {
        ResourceLocation id = new ResourceLocation("test_addon", "root_animal");
        if (!AnimaniaApi.hasSpecies(id)) {
            AnimaniaApi.registerSpecies(new SpeciesDefinition(id, "test", AnimalGender.MALE, 0.8f, 1.0f, 120));
        }
        com.animania.api.IAnimaniaAnimal animal = new com.animania.api.IAnimaniaAnimal() {
            @Override public AnimalGender getGender() { return AnimalGender.MALE; }
            @Override public void setGender(AnimalGender gender) { }
            @Override public String getVariantName() { return "default"; }
            @Override public void setVariantName(String variant) { }
            @Override public int getHunger() { return 100; }
            @Override public int getThirst() { return 100; }
            @Override public boolean isSleeping() { return false; }
            @Override public boolean isPregnant() { return false; }
            @Override public boolean isSterilized() { return false; }
            @Override public com.animania.api.data.AnimalSnapshot snapshot() {
                return new com.animania.api.data.AnimalSnapshot(
                        new ResourceLocation("test_addon", "root_animal"), AnimalGender.MALE,
                        com.animania.api.data.AnimalAge.ADULT, "default", 100, 100, false, false, false);
            }
            @Override public net.minecraft.world.entity.AgeableMob asMob() { return null; }
        };
        assertTrue(AnimaniaApi.isAnimaniaAnimal(animal));
        assertTrue(AnimaniaApi.speciesOf(animal).isPresent());
        assertFalse(animal.isTamed());
        assertFalse(animal.isInBall());
    }

    @Test
    void defaultCareHooksAreSafeForThirdPartyImplementations() {
        IAnimaniaAnimal animal = new IAnimaniaAnimal() {
            @Override public AnimalGender getGender() { return AnimalGender.MALE; }
            @Override public void setGender(AnimalGender gender) { }
            @Override public String getVariantName() { return "default"; }
            @Override public void setVariantName(String variant) { }
            @Override public int getHunger() { return 100; }
            @Override public int getThirst() { return 100; }
            @Override public boolean isSleeping() { return false; }
            @Override public boolean isPregnant() { return false; }
            @Override public boolean isSterilized() { return false; }
            @Override public com.animania.api.data.AnimalSnapshot snapshot() { return null; }
            @Override public net.minecraft.world.entity.AgeableMob asMob() { return null; }
        };
        assertFalse(animal.feed(null));
        assertFalse(animal.drink(null));
        assertFalse(animal.play(null));
    }
}
