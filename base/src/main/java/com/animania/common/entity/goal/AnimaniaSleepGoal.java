package com.animania.common.entity.goal;

import com.animania.common.config.AnimaniaConfig;
import com.animania.common.entity.AnimaniaAnimalEntity;
import com.animania.common.entity.AnimaniaSleepProfiles;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.Block;

import java.util.EnumSet;

/** Bed-seeking sleep AI preserving the 1.12 timing, weather and wake rules. */
public final class AnimaniaSleepGoal extends Goal {
    private final AnimaniaAnimalEntity animal;
    private BlockPos bedPos;
    private int delay;
    private int walkTries;

    public AnimaniaSleepGoal(AnimaniaAnimalEntity animal) {
        this.animal = animal;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        AnimaniaSleepProfiles.Profile profile = AnimaniaSleepProfiles.resolve(animal).orElse(null);
        if (profile == null || !configured(AnimaniaConfig.ANIMALS_SLEEP, true)) {
            wake();
            return false;
        }
        long time = animal.level().getDayTime();
        if (animal.isSleeping()) {
            if (!profile.shouldSleep(time) || animal.isOnFire() || animal.isSitting()
                    || animal.isLeashed() || animal.isPassenger()
                    || (animal.level().isRainingAt(animal.blockPosition())
                    && animal.level().canSeeSky(animal.blockPosition()))) {
                wake();
            }
            return false;
        }
        if (animal.isSitting() || animal.isLeashed() || animal.isPassenger() || animal.isOnFire()
                || animal.level().isRainingAt(animal.blockPosition()) || !profile.shouldSleep(time)) return false;
        if (++delay <= configured(AnimaniaConfig.AI_TICKS_BETWEEN_FIRINGS, 100) + animal.getRandom().nextInt(100)) {
            return false;
        }
        delay = 0;
        if (animal.getRandom().nextInt(3) != 0) return false;
        bedPos = findBed(profile);
        return bedPos != null;
    }

    @Override
    public void start() {
        walkTries = 0;
        moveToBed();
    }

    @Override
    public boolean canContinueToUse() {
        AnimaniaSleepProfiles.Profile profile = AnimaniaSleepProfiles.resolve(animal).orElse(null);
        return profile != null && bedPos != null && !animal.isSleeping() && profile.shouldSleep(animal.level().getDayTime())
                && isConfiguredBed(profile, bedPos) && !animal.isOnFire() && !animal.isLeashed()
                && !animal.isPassenger() && !animal.isSitting();
    }

    @Override
    public void tick() {
        if (bedPos == null) return;
        if (animal.distanceToSqr(bedPos.getX() + 0.5D, bedPos.getY() + 1.0D, bedPos.getZ() + 0.5D) <= 2.25D) {
            animal.getNavigation().stop();
            animal.setSleeping(true);
            animal.setDeltaMovement(0.0D, animal.getDeltaMovement().y, 0.0D);
        } else if (animal.getNavigation().isDone()) {
            if (++walkTries > 100) {
                bedPos = null;
            } else if (walkTries % 40 == 0) {
                moveToBed();
            }
        }
    }

    @Override
    public void stop() {
        bedPos = null;
        walkTries = 0;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    public BlockPos targetBed() {
        return bedPos;
    }

    private BlockPos findBed(AnimaniaSleepProfiles.Profile profile) {
        Block primary = profile.primaryBlock();
        Block secondary = profile.secondaryBlock();
        if (primary == null && secondary == null) return null;
        int range = Math.max(1, configured(AnimaniaConfig.AI_BLOCK_SEARCH_RANGE, 16));
        int verticalRange = Math.max(1, range / 2);
        BlockPos origin = animal.blockPosition();
        BlockPos bestPrimary = null;
        BlockPos bestSecondary = null;
        double primaryDistance = Double.MAX_VALUE;
        double secondaryDistance = Double.MAX_VALUE;
        for (BlockPos candidate : BlockPos.betweenClosed(origin.offset(-range, -verticalRange, -range),
                origin.offset(range, verticalRange, range))) {
            Block block = animal.level().getBlockState(candidate).getBlock();
            if ((block != primary && block != secondary) || !animal.level().getBlockState(candidate.above()).isAir()) continue;
            double distance = candidate.distSqr(origin);
            if (block == primary && distance < primaryDistance) {
                bestPrimary = candidate.immutable();
                primaryDistance = distance;
            } else if (block == secondary && distance < secondaryDistance) {
                bestSecondary = candidate.immutable();
                secondaryDistance = distance;
            }
        }
        return bestPrimary != null ? bestPrimary : bestSecondary;
    }

    private void wake() {
        if (animal.isSleeping()) animal.setSleeping(false);
    }

    private void moveToBed() {
        if (bedPos != null) animal.getNavigation().moveTo(
                bedPos.getX() + 0.5D, bedPos.getY() + 1.0D, bedPos.getZ() + 0.5D, 0.8D);
    }

    private boolean isConfiguredBed(AnimaniaSleepProfiles.Profile profile, BlockPos position) {
        Block block = animal.level().getBlockState(position).getBlock();
        return block == profile.primaryBlock() || block == profile.secondaryBlock();
    }

    private static boolean configured(net.minecraftforge.common.ForgeConfigSpec.BooleanValue value, boolean fallback) {
        try { return value.get(); } catch (IllegalStateException ignored) { return fallback; }
    }

    private static int configured(net.minecraftforge.common.ForgeConfigSpec.IntValue value, int fallback) {
        try { return value.get(); } catch (IllegalStateException ignored) { return fallback; }
    }
}
