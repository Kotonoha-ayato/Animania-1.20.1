package com.animania.common.entity.goal;

import com.animania.api.data.AnimalGender;
import com.animania.common.config.AnimaniaConfig;
import com.animania.common.entity.AnimaniaAnimalEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/** UUID-bound daylight parent following replacement for 1.12 GenericAIFollowParents. */
public final class AnimaniaFollowParentGoal extends Goal {
    private final AnimaniaAnimalEntity child;
    private final double speed;
    private AnimaniaAnimalEntity parent;
    private int delay;
    private int navigationDelay;

    public AnimaniaFollowParentGoal(AnimaniaAnimalEntity child, double speed) {
        this.child = child;
        this.speed = speed;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (++delay <= configured(AnimaniaConfig.AI_TICKS_BETWEEN_FIRINGS, 100)) return false;
        delay = 0;
        if (child.getGender() != AnimalGender.CHILD || child.parentUuid() == null
                || child.isSleeping() || !child.level().isDay()) return false;
        if (!(child.level() instanceof ServerLevel server)) return false;
        Entity found = server.getEntity(child.parentUuid());
        if (!(found instanceof AnimaniaAnimalEntity candidate) || !candidate.isAlive()) return false;
        double dx = Math.abs(candidate.getX() - child.getX());
        double dy = Math.abs(candidate.getY() - child.getY());
        double dz = Math.abs(candidate.getZ() - child.getZ());
        if (dx > 20.0D || dy > 8.0D || dz > 20.0D || dx < 3.0D || dz < 3.0D) return false;
        parent = candidate;
        return true;
    }

    @Override
    public void start() {
        navigationDelay = 0;
    }

    @Override
    public boolean canContinueToUse() {
        if (parent == null || !parent.isAlive() || child.isSleeping() || !child.level().isDay()) return false;
        double distance = child.distanceToSqr(parent);
        return distance >= 9.0D && distance <= 256.0D;
    }

    @Override
    public void tick() {
        if (parent != null && --navigationDelay <= 0) {
            navigationDelay = 40;
            child.getNavigation().moveTo(parent, speed);
        }
    }

    @Override
    public void stop() {
        parent = null;
        child.getNavigation().stop();
    }

    public AnimaniaAnimalEntity targetParent() {
        return parent;
    }

    private static int configured(net.minecraftforge.common.ForgeConfigSpec.IntValue value, int fallback) {
        try { return value.get(); } catch (IllegalStateException ignored) { return fallback; }
    }
}
