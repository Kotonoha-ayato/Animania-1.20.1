package com.animania.common.entity.goal;

import com.animania.api.data.AnimalGender;
import com.animania.common.config.AnimaniaConfig;
import com.animania.common.entity.AnimaniaAnimalEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.UUID;
import java.util.function.BooleanSupplier;

/** Server-authoritative courtship and pregnancy replacement for 1.12 GenericAIMate. */
public final class AnimaniaMateGoal extends Goal {
    private final AnimaniaAnimalEntity male;
    private final double speed;
    private final BooleanSupplier startGate;
    private AnimaniaAnimalEntity female;
    private int delay;
    private int courtshipTicks;

    public AnimaniaMateGoal(AnimaniaAnimalEntity male, double speed) {
        this(male, speed, () -> male.getRandom().nextInt(20) != 0);
    }

    /** Injectable start gate keeps the legacy 19/20 attempt deterministic in GameTests. */
    public AnimaniaMateGoal(AnimaniaAnimalEntity male, double speed, BooleanSupplier startGate) {
        this.male = male;
        this.speed = speed;
        this.startGate = startGate;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (++delay <= configured(AnimaniaConfig.AI_TICKS_BETWEEN_FIRINGS, 100)) return false;
        delay = 0;
        if (male.getGender() != AnimalGender.MALE || !male.isAdult() || male.isSterilized()
                || male.isSleeping() || male.isInWater() || !legacyTimeGateAllows()) return false;
        female = findMate();
        if (female == null || !startGate.getAsBoolean()) {
            female = null;
            return false;
        }
        courtshipTicks = 200;
        return true;
    }

    @Override
    public void start() {
        moveTogether();
    }

    @Override
    public boolean canContinueToUse() {
        return female != null && female.isAlive() && courtshipTicks >= 0 && male.canBreedWith(female);
    }

    @Override
    public void tick() {
        if (female == null) return;
        male.getLookControl().setLookAt(female, 10.0F, male.getMaxHeadXRot());
        female.getLookControl().setLookAt(male, 10.0F, female.getMaxHeadXRot());
        if (courtshipTicks-- % 20 == 0) moveTogether();
        if (male.distanceTo(female) <= 1.8F) {
            male.spawnChildFromBreeding((ServerLevel) male.level(), female);
            male.getNavigation().stop();
            female.getNavigation().stop();
            female = null;
        } else if (courtshipTicks < 0) {
            // Legacy failed courtship waits another 2,000 ticks before retrying.
            delay = -2000;
            stop();
        }
    }

    @Override
    public void stop() {
        female = null;
        courtshipTicks = 0;
        male.getNavigation().stop();
    }

    public AnimaniaAnimalEntity targetMate() {
        return female;
    }

    public boolean legacyTimeGateAllows() {
        return !male.isFarmHorse() || male.isLegacyDaytime();
    }

    private void moveTogether() {
        if (female == null) return;
        male.getNavigation().moveTo(female, speed);
        female.getNavigation().moveTo(male, speed);
    }

    private AnimaniaAnimalEntity findMate() {
        UUID reserved = configured(AnimaniaConfig.MALES_MATE_MULTIPLE_FEMALES, false) ? null : male.mateUuid();
        var candidates = male.level().getEntitiesOfClass(AnimaniaAnimalEntity.class,
                male.getBoundingBox().inflate(reserved == null ? 8.0D : 5.0D), candidate -> {
                    if (candidate.getGender() != AnimalGender.FEMALE || !male.canBreedWith(candidate)) return false;
                    if (reserved != null) return reserved.equals(candidate.getUUID());
                    UUID femaleMate = candidate.mateUuid();
                    return configured(AnimaniaConfig.MALES_MATE_MULTIPLE_FEMALES, false)
                            ? femaleMate == null || femaleMate.equals(male.getUUID())
                            : femaleMate == null;
                });
        AnimaniaAnimalEntity result = candidates.stream()
                .min(Comparator.comparingDouble(male::distanceToSqr)).orElse(null);
        // A dead or unloaded reserved mate must not permanently lock the male.
        if (result == null && reserved != null && male.level().getEntitiesOfClass(AnimaniaAnimalEntity.class,
                male.getBoundingBox().inflate(30.0D), candidate -> candidate.getUUID().equals(reserved)).isEmpty()) {
            male.setMateUuid(null);
        }
        return result;
    }

    private static boolean configured(net.minecraftforge.common.ForgeConfigSpec.BooleanValue value, boolean fallback) {
        try { return value.get(); } catch (IllegalStateException ignored) { return fallback; }
    }

    private static int configured(net.minecraftforge.common.ForgeConfigSpec.IntValue value, int fallback) {
        try { return value.get(); } catch (IllegalStateException ignored) { return fallback; }
    }
}
