package com.animania.catsdogs;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

/** Native equivalent of the 1.12 TileEntityProp shared by all pet facilities. */
public final class CatsDogsPetFacilityBlockEntity extends BlockEntity {
    public CatsDogsPetFacilityBlockEntity(BlockPos pos, BlockState state) {
        super(CatsDogsContent.PET_PROP_BE.get(), pos, state);
    }

    public String facilityType() {
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(getBlockState().getBlock());
        return id == null ? "unknown" : id.getPath();
    }
}
