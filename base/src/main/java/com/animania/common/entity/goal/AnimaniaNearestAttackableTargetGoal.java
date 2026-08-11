package com.animania.common.entity.goal;

import com.animania.common.entity.AnimaniaAnimalEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.phys.AABB;

import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

/**
 * Modern target selector with the generic Animania sleep/sit gates preserved.
 * The predicate and chance are delegated to Forge's server-side targeting
 * conditions, so no client-side target state can affect combat.
 */
public class AnimaniaNearestAttackableTargetGoal<T extends LivingEntity>
        extends NearestAttackableTargetGoal<T> {
    protected final AnimaniaAnimalEntity animal;
    private final Predicate<LivingEntity> selector;
    private final int chance;
    private T selectedTarget;

    public AnimaniaNearestAttackableTargetGoal(AnimaniaAnimalEntity animal, Class<T> targetClass,
                                               boolean checkSight) {
        super(animal, targetClass, checkSight);
        this.animal = animal;
        this.selector = target -> true;
        this.chance = 10;
    }

    public AnimaniaNearestAttackableTargetGoal(AnimaniaAnimalEntity animal, Class<T> targetClass,
                                               boolean checkSight, Predicate<LivingEntity> selector) {
        super(animal, targetClass, checkSight, selector);
        this.animal = animal;
        this.selector = selector == null ? target -> true : selector;
        this.chance = 10;
    }

    public AnimaniaNearestAttackableTargetGoal(AnimaniaAnimalEntity animal, Class<T> targetClass,
                                               int chance, boolean checkSight, boolean onlyNearby,
                                               Predicate<LivingEntity> selector) {
        super(animal, targetClass, chance, checkSight, onlyNearby, selector);
        this.animal = animal;
        this.selector = selector == null ? target -> true : selector;
        this.chance = Math.max(0, chance);
    }

    @Override
    public boolean canUse() {
        if (blocked() || (chance > 0 && animal.getRandom().nextInt(chance) != 0)) return false;
        AABB area = getTargetSearchArea(animal.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE));
        List<T> candidates = animal.level().getEntitiesOfClass(targetType, area,
                candidate -> candidate.isAlive() && !candidate.isSpectator()
                        && selector.test(candidate) && animal.canAttack(candidate));
        candidates.sort(Comparator.comparingDouble(animal::distanceToSqr));
        selectedTarget = candidates.isEmpty() ? null : candidates.get(0);
        return selectedTarget != null;
    }

    @Override
    public boolean canContinueToUse() {
        return !blocked() && selectedTarget != null && selectedTarget.isAlive()
                && animal.getTarget() == selectedTarget;
    }

    @Override
    public void start() {
        if (selectedTarget != null) {
            setTarget(selectedTarget);
            super.start();
        }
    }

    @Override
    public void stop() {
        selectedTarget = null;
        super.stop();
    }

    public T selectedTarget() { return selectedTarget; }

    protected boolean blocked() {
        return animal.isSleeping() || animal.isSitting() || animal.isPassenger();
    }
}
