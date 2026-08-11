package com.animania.common.entity.goal;

import com.animania.common.entity.AnimaniaAnimalEntity;
import com.animania.common.entity.AnimaniaLegacyGoalProfiles;
import net.minecraft.world.entity.ai.goal.PanicGoal;

/** Legacy panic behavior, including the cow retaliation exception. */
public final class AnimaniaPanicGoal extends PanicGoal {
    private final AnimaniaAnimalEntity animal;
    private final double legacySpeed;

    public AnimaniaPanicGoal(AnimaniaAnimalEntity animal, double speed) {
        super(animal, speed);
        this.animal = animal;
        this.legacySpeed = speed;
    }

    @Override
    public boolean canUse() {
        if (!legacyGateAllows()) return false;
        if (animal.isOnFire() && animal.isSleeping()) animal.setSleeping(false);
        return super.canUse();
    }

    public boolean legacyGateAllows() {
        return !isCow(animal) || animal.getLastHurtByMob() == null;
    }

    public double legacySpeed() {
        return legacySpeed;
    }

    public static boolean supports(AnimaniaAnimalEntity animal) {
        return AnimaniaLegacyGoalProfiles.resolve(animal).isPresent();
    }

    public static double legacySpeed(AnimaniaAnimalEntity animal) {
        return AnimaniaLegacyGoalProfiles.resolve(animal).orElseThrow().panicSpeed();
    }

    private static boolean isCow(AnimaniaAnimalEntity animal) {
        String path = animal.registryPath();
        return animal.registryNamespace().equals("animania_farm")
                && (path.startsWith("cow_") || path.startsWith("bull_") || path.startsWith("calf_"));
    }
}
