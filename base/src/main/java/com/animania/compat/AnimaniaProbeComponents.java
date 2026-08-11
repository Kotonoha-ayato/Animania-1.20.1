package com.animania.compat;

import com.animania.common.entity.AnimaniaAnimalEntity;
import com.animania.api.data.AnimalGender;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** Shared, server-authoritative tooltip content for Jade and The One Probe. */
public final class AnimaniaProbeComponents {
    private AnimaniaProbeComponents() { }

    public static List<Component> animal(AnimaniaAnimalEntity animal) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("jade.animania.animal_state", animal.getHunger(), animal.getThirst()));
        if (animal.getGender() == AnimalGender.MALE || animal.getGender() == AnimalGender.FEMALE) {
            lines.add(Component.translatable("jade.animania.gender." + animal.getGender().name().toLowerCase(java.util.Locale.ROOT)));
        }
        if (animal.parentUuid() != null) lines.add(Component.translatable("text.waila.parent"));
        if (animal.mateUuid() != null) lines.add(Component.translatable("text.waila.mated"));
        if (animal.isPregnant()) {
            lines.add(Component.translatable("jade.animania.pregnancy_remaining",
                    Math.max(0, animal.gestationTicks() - animal.pregnancyTicks())));
        }
        if (animal.isMilkReady()) lines.add(Component.translatable("text.waila.milkable"));
        if (animal.isSheared()) {
            lines.add(Component.translatable("jade.animania.wool_remaining", animal.woolRegrowthTicks()));
        } else if (animal.isShearableAnimal()) {
            lines.add(Component.translatable("text.waila.wool3"));
        }
        if (animal.isPigAnimal()) {
            lines.add(Component.translatable(animal.isPlaying() ? "text.waila.played" : "text.waila.bored"));
        }
        if (animal.isEggLayer()) {
            lines.add(Component.translatable("jade.animania.egg_remaining", animal.eggLayTicks()));
        }
        if (animal.isSleeping()) lines.add(Component.translatable("text.waila.sleeping"));
        if (animal.isSterilized()) lines.add(Component.translatable("text.waila.sterilized"));
        if (animal.isTamed()) lines.add(Component.translatable("jade.animania.tamed"));
        if (animal.isSitting()) lines.add(Component.translatable("text.waila.sitting"));
        return List.copyOf(lines);
    }
}
