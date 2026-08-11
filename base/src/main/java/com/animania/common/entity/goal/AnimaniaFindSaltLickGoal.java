package com.animania.common.entity.goal;

import com.animania.common.block.AnimaniaSaltLickBlockEntity;
import com.animania.common.config.AnimaniaConfig;
import com.animania.common.entity.AnimaniaAnimalEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/** Path-based replacement for 1.12 GenericAIFindSaltLick. */
public final class AnimaniaFindSaltLickGoal extends Goal {
    private final AnimaniaAnimalEntity animal;
    private BlockPos target;
    private int delay;

    public AnimaniaFindSaltLickGoal(AnimaniaAnimalEntity animal) {
        this.animal = animal;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override public boolean canUse() {
        if (++delay <= configured(AnimaniaConfig.SALT_LICK_TICK, 8000)) return false;
        if (animal.getHealth() >= animal.getMaxHealth() || animal.isPassenger() || animal.isSleeping()
                || (AnimaniaFindMudGoal.supports(animal) && animal.isMuddy())) {
            delay = 0;
            return false;
        }
        if (animal.getRandom().nextInt(3) != 0) return false;
        delay = 0;
        target = findNearest();
        return target != null;
    }

    @Override public void start() {
        if (target != null) animal.getNavigation().moveTo(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D, 1.0D);
    }
    @Override public boolean canContinueToUse() {
        return target != null && animal.getHealth() < animal.getMaxHealth() && !animal.getNavigation().isDone();
    }
    @Override public void tick() {
        if (target == null || animal.distanceToSqr(target.getX() + 0.5D, target.getY() + 0.5D,
                target.getZ() + 0.5D) > 4.0D) return;
        if (animal.level().getBlockEntity(target) instanceof AnimaniaSaltLickBlockEntity lick && lick.use(animal)) {
            animal.setEatingTicks(80);
            animal.getNavigation().stop();
        }
    }
    @Override public void stop() { target = null; }
    public BlockPos target() { return target; }

    private BlockPos findNearest() {
        int range = Math.max(1, configured(AnimaniaConfig.AI_BLOCK_SEARCH_RANGE, 16));
        BlockPos origin = animal.blockPosition();
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-range, -2, -range), origin.offset(range, 2, range))) {
            if (!(animal.level().getBlockEntity(pos) instanceof AnimaniaSaltLickBlockEntity lick) || lick.usesLeft() <= 0) continue;
            double distance = pos.distSqr(origin);
            if (distance < bestDistance) { best = pos.immutable(); bestDistance = distance; }
        }
        return best;
    }
    private static int configured(net.minecraftforge.common.ForgeConfigSpec.IntValue value, int fallback) {
        try { return value.get(); } catch (IllegalStateException ignored) { return fallback; }
    }
}
