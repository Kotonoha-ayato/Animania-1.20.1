package com.animania.common.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Legacy mud collision behaviour: movement is heavily damped on contact. */
public final class AnimaniaMudBlock extends Block {
    public AnimaniaMudBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        entity.setDeltaMovement(entity.getDeltaMovement().multiply(0.2D, 1.0D, 0.2D));
        super.stepOn(level, pos, state, entity);
    }
}
