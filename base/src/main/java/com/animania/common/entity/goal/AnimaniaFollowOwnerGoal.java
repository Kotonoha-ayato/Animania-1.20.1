package com.animania.common.entity.goal;

import com.animania.common.config.AnimaniaConfig;
import com.animania.common.entity.AnimaniaAnimalEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;

import java.util.EnumSet;

/** Server-authoritative replacement for the 1.12 GenericAIFollowOwner goal. */
public final class AnimaniaFollowOwnerGoal extends Goal {
    private final AnimaniaAnimalEntity animal;
    private final double speed;
    private final float minDistance;
    private final float maxDistance;
    private Player owner;

    public AnimaniaFollowOwnerGoal(AnimaniaAnimalEntity animal) {
        this(animal, legacySpeed(animal), legacyMinDistance(animal), legacyMaxDistance(animal));
    }

    public AnimaniaFollowOwnerGoal(AnimaniaAnimalEntity animal, double speed, float minDistance, float maxDistance) {
        this.animal = animal;
        this.speed = speed;
        this.minDistance = minDistance;
        this.maxDistance = maxDistance;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        owner = resolveOwner();
        if (!legacyGateAllows() || owner == null) return false;
        return animal.distanceToSqr(owner) > minDistance * minDistance;
    }

    @Override
    public boolean canContinueToUse() {
        return owner != null && legacyGateAllows() && owner.isAlive()
                && animal.distanceToSqr(owner) > minDistance * minDistance;
    }

    @Override
    public void start() {
        if (owner != null) followOrTeleport();
    }

    @Override
    public void tick() {
        if (owner != null) followOrTeleport();
    }

    @Override
    public void stop() {
        owner = null;
        animal.getNavigation().stop();
    }

    public boolean legacyGateAllows() {
        return animal.isTamed() && !animal.isSleeping() && !animal.isSitting()
                && !animal.isLeashed() && !animal.isPassenger() && !animal.isInWater();
    }

    public Player owner() { return owner; }
    public double speed() { return speed; }
    public float minDistance() { return minDistance; }
    public float maxDistance() { return maxDistance; }

    private Player resolveOwner() {
        return resolveOwner(animal);
    }

    /** Shared server-side UUID lookup for companion combat goals. */
    public static Player resolveOwner(AnimaniaAnimalEntity animal) {
        if (!(animal.level() instanceof ServerLevel server) || animal.getOwnerUUID() == null) return null;
        ServerPlayer listed = server.getServer().getPlayerList().getPlayer(animal.getOwnerUUID());
        if (listed != null) return listed;
        listed = server.players().stream().filter(player -> player.getUUID().equals(animal.getOwnerUUID()))
                .findFirst().orElse(null);
        if (listed != null) return listed;
        // GameTest mock players are inserted into the level without being added to
        // the server's connection/player list.  Scanning the loaded level keeps the
        // same server-authoritative UUID lookup while making the goal deterministic
        // in those tests (and for LAN/FakePlayer integrations).
        return server.getEntitiesOfClass(Player.class, animal.getBoundingBox().inflate(128.0D),
                        player -> player.getUUID().equals(animal.getOwnerUUID()))
                .stream().findFirst().orElse(null);
    }

    private void followOrTeleport() {
        if (owner == null) return;
        double distance = animal.distanceToSqr(owner);
        if (distance >= maxDistance * maxDistance && configured(AnimaniaConfig.TAMED_ANIMALS_TELEPORT, true)
                && tryTeleportNearOwner()) return;
        if (distance > minDistance * minDistance) {
            animal.getNavigation().moveTo(owner, speed);
            animal.getLookControl().setLookAt(owner, animal.getMaxHeadYRot(), animal.getMaxHeadXRot());
        }
    }

    private boolean tryTeleportNearOwner() {
        BlockPos origin = owner.blockPosition();
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (Math.abs(dx) < 2 && Math.abs(dz) < 2) continue;
                for (int dy = 0; dy <= 1; dy++) {
                    BlockPos target = origin.offset(dx, dy, dz);
                    BlockState state = animal.level().getBlockState(target);
                    BlockState below = animal.level().getBlockState(target.below());
                    if (!state.isAir() || !below.isFaceSturdy(animal.level(), target.below(), net.minecraft.core.Direction.UP)) continue;
                    double x = target.getX() + 0.5D;
                    double y = target.getY();
                    double z = target.getZ() + 0.5D;
                    if (!animal.level().noCollision(animal, animal.getBoundingBox().move(x - animal.getX(),
                            y - animal.getY(), z - animal.getZ()))) continue;
                    animal.moveTo(x, y, z, animal.getYRot(), animal.getXRot());
                    animal.getNavigation().stop();
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean supports(AnimaniaAnimalEntity animal) {
        return supports(animal.registryNamespace(), animal.registryPath());
    }

    public static boolean supports(String namespace, String path) {
        if (namespace.equals("animania_catsdogs")) {
            return starts(path, "queen_", "tom_", "kitten_", "female_", "male_", "puppy_");
        }
        return namespace.equals("animania_extra") && (path.startsWith("hamster")
                || path.startsWith("ferret_") || path.startsWith("hedgehog"));
    }

    public static double legacySpeed(AnimaniaAnimalEntity animal) {
        return animal.registryNamespace().equals("animania_catsdogs") ? 1.5D : 1.0D;
    }

    public static float legacyMinDistance(AnimaniaAnimalEntity animal) {
        return animal.registryNamespace().equals("animania_catsdogs") ? 5.0F : 10.0F;
    }

    public static float legacyMaxDistance(AnimaniaAnimalEntity animal) {
        return animal.registryNamespace().equals("animania_catsdogs") ? 30.0F : 2.0F;
    }

    private static boolean starts(String value, String... prefixes) {
        for (String prefix : prefixes) if (value.startsWith(prefix)) return true;
        return false;
    }

    private static boolean configured(net.minecraftforge.common.ForgeConfigSpec.BooleanValue value, boolean fallback) {
        try { return value.get(); } catch (IllegalStateException ignored) { return fallback; }
    }
}
