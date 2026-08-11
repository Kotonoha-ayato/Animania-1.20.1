package com.animania.common.entity.goal;

import com.animania.common.entity.AnimaniaAnimalEntity;
import com.animania.common.entity.AnimaniaLegacyGoalProfiles;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;

/** 1.12 random stroll with its 0.001 water-biased chance and sleep gate. */
public final class AnimaniaWanderAvoidWaterGoal extends WaterAvoidingRandomStrollGoal {
    private final AnimaniaAnimalEntity animal;
    private final double legacySpeed;

    public AnimaniaWanderAvoidWaterGoal(AnimaniaAnimalEntity animal, double speed) {
        super(animal, speed, 0.001F);
        this.animal = animal;
        this.legacySpeed = speed;
    }

    @Override
    public boolean canUse() {
        return legacyGateAllows() && super.canUse();
    }

    public boolean legacyGateAllows() {
        return !animal.isSleeping() && (!animal.isFarmHorse()
                || (animal.isLegacyDaytime() && !animal.isPullingVehicle()));
    }

    public double legacySpeed() {
        return legacySpeed;
    }

    public static boolean supports(AnimaniaAnimalEntity animal) {
        return AnimaniaLegacyGoalProfiles.resolve(animal).map(p -> p.wanderSpeed() != null).orElse(false);
    }

    public static double legacySpeed(AnimaniaAnimalEntity animal) {
        return AnimaniaLegacyGoalProfiles.resolve(animal).map(AnimaniaLegacyGoalProfiles.Profile::wanderSpeed)
                .orElseThrow();
    }
}
