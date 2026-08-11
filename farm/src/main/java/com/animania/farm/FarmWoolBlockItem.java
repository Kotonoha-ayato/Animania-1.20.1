package com.animania.farm;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/** A single stable registry ID with explicit NBT-backed modern variants. */
public final class FarmWoolBlockItem extends BlockItem {
    private static final String BLOCK_STATE_TAG = "BlockStateTag";

    public FarmWoolBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    public static ItemStack stack(FarmWoolBlock.Variant variant) {
        ItemStack stack = new ItemStack(FarmContent.ITEM_ENTRIES.get("animania_wool").get());
        CompoundTag state = new CompoundTag();
        state.putString("variant", variant.getSerializedName());
        stack.getOrCreateTag().put(BLOCK_STATE_TAG, state);
        return stack;
    }

    public static FarmWoolBlock.Variant variant(ItemStack stack) {
        CompoundTag state = stack.getTagElement(BLOCK_STATE_TAG);
        String value = state == null ? "" : state.getString("variant");
        for (FarmWoolBlock.Variant variant : FarmWoolBlock.Variant.values()) {
            if (variant.getSerializedName().equals(value)) return variant;
        }
        return FarmWoolBlock.Variant.DORSET_BROWN;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("item.animania_farm.wool_" + variant(stack).getSerializedName());
    }
}
