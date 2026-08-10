package com.animania.extra;

import com.animania.common.block.AnimaniaContainerBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** Functional hamster wheel block replacing the old CraftStudio tile entity. */
public final class ExtraHamsterWheelBlock extends AnimaniaContainerBlock {
    public ExtraHamsterWheelBlock(BlockBehaviour.Properties properties) {
        super(properties, ExtraHamsterWheelBlockEntity::new);
    }
}
