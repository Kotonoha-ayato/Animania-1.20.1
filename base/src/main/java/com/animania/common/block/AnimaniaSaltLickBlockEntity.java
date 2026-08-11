package com.animania.common.block;

import com.animania.common.AnimaniaBlocks;
import com.animania.common.config.AnimaniaConfig;
import com.animania.common.entity.AnimaniaAnimalEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;

/** Server-side salt lick durability and consumption cooldown. */
public final class AnimaniaSaltLickBlockEntity extends BlockEntity {
    private int usesLeft;

    public AnimaniaSaltLickBlockEntity(BlockPos pos, BlockState state) {
        super(AnimaniaBlocks.SALT_LICK_BE.get(), pos, state);
        usesLeft = AnimaniaConfig.SALT_LICK_MAX_USES.get();
    }

    public void serverTick() { }

    public boolean use(Entity entity) {
        if (!(entity instanceof AnimaniaAnimalEntity animal) || usesLeft <= 0 || level == null || level.isClientSide) return false;
        usesLeft--;
        animal.heal(2.0F);
        if (usesLeft <= 0) level.removeBlock(worldPosition, false);
        else setChanged();
        return true;
    }

    public int usesLeft() {
        return usesLeft;
    }

    public void setUsesLeft(int usesLeft) {
        this.usesLeft = Math.max(0, Math.min(AnimaniaConfig.SALT_LICK_MAX_USES.get(), usesLeft));
        setChanged();
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet) {
        CompoundTag tag = packet.getTag();
        if (tag != null) load(tag);
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("UsesLeft", usesLeft);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        usesLeft = tag.contains("UsesLeft") ? Math.max(0, tag.getInt("UsesLeft")) : AnimaniaConfig.SALT_LICK_MAX_USES.get();
    }
}
