package com.animania.common.block;

import net.minecraft.world.level.block.Block;

/** Collision-free helper used by the legacy trough/invisiblock layout. */
public final class AnimaniaInvisibleBlock extends Block {
    public AnimaniaInvisibleBlock(Properties properties) {
        super(properties.noOcclusion().noCollission().noLootTable());
    }
}
