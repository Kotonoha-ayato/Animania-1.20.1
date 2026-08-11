package com.animania.common.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import com.animania.common.config.AnimaniaConfig;
import com.animania.common.item.AnimaniaSaltLickItem;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import java.util.List;

/** Low-profile block that animals can autonomously consume for care. */
public final class AnimaniaSaltLickBlock extends BaseEntityBlock {
    public AnimaniaSaltLickBlock(Properties properties) {
        super(properties.noOcclusion());
    }

    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.INVISIBLE; }

    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        double ratio = 0.4D;
        if (level.getBlockEntity(pos) instanceof AnimaniaSaltLickBlockEntity lick)
            ratio = Math.max(0.05D, (double) lick.usesLeft() / Math.max(1, AnimaniaConfig.SALT_LICK_MAX_USES.get()));
        return Block.box(3, 0, 3, 13, 10 * ratio, 13);
    }

    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AnimaniaSaltLickBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return (tickLevel, pos, tickState, blockEntity) -> {
            if (blockEntity instanceof AnimaniaSaltLickBlockEntity lick) lick.serverTick();
        };
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        super.entityInside(state, level, pos, entity);
    }

    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        if (level.getBlockEntity(pos) instanceof AnimaniaSaltLickBlockEntity lick)
            lick.setUsesLeft(AnimaniaSaltLickItem.remainingUses(stack));
    }

    public ItemStack stackForRemainingUses(int uses) {
        ItemStack stack = new ItemStack(asItem());
        stack.setDamageValue(AnimaniaSaltLickItem.damageForRemainingUses(uses, AnimaniaSaltLickItem.configuredMaxUses()));
        return stack;
    }

    @Override public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        BlockEntity blockEntity = builder.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_ENTITY);
        return List.of(stackForRemainingUses(blockEntity instanceof AnimaniaSaltLickBlockEntity lick
                ? lick.usesLeft() : AnimaniaSaltLickItem.configuredMaxUses()));
    }
}
