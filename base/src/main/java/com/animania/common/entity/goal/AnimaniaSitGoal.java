package com.animania.common.entity.goal;

import com.animania.common.entity.AnimaniaAnimalEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/** 1.12 GenericAISit with the shared sleeping and vehicle safety gates. */
public final class AnimaniaSitGoal extends Goal {
    private final AnimaniaAnimalEntity animal;

    public AnimaniaSitGoal(AnimaniaAnimalEntity animal) {
        this.animal = animal;
        setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP));
    }

    @Override
    public boolean canUse() { return legacyGateAllows(); }

    @Override
    public boolean canContinueToUse() { return legacyGateAllows(); }

    @Override
    public void start() { animal.getNavigation().stop(); }

    @Override
    public void tick() {
        animal.getNavigation().stop();
        animal.setDeltaMovement(0.0D, animal.getDeltaMovement().y, 0.0D);
    }

    public boolean legacyGateAllows() {
        return animal.isTamed() && animal.isSitting() && !animal.isSleeping()
                && !animal.isInWater() && !animal.isLeashed() && !animal.isPassenger();
    }

    public static boolean supports(AnimaniaAnimalEntity animal) {
        return AnimaniaFollowOwnerGoal.supports(animal);
    }
}
