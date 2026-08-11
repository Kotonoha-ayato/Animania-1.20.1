package com.animania.common.entity.goal;

import com.animania.common.entity.AnimaniaAnimalEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;

import java.util.function.Predicate;

/** GenericAIAvoidEntity replacement with the legacy sleep/sit gates. */
public final class AnimaniaAvoidEntityGoal<T extends LivingEntity> extends AvoidEntityGoal<T> {
    private final AnimaniaAnimalEntity animal;
    private final float distance;
    private final double farSpeed;
    private final double nearSpeed;

    public AnimaniaAvoidEntityGoal(AnimaniaAnimalEntity animal, Class<T> targetClass,
                                   float distance, double farSpeed, double nearSpeed) {
        super(animal, targetClass, distance, farSpeed, nearSpeed);
        this.animal = animal;
        this.distance = distance;
        this.farSpeed = farSpeed;
        this.nearSpeed = nearSpeed;
    }

    public AnimaniaAvoidEntityGoal(AnimaniaAnimalEntity animal, Class<T> targetClass,
                                   Predicate<LivingEntity> targetSelector, float distance,
                                   double farSpeed, double nearSpeed,
                                   Predicate<LivingEntity> safeTargetSelector) {
        super(animal, targetClass, targetSelector, distance, farSpeed, nearSpeed, safeTargetSelector);
        this.animal = animal;
        this.distance = distance;
        this.farSpeed = farSpeed;
        this.nearSpeed = nearSpeed;
    }

    @Override
    public boolean canUse() {
        return !blocked() && super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        return !blocked() && super.canContinueToUse();
    }

    public boolean legacyGateAllows() { return !blocked(); }
    public float distance() { return distance; }
    public double farSpeed() { return farSpeed; }
    public double nearSpeed() { return nearSpeed; }

    private boolean blocked() {
        return animal.isSleeping() || animal.isSitting() || animal.isPassenger();
    }
}
