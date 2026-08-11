package com.animania.common.entity.goal;

import com.animania.common.entity.AnimaniaAnimalEntity;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;

/** Shared sleep/sit-gated hurt-by target behavior for companion animals. */
public final class AnimaniaHurtByTargetGoal extends HurtByTargetGoal {
    private final AnimaniaAnimalEntity animal;

    public AnimaniaHurtByTargetGoal(AnimaniaAnimalEntity animal) {
        super(animal);
        this.animal = animal;
    }

    @Override
    public boolean canUse() {
        return !blocked() && super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        return !blocked() && super.canContinueToUse();
    }

    private boolean blocked() {
        return animal.isSleeping() || animal.isSitting() || animal.isPassenger();
    }
}
