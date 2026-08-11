package com.animania.common.entity.goal;

import com.animania.common.entity.AnimaniaAnimalEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

/** Server-authoritative GenericAIOwnerHurtTarget replacement for dogs. */
public final class AnimaniaOwnerHurtTargetGoal extends Goal {
    private final AnimaniaAnimalEntity animal;
    private LivingEntity target;
    private int lastTimestamp = -1;

    public AnimaniaOwnerHurtTargetGoal(AnimaniaAnimalEntity animal) {
        this.animal = animal;
        setFlags(EnumSet.of(Flag.TARGET));
    }

    @Override
    public boolean canUse() {
        if (blocked()) return false;
        Player owner = AnimaniaFollowOwnerGoal.resolveOwner(animal);
        if (owner == null) return false;
        LivingEntity candidate = owner.getLastHurtMob();
        int timestamp = owner.getLastHurtMobTimestamp();
        if (candidate == null || !candidate.isAlive() || candidate == animal || timestamp == lastTimestamp) return false;
        target = candidate;
        lastTimestamp = timestamp;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return !blocked() && target != null && target.isAlive();
    }

    @Override
    public void start() {
        animal.setTarget(target);
    }

    @Override
    public void stop() {
        target = null;
    }

    public LivingEntity target() { return target; }

    private boolean blocked() {
        return !animal.isTamed() || animal.isSleeping() || animal.isSitting() || animal.isPassenger();
    }
}
