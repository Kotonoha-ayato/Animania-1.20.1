package com.animania.common.helper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

/**
 * Small, registry-safe helpers replacing the 1.12 static utility bag.  Every
 * world mutation is explicit and must be called by the authoritative side.
 */
public final class AnimaniaHelper {
    private AnimaniaHelper() { }

    public static Item item(ResourceLocation id) {
        return id == null ? null : ForgeRegistries.ITEMS.getValue(id);
    }

    public static Block block(ResourceLocation id) {
        return id == null ? null : ForgeRegistries.BLOCKS.getValue(id);
    }

    public static ItemStack itemStack(ResourceLocation id, int count, CompoundTag tag) {
        Item value = item(id);
        if (value == null || count <= 0) return ItemStack.EMPTY;
        ItemStack stack = new ItemStack(value, count);
        if (tag != null && !tag.isEmpty()) stack.setTag(tag.copy());
        return stack;
    }

    public static List<Entity> entitiesInRange(Level level, BlockPos center, double range, Entity excluded) {
        AABB box = new AABB(center).inflate(Math.max(0.0D, range));
        return level.getEntities((Entity) null, box, entity -> entity != excluded);
    }

    public static <T extends Entity> List<T> entitiesInRange(Level level, Class<T> type, Entity origin, double range) {
        return level.getEntitiesOfClass(type, origin.getBoundingBox().inflate(Math.max(0.0D, range)), entity -> entity != origin);
    }

    public static void sendBlockEntityUpdate(BlockEntity blockEntity) {
        if (blockEntity == null || blockEntity.getLevel() == null || blockEntity.getLevel().isClientSide) return;
        BlockPos pos = blockEntity.getBlockPos();
        blockEntity.getLevel().sendBlockUpdated(pos, blockEntity.getBlockState(), blockEntity.getBlockState(), 3);
    }

    public static boolean hasFluid(ItemStack stack, Fluid fluid, int minimumAmount) {
        if (stack == null || stack.isEmpty() || fluid == null) return false;
        return FluidUtil.getFluidHandler(stack).map(handler -> {
            var content = handler.getFluidInTank(0);
            return content.getFluid().isSame(fluid) && content.getAmount() >= minimumAmount;
        }).orElse(false);
    }

    public static ItemStack emptyContainer(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return ItemStack.EMPTY;
        return FluidUtil.tryEmptyContainer(stack, null, Integer.MAX_VALUE, null, true).result;
    }

    public static CompoundTag parseTag(String raw) {
        if (raw == null || raw.isBlank()) return new CompoundTag();
        try {
            return TagParser.parseTag(raw);
        } catch (Exception ignored) {
            return new CompoundTag();
        }
    }
}
