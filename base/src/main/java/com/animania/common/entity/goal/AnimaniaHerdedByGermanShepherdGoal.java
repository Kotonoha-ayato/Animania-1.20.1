package com.animania.common.entity.goal;

import com.animania.common.entity.AnimaniaAnimalEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.EnumSet;

/** Restores the optional Cats&Dogs shepherd injection for farm sheep/goats. */
public final class AnimaniaHerdedByGermanShepherdGoal extends Goal {
    private final AnimaniaAnimalEntity herdAnimal;
    private AnimaniaAnimalEntity shepherd;

    public AnimaniaHerdedByGermanShepherdGoal(AnimaniaAnimalEntity herdAnimal) {
        this.herdAnimal = herdAnimal;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    public static boolean supports(AnimaniaAnimalEntity animal) {
        if (!"animania_farm".equals(animal.registryNamespace())) return false;
        String path = animal.registryPath();
        return path.startsWith("ewe_") || path.startsWith("ram_") || path.startsWith("lamb_")
                || path.startsWith("doe_") || path.startsWith("buck_") || path.startsWith("kid_");
    }

    @Override
    public boolean canUse() {
        shepherd = findShepherd();
        return shepherd != null && !herdAnimal.isSleeping();
    }

    @Override
    public boolean canContinueToUse() {
        return shepherd != null && shepherd.isAlive() && !shepherd.isSleeping()
                && !herdAnimal.isSleeping() && herdAnimal.distanceToSqr(shepherd) <= 14.0D * 14.0D;
    }

    @Override public void start() { followShepherd(); }
    @Override public void tick() { shepherd = findShepherd(); followShepherd(); }
    @Override public void stop() { shepherd = null; herdAnimal.getNavigation().stop(); }

    public AnimaniaAnimalEntity shepherd() { return shepherd; }

    private AnimaniaAnimalEntity findShepherd() {
        return herdAnimal.level().getEntitiesOfClass(AnimaniaAnimalEntity.class,
                herdAnimal.getBoundingBox().inflate(10.0D), candidate -> {
                    var id = ForgeRegistries.ENTITY_TYPES.getKey(candidate.getType());
                    return id != null && "animania_catsdogs".equals(id.getNamespace())
                            && ("male_german_shepherd".equals(id.getPath()) || "female_german_shepherd".equals(id.getPath()))
                            && candidate.isTamed() && !candidate.isSitting() && !candidate.isSleeping();
                }).stream().findFirst().orElse(null);
    }

    private void followShepherd() {
        if (shepherd == null) return;
        Path path = shepherd.getNavigation().getPath();
        if (path == null) return;
        Node destination = path.getEndNode();
        if (destination != null) herdAnimal.getNavigation().moveTo(destination.x, destination.y, destination.z, 1.0D);
    }
}
