package com.animania.common.block;

import com.animania.common.AnimaniaBlocks;
import com.animania.common.config.AnimaniaConfig;
import com.animania.common.entity.AnimaniaAnimalEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Server-side salt lick durability and consumption cooldown. */
public final class AnimaniaSaltLickBlockEntity extends BlockEntity {
    private int usesLeft;
    private int cooldown;

    public AnimaniaSaltLickBlockEntity(BlockPos pos, BlockState state) {
        super(AnimaniaBlocks.SALT_LICK_BE.get(), pos, state);
        usesLeft = AnimaniaConfig.SALT_LICK_MAX_USES.get();
    }

    public void serverTick() {
        if (cooldown > 0) cooldown--;
    }

    public void use(Entity entity) {
        if (!(entity instanceof AnimaniaAnimalEntity animal) || usesLeft <= 0 || cooldown > 0) return;
        usesLeft--;
        cooldown = Math.max(1, AnimaniaConfig.SALT_LICK_TICK.get() / 20);
        animal.setHunger(100);
        animal.setThirst(100);
        animal.heal(2.0F);
        setChanged();
    }

    public int usesLeft() {
        return usesLeft;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("UsesLeft", usesLeft);
        tag.putInt("Cooldown", cooldown);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        usesLeft = Math.max(0, tag.getInt("UsesLeft"));
        cooldown = Math.max(0, tag.getInt("Cooldown"));
    }
}
