package com.animania.common.entity.goal;

import com.animania.common.entity.AnimaniaAnimalEntity;
import com.animania.common.entity.AnimaniaLegacyGoalProfiles;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.player.Player;

/** Player observation suppressed while sleeping or physically resting in mud. */
public final class AnimaniaWatchClosestGoal extends LookAtPlayerGoal {
    private final AnimaniaAnimalEntity animal;

    public AnimaniaWatchClosestGoal(AnimaniaAnimalEntity animal) {
        super(animal, Player.class, 6.0F);
        this.animal = animal;
    }

    @Override
    public boolean canUse() {
        return legacyGateAllows() && super.canUse();
    }

    public boolean legacyGateAllows() {
        return !animal.isSleeping() && !animal.isStandingInMud();
    }

    public static boolean supports(AnimaniaAnimalEntity animal) {
        return AnimaniaLegacyGoalProfiles.resolve(animal).map(AnimaniaLegacyGoalProfiles.Profile::watchesPlayers)
                .orElse(false);
    }
}
