package com.animania.farm;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;

/** Durable crafting tool replacing the old non-repairable carving knife. */
public final class FarmCarvingKnifeItem extends SwordItem {
    public FarmCarvingKnifeItem() {
        super(Tiers.IRON, 2, -2.4F, new Item.Properties().durability(100));
    }

    @Override
    public boolean hasCraftingRemainingItem(ItemStack stack) { return true; }

    @Override
    public ItemStack getCraftingRemainingItem(ItemStack stack) {
        ItemStack remaining = stack.copy();
        remaining.setDamageValue(Math.min(remaining.getMaxDamage(), remaining.getDamageValue() + 1));
        return remaining.getDamageValue() >= remaining.getMaxDamage() ? ItemStack.EMPTY : remaining;
    }
}
