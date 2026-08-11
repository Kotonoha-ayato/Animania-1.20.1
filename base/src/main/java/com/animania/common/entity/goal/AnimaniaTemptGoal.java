package com.animania.common.entity.goal;

import com.animania.common.entity.AnimaniaAnimalEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;

import java.util.Comparator;
import java.util.EnumSet;

/**
 * Temptation goal backed by the live addon food matcher instead of a fixed
 * vanilla Ingredient. This preserves per-species Forge config reloads and
 * prevents unrelated animals from following the same four farm foods.
 */
public final class AnimaniaTemptGoal extends Goal {
    private final AnimaniaAnimalEntity animal;
    private final double speedModifier;
    private final boolean scaredByPlayerMovement;
    private Player temptingPlayer;
    private double playerX;
    private double playerY;
    private double playerZ;
    private double playerXRot;
    private double playerYRot;
    private int calmDown;

    public AnimaniaTemptGoal(AnimaniaAnimalEntity animal, double speedModifier) {
        this(animal, speedModifier, false);
    }

    public AnimaniaTemptGoal(AnimaniaAnimalEntity animal, double speedModifier, boolean scaredByPlayerMovement) {
        this.animal = animal;
        this.speedModifier = speedModifier;
        this.scaredByPlayerMovement = scaredByPlayerMovement;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (calmDown > 0) {
            calmDown--;
            return false;
        }
        if (animal.isSitting() || animal.isSleeping()) return false;
        temptingPlayer = animal.level().getEntitiesOfClass(Player.class,
                        animal.getBoundingBox().inflate(10.0D), this::holdsAcceptedFood)
                .stream()
                .min(Comparator.comparingDouble(animal::distanceToSqr))
                .orElse(null);
        return temptingPlayer != null;
    }

    @Override
    public boolean canContinueToUse() {
        if (temptingPlayer == null || !temptingPlayer.isAlive()) return false;
        if (scaredByPlayerMovement && animal.distanceToSqr(temptingPlayer) < 36.0D) {
            if (temptingPlayer.distanceToSqr(playerX, playerY, playerZ) > 0.01D
                    || Math.abs(temptingPlayer.getXRot() - playerXRot) > 5.0D
                    || Math.abs(temptingPlayer.getYRot() - playerYRot) > 5.0D) return false;
            playerX = temptingPlayer.getX();
            playerY = temptingPlayer.getY();
            playerZ = temptingPlayer.getZ();
            playerXRot = temptingPlayer.getXRot();
            playerYRot = temptingPlayer.getYRot();
        }
        return temptingPlayer.isAlive()
                && animal.distanceToSqr(temptingPlayer) < 144.0D
                && holdsAcceptedFood(temptingPlayer)
                && !animal.isSitting() && !animal.isSleeping();
    }

    @Override
    public void stop() {
        temptingPlayer = null;
        animal.getNavigation().stop();
        calmDown = 100;
    }

    @Override
    public void start() {
        if (temptingPlayer == null) return;
        playerX = temptingPlayer.getX();
        playerY = temptingPlayer.getY();
        playerZ = temptingPlayer.getZ();
        playerXRot = temptingPlayer.getXRot();
        playerYRot = temptingPlayer.getYRot();
    }

    @Override
    public void tick() {
        if (temptingPlayer == null) return;
        if (!animal.hasInteracted()) animal.markInteracted();
        animal.getLookControl().setLookAt(temptingPlayer, animal.getMaxHeadYRot() + 20.0F,
                animal.getMaxHeadXRot());
        if (animal.distanceToSqr(temptingPlayer) < 6.25D) {
            animal.getNavigation().stop();
        } else {
            animal.getNavigation().moveTo(temptingPlayer, speedModifier);
        }
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    private boolean holdsAcceptedFood(Player player) {
        return isLegacyTemptItem(player.getMainHandItem()) || isLegacyTemptItem(player.getOffhandItem());
    }

    private boolean isLegacyTemptItem(net.minecraft.world.item.ItemStack stack) {
        if (animal.isFood(stack)) return true;
        String namespace = animal.registryNamespace();
        String path = animal.registryPath();
        if (namespace.equals("animania_farm") && (path.startsWith("cow_") || path.startsWith("bull_")
                || path.startsWith("calf_") || path.startsWith("ewe_") || path.startsWith("ram_")
                || path.startsWith("lamb_")) && stack.is(ItemTags.FLOWERS)) return true;
        return namespace.equals("animania_farm") && (path.startsWith("sow_") || path.startsWith("hog_")
                || path.startsWith("piglet_")) && stack.is(Items.CARROT_ON_A_STICK);
    }

    public Player temptingPlayer() {
        return temptingPlayer;
    }

    public int calmDownTicks() {
        return calmDown;
    }

    public static double legacySpeed(AnimaniaAnimalEntity animal) {
        String namespace = animal.registryNamespace();
        String path = animal.registryPath();
        if (namespace.equals("animania_catsdogs") && (path.startsWith("queen_") || path.startsWith("tom_")
                || path.startsWith("kitten_"))) return 0.6D;
        if (namespace.equals("animania_farm") && (path.startsWith("cow_") || path.startsWith("bull_")
                || path.startsWith("calf_") || path.startsWith("doe_") || path.startsWith("buck_")
                || path.startsWith("kid_") || path.startsWith("ewe_") || path.startsWith("ram_")
                || path.startsWith("lamb_") || path.startsWith("mare_") || path.startsWith("stallion_")
                || path.startsWith("foal_"))) return 1.25D;
        if (namespace.equals("animania_extra") && (path.startsWith("doe_") || path.startsWith("buck_")
                || path.startsWith("kit_"))) return 1.25D;
        return 1.2D;
    }

    public static boolean legacyScaredByMovement(AnimaniaAnimalEntity animal) {
        String path = animal.registryPath();
        return animal.registryNamespace().equals("animania_catsdogs")
                && (path.startsWith("queen_") || path.startsWith("tom_") || path.startsWith("kitten_"));
    }
}
