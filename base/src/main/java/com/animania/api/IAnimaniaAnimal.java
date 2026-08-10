package com.animania.api;

import com.animania.api.data.AnimalGender;
import com.animania.api.data.AnimalAge;
import com.animania.api.data.AnimalSnapshot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.AgeableMob;

/** Public, implementation-independent contract exposed to addon and probe integrations. */
public interface IAnimaniaAnimal {
    AnimalGender getGender();

    void setGender(AnimalGender gender);

    String getVariantName();

    void setVariantName(String variant);

    int getHunger();

    int getThirst();

    boolean isSleeping();

    default boolean isPlaying() {
        return false;
    }

    boolean isPregnant();

    /** Number of server ticks already spent in the current pregnancy. */
    default int pregnancyTicks() {
        return 0;
    }

    /** Gestation duration in server ticks for this species. */
    default int gestationTicks() {
        return 0;
    }

    boolean isSterilized();

    /** Explicit state mutators are server-authoritative in the base entity. */
    default void setSleeping(boolean sleeping) {
    }

    default void setPlaying(boolean playing) {
    }

    default void setPregnant(boolean pregnant) {
    }

    default void setSterilized(boolean sterilized) {
    }

    /** Stable age view used by addon renderers and compatibility integrations. */
    default AnimalAge age() {
        return asMob().isBaby() ? AnimalAge.BABY : AnimalAge.ADULT;
    }

    default boolean isAdult() {
        return age() == AnimalAge.ADULT && getGender().isAdult();
    }

    /**
     * Care hooks intentionally have defaults so third-party addons compiled
     * against the 3.0 API remain source/binary compatible.  Base entities
     * override them with server-side hunger/thirst/play handling.
     */
    default boolean feed(ItemStack stack) {
        return false;
    }

    default boolean drink(ItemStack stack) {
        return false;
    }

    default boolean play(ItemStack stack) {
        return false;
    }

    default boolean canBreedWith(IAnimaniaAnimal other) {
        return other != null && other != this && getGender().isAdult() && other.getGender().isAdult()
                && getGender() != other.getGender() && !isPregnant() && !other.isPregnant()
                && !isSterilized() && !other.isSterilized();
    }

    AnimalSnapshot snapshot();

    AgeableMob asMob();
}
