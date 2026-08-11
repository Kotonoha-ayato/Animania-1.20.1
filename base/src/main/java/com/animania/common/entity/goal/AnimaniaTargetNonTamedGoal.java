package com.animania.common.entity.goal;

import com.animania.common.entity.AnimaniaAnimalEntity;
import net.minecraft.world.entity.LivingEntity;

import java.util.function.Predicate;

/** GenericAITargetNonTamed replacement used by optional prey/rodent hooks. */
public final class AnimaniaTargetNonTamedGoal<T extends LivingEntity>
        extends AnimaniaNearestAttackableTargetGoal<T> {
    private final AnimaniaAnimalEntity animal;

    public AnimaniaTargetNonTamedGoal(AnimaniaAnimalEntity animal, Class<T> targetClass,
                                      boolean checkSight, Predicate<? super T> selector) {
        super(animal, targetClass, checkSight,
                target -> !animal.isTamed() && (selector == null || selector.test(targetClass.cast(target))));
        this.animal = animal;
    }

    @Override
    public boolean canUse() {
        return !animal.isTamed() && super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        return !animal.isTamed() && super.canContinueToUse();
    }
}
