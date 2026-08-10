package com.animania.farm;

import com.animania.common.block.AnimaniaContainerBlock;
import com.animania.common.block.AnimaniaStorageBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;

/** Modern bee-hive interaction replacing the old tile entity/CraftStudio path. */
public final class FarmHiveBlock extends AnimaniaContainerBlock {
    private static final VoxelShape SHAPE = Block.box(2, 1, 2, 14, 15, 14);

    public FarmHiveBlock(BlockBehaviour.Properties properties, boolean wild) {
        super(properties, (pos, state) -> wild
                ? new FarmHiveBlockEntity(FarmContent.WILD_HIVE_BE.get(), pos, state)
                : new FarmHiveBlockEntity(FarmContent.HIVE_BE.get(), pos, state));
    }

    @Override
    public VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof FarmHiveBlockEntity hive)) return InteractionResult.PASS;
        ItemStack held = player.getItemInHand(hand);
        if (held.is(Items.GLASS_BOTTLE) && hive.honeyAmount() >= FluidType.BUCKET_VOLUME) {
            if (level.isClientSide) return InteractionResult.SUCCESS;
            hive.honeyTank().drain(FluidType.BUCKET_VOLUME, IFluidHandler.FluidAction.EXECUTE);
            ItemStack honey = new ItemStack(FarmContent.ITEM_ENTRIES.get("honey_jar").get());
            replaceHeld(player, hand, honey);
            level.playSound(null, pos, SoundEvents.BOTTLE_FILL, net.minecraft.sounds.SoundSource.BLOCKS, 0.8F, 1.0F);
            return InteractionResult.CONSUME;
        }
        if (!held.isEmpty() && isEmptyFluidContainer(held) && hive.honeyAmount() >= FluidType.BUCKET_VOLUME) {
            if (level.isClientSide) return InteractionResult.SUCCESS;
            IFluidHandlerItem handler = FluidUtil.getFluidHandler(held).orElse(null);
            if (handler == null) return InteractionResult.PASS;
            FluidStack honey = hive.honeyTank().drain(FluidType.BUCKET_VOLUME, IFluidHandler.FluidAction.SIMULATE);
            if (honey.getAmount() < FluidType.BUCKET_VOLUME || handler.fill(honey, IFluidHandler.FluidAction.SIMULATE) < honey.getAmount()) return InteractionResult.PASS;
            hive.honeyTank().drain(honey.getAmount(), IFluidHandler.FluidAction.EXECUTE);
            handler.fill(honey, IFluidHandler.FluidAction.EXECUTE);
            replaceHeld(player, hand, handler.getContainer());
            return InteractionResult.CONSUME;
        }
        if (player.isShiftKeyDown() && held.isEmpty()) {
            if (!level.isClientSide) player.displayClientMessage(Component.translatable("message.animania.hive_honey", hive.honeyAmount()), true);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return super.use(state, level, pos, player, hand, hit);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (!state.is(replacement.getBlock()) && level.getBlockEntity(pos) instanceof AnimaniaStorageBlockEntity storage) {
            Containers.dropContents(level, pos, storage);
        }
        super.onRemove(state, level, pos, replacement, moving);
    }

    private static boolean isEmptyFluidContainer(ItemStack stack) {
        return stack.is(Items.BUCKET) || stack.getItem() instanceof net.minecraft.world.item.BucketItem;
    }

    private static void replaceHeld(Player player, InteractionHand hand, ItemStack replacement) {
        if (player.getAbilities().instabuild) return;
        ItemStack held = player.getItemInHand(hand);
        if (held.getCount() > 1) {
            held.shrink(1);
            if (!player.addItem(replacement)) player.drop(replacement, false);
        } else player.setItemInHand(hand, replacement);
    }
}
