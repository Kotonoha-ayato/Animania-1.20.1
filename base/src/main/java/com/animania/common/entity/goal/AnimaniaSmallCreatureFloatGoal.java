package com.animania.common.entity.goal;

import com.animania.common.entity.AnimaniaAnimalEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Legacy small-creature swimming: vanilla floating plus an early jump when
 * the movement vector is about to carry the animal into Animania mud.
 */
public final class AnimaniaSmallCreatureFloatGoal extends FloatGoal {
    private final AnimaniaAnimalEntity animal;

    public AnimaniaSmallCreatureFloatGoal(AnimaniaAnimalEntity animal) {
        super(animal);
        this.animal = animal;
    }

    @Override
    public boolean canUse() {
        return isMudAhead() || super.canUse();
    }

    public boolean isMudAhead() {
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(animal.level().getBlockState(predictedPosition()).getBlock());
        return id != null && id.getPath().equals("mud");
    }

    public BlockPos predictedPosition() {
        return BlockPos.containing(animal.getX() + animal.getDeltaMovement().x / 1.5D,
                animal.getY() + 0.1D, animal.getZ() + animal.getDeltaMovement().z / 1.5D);
    }

    public static boolean supports(AnimaniaAnimalEntity animal) {
        return supports(animal.registryNamespace(), animal.registryPath());
    }

    public static boolean supports(String namespace, String path) {
        if (namespace.equals("animania_farm")) {
            return path.startsWith("hen_") || path.startsWith("rooster_") || path.startsWith("chick_");
        }
        return namespace.equals("animania_extra") && (path.startsWith("hamster")
                || path.startsWith("hedgehog") || path.startsWith("ferret_"));
    }

    public static int legacyPriority(AnimaniaAnimalEntity animal) {
        String path = animal.registryPath();
        if (animal.registryNamespace().equals("animania_extra")) {
            if (path.startsWith("hamster")) return 2;
            if (path.startsWith("hedgehog")) return 1;
        }
        return 0;
    }
}
