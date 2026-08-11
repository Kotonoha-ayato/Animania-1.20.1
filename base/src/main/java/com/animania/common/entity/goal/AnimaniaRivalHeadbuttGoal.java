package com.animania.common.entity.goal;

import com.animania.common.config.AnimaniaConfig;
import com.animania.common.entity.AnimaniaAnimalEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;
import java.util.UUID;

/** Server-side rivalry/headbutt loop shared by Animania bucks and rams. */
public final class AnimaniaRivalHeadbuttGoal extends Goal {
    private final AnimaniaAnimalEntity animal;
    private final double speed;
    private int delay;
    private int fightTicks;

    public AnimaniaRivalHeadbuttGoal(AnimaniaAnimalEntity animal) { this(animal, 1.05D); }

    public AnimaniaRivalHeadbuttGoal(AnimaniaAnimalEntity animal, double speed) {
        this.animal = animal;
        this.speed = speed;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    public static boolean supports(AnimaniaAnimalEntity animal) {
        if (!"animania_farm".equals(animal.registryNamespace()) || !animal.isAdult()) return false;
        String path = animal.registryPath();
        return path.startsWith("buck_") || path.startsWith("ram_");
    }

    private boolean sameRivalFamily(AnimaniaAnimalEntity other) {
        if (other == null || !supports(other)) return false;
        return animal.registryPath().startsWith("buck_") == other.registryPath().startsWith("buck_");
    }

    @Override
    public boolean canUse() {
        if (animal.level().isClientSide || !supports(animal) || animal.isSleeping()) return false;
        if (animal.isFighting() && rival() != null) return true;
        if (++delay <= configured(AnimaniaConfig.AI_TICKS_BETWEEN_FIRINGS, 100) * 20) return false;
        delay = 0;
        if (animal.getRandom().nextInt(20) != 0) return false;
        AnimaniaAnimalEntity candidate = animal.level().getEntitiesOfClass(AnimaniaAnimalEntity.class,
                animal.getBoundingBox().inflate(10.0D), other -> other != animal && sameRivalFamily(other)
                        && !other.isSleeping() && !other.isFighting()).stream().findFirst().orElse(null);
        if (candidate == null) return false;
        fightTicks = 100 + animal.getRandom().nextInt(50);
        animal.setRivalUuid(candidate.getUUID());
        candidate.setRivalUuid(animal.getUUID());
        animal.setFighting(true);
        candidate.setFighting(true);
        animal.setTarget(candidate);
        candidate.setTarget(animal);
        return true;
    }

    @Override public boolean canContinueToUse() {
        AnimaniaAnimalEntity rival = rival();
        return animal.isFighting() && rival != null && rival.isAlive() && !animal.isSleeping();
    }

    @Override public void start() { if (fightTicks <= 0) fightTicks = 100; }

    @Override
    public void tick() {
        AnimaniaAnimalEntity rival = rival();
        if (rival == null) { stop(); return; }
        animal.getLookControl().setLookAt(rival, 10.0F, animal.getMaxHeadXRot());
        animal.getNavigation().moveTo(rival, speed);
        if (animal.distanceToSqr(rival) <= 3.0D && animal.tickCount % 20 == 0) animal.doHurtTarget(rival);
        if (--fightTicks <= 0) stop();
    }

    @Override
    public void stop() {
        AnimaniaAnimalEntity rival = rival();
        animal.setFighting(false);
        animal.setRivalUuid(null);
        animal.setTarget(null);
        animal.getNavigation().stop();
        if (rival != null && animal.getUUID().equals(rival.getRivalUuid())) {
            rival.setFighting(false);
            rival.setRivalUuid(null);
            rival.setTarget(null);
        }
        fightTicks = 0;
    }

    private AnimaniaAnimalEntity rival() {
        UUID id = animal.getRivalUuid();
        if (id == null) return null;
        return animal.level().getEntitiesOfClass(AnimaniaAnimalEntity.class,
                animal.getBoundingBox().inflate(128.0D), entity -> id.equals(entity.getUUID()))
                .stream().findFirst().filter(this::sameRivalFamily).orElse(null);
    }

    private static int configured(net.minecraftforge.common.ForgeConfigSpec.IntValue value, int fallback) {
        try { return value.get(); } catch (IllegalStateException ignored) { return fallback; }
    }
}
