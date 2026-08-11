package com.animania.common.entity.goal;

import com.animania.common.entity.AnimaniaAnimalEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

/**
 * Native replacement for 1.12's {@code GenericAIPlay}. Only kittens play
 * with kittens and puppies with puppies. One animal chases while the other
 * runs away; touching swaps the roles.
 */
public final class AnimaniaPlayGoal extends Goal {
    private final AnimaniaAnimalEntity animal;
    private AnimaniaAnimalEntity pendingMate;
    private AnimaniaAnimalEntity playmate;
    private boolean running;
    private boolean chaser;

    public AnimaniaPlayGoal(AnimaniaAnimalEntity animal) {
        this.animal = animal;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (running) return validPair();
        if (!canPlay(animal) || animal.getRandom().nextDouble() >= 0.2D) return false;
        pendingMate = nearbyAvailableMates().stream()
                .min(Comparator.comparingDouble(animal::distanceToSqr)).orElse(null);
        return pendingMate != null;
    }

    @Override
    public boolean canContinueToUse() {
        return running && validPair() && animal.getRandom().nextDouble() >= 0.1D;
    }

    @Override
    public void start() {
        if (running || pendingMate == null) return;
        AnimaniaPlayGoal other = pendingMate.getPlayGoal();
        if (other == null || other.running || !canPlay(pendingMate)) {
            pendingMate = null;
            return;
        }
        playmate = pendingMate;
        pendingMate = null;
        running = true;
        chaser = true;
        other.playmate = animal;
        other.pendingMate = null;
        other.running = true;
        other.chaser = false;
        animal.setPlaying(true);
        playmate.setPlaying(true);
    }

    @Override
    public void tick() {
        if (!validPair()) {
            stop();
            return;
        }
        animal.setPlaying(true);
        if (chaser) {
            animal.getNavigation().moveTo(playmate, 1.0D);
            if (animal.distanceToSqr(playmate) <= 0.25D) {
                AnimaniaPlayGoal other = playmate.getPlayGoal();
                chaser = false;
                if (other != null) other.chaser = true;
            }
        } else if (animal.getNavigation().isDone()) {
            Vec3 target = DefaultRandomPos.getPosAway(animal, 16, 7, playmate.position());
            if (target != null) animal.getNavigation().moveTo(target.x, target.y, target.z, 1.0D);
        }
    }

    @Override
    public void stop() {
        AnimaniaAnimalEntity oldMate = playmate;
        clearLocal();
        if (oldMate != null) {
            AnimaniaPlayGoal other = oldMate.getPlayGoal();
            if (other != null && other.playmate == animal) other.clearLocal();
        }
    }

    public boolean isRunning() {
        return running;
    }

    public boolean isChaser() {
        return chaser;
    }

    public AnimaniaAnimalEntity playmate() {
        return playmate;
    }

    private void clearLocal() {
        animal.getNavigation().stop();
        animal.setPlaying(false);
        pendingMate = null;
        playmate = null;
        running = false;
        chaser = false;
    }

    private boolean validPair() {
        return playmate != null && playmate.isAlive() && canPlay(animal) && canPlay(playmate)
                && samePlayGroup(animal, playmate) && animal.distanceToSqr(playmate) <= 256.0D;
    }

    private List<AnimaniaAnimalEntity> nearbyAvailableMates() {
        return animal.level().getEntitiesOfClass(AnimaniaAnimalEntity.class,
                animal.getBoundingBox().inflate(5.0D), candidate -> {
                    AnimaniaPlayGoal goal = candidate.getPlayGoal();
                    return candidate != animal && samePlayGroup(animal, candidate) && canPlay(candidate)
                            && goal != null && !goal.running;
                });
    }

    private static boolean canPlay(AnimaniaAnimalEntity candidate) {
        return candidate.isAlive() && !candidate.isSleeping() && !candidate.isSitting()
                && !candidate.isLeashed() && !candidate.isPassenger();
    }

    private static boolean samePlayGroup(AnimaniaAnimalEntity first, AnimaniaAnimalEntity second) {
        String a = registryPath(first);
        String b = registryPath(second);
        return (a.startsWith("kitten_") && b.startsWith("kitten_"))
                || (a.startsWith("puppy_") && b.startsWith("puppy_"));
    }

    public static boolean supports(AnimaniaAnimalEntity animal) {
        String path = registryPath(animal);
        return path.startsWith("kitten_") || path.startsWith("puppy_");
    }

    private static String registryPath(AnimaniaAnimalEntity animal) {
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(animal.getType());
        return id == null ? "" : id.getPath();
    }
}
