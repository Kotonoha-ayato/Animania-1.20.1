package com.animania.farm;

import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/** Server-authoritative brown egg projectile with the migrated hatch chance. */
public final class FarmBrownEggProjectile extends ThrowableItemProjectile {
    public FarmBrownEggProjectile(EntityType<FarmBrownEggProjectile> type, Level level) {
        super(type, level);
    }

    public FarmBrownEggProjectile(Level level, LivingEntity owner) {
        super(FarmContent.BROWN_EGG_PROJECTILE.get(), owner, level);
    }

    @Override
    protected void onHit(HitResult result) {
        // Projectile's collision/damage path is retained; only the vanilla
        // hard-coded 1/8 hatch roll is replaced with the Forge config value.
        super.onHit(result);
        if (level().isClientSide) return;
        int chance;
        try {
            chance = Math.max(1, com.animania.common.config.AnimaniaConfig.EGG_HATCH_CHANCE.get());
        } catch (RuntimeException ignored) {
            chance = 2;
        }
        if (random.nextInt(chance) == 0) {
            int count = random.nextInt(32) == 0 ? 4 : 1;
            for (int i = 0; i < count; i++) {
                Chicken chicken = EntityType.CHICKEN.create(level());
                if (chicken == null) continue;
                chicken.setAge(-24000);
                chicken.moveTo(getX(), getY(), getZ(), getYRot(), 0.0F);
                level().addFreshEntity(chicken);
            }
        }
        level().broadcastEntityEvent(this, (byte) 3);
        discard();
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 3) {
            for (int i = 0; i < 8; i++) {
                level().addParticle(new ItemParticleOption(ParticleTypes.ITEM, getItem()),
                        getX(), getY(), getZ(), (random.nextFloat() - 0.5D) * 0.08D,
                        (random.nextFloat() - 0.5D) * 0.08D, (random.nextFloat() - 0.5D) * 0.08D);
            }
            return;
        }
        super.handleEntityEvent(id);
    }

    @Override
    protected Item getDefaultItem() {
        if (FarmContent.ITEM_ENTRIES.containsKey("brown_egg")) return FarmContent.ITEM_ENTRIES.get("brown_egg").get();
        return Items.EGG;
    }
}
