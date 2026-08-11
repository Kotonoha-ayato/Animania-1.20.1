package com.animania.common;

import com.animania.common.block.AnimaniaThinBlock;
import com.animania.common.config.AnimaniaConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Shared server-authoritative placement path for hand and dispenser seed piles. */
public final class AnimaniaSeedPlacement {
    private AnimaniaSeedPlacement() { }

    public static AnimaniaThinBlock.SeedVariant variant(Item item) {
        if (item == Items.PUMPKIN_SEEDS) return AnimaniaThinBlock.SeedVariant.PUMPKIN;
        if (item == Items.MELON_SEEDS) return AnimaniaThinBlock.SeedVariant.MELON;
        if (item == Items.BEETROOT_SEEDS) return AnimaniaThinBlock.SeedVariant.BEETROOT;
        return item == Items.WHEAT_SEEDS ? AnimaniaThinBlock.SeedVariant.WHEAT : null;
    }

    public static boolean place(Level level, BlockPos pos, ItemStack stack) {
        AnimaniaThinBlock.SeedVariant variant = variant(stack.getItem());
        if (variant == null || stack.isEmpty() || !canPlace(level, pos)) return false;
        if (level.isClientSide) return true;
        return level.setBlock(pos, AnimaniaBlocks.SEEDS.get().defaultBlockState().setValue(AnimaniaThinBlock.VARIANT, variant), 3);
    }

    public static boolean canPlace(Level level, BlockPos pos) {
        if (!level.getBlockState(pos).canBeReplaced()) return false;
        BlockPos below = pos.below();
        BlockState support = level.getBlockState(below);
        return !(support.getBlock() instanceof FarmBlock) && support.isFaceSturdy(level, below, Direction.UP);
    }

    public static void registerDispenserBehaviors() {
        DefaultDispenseItemBehavior behavior = new DefaultDispenseItemBehavior() {
            @Override protected ItemStack execute(BlockSource source, ItemStack stack) {
                boolean allowed;
                try { allowed = AnimaniaConfig.ALLOW_SEED_DISPENSER_PLACEMENT.get(); }
                catch (IllegalStateException ignored) { allowed = true; }
                BlockPos target = source.getPos().relative(source.getBlockState().getValue(DispenserBlock.FACING));
                if (allowed && place(source.getLevel(), target, stack)) {
                    stack.shrink(1);
                    source.getLevel().playSound(null, target, SoundEvents.GRASS_PLACE, SoundSource.BLOCKS, 0.5F, 1.0F);
                    return stack;
                }
                return super.execute(source, stack);
            }
        };
        DispenserBlock.registerBehavior(Items.WHEAT_SEEDS, behavior);
        DispenserBlock.registerBehavior(Items.PUMPKIN_SEEDS, behavior);
        DispenserBlock.registerBehavior(Items.MELON_SEEDS, behavior);
        DispenserBlock.registerBehavior(Items.BEETROOT_SEEDS, behavior);
    }
}
