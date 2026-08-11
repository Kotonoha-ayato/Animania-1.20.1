package com.animania.common.entity.goal;

import com.animania.common.entity.AnimaniaAnimalEntity;
import com.animania.common.entity.AnimaniaLegacyGoalProfiles;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;

/** Idle look suppressed while sleeping or physically resting in pig mud. */
public final class AnimaniaLookIdleGoal extends RandomLookAroundGoal {
    private final AnimaniaAnimalEntity animal;

    public AnimaniaLookIdleGoal(AnimaniaAnimalEntity animal) {
        super(animal);
        this.animal = animal;
    }

    @Override
    public boolean canUse() {
        return legacyGateAllows() && super.canUse();
    }

    public boolean legacyGateAllows() {
        return !animal.isSleeping() && !animal.isStandingInMud()
                && (!animal.isFarmHorse() || animal.isLegacyDaytime());
    }

    public static boolean supports(AnimaniaAnimalEntity animal) {
        return AnimaniaLegacyGoalProfiles.resolve(animal).map(AnimaniaLegacyGoalProfiles.Profile::looksIdle)
                .orElse(false);
    }
}
