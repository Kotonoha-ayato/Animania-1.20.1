package com.animania.common.item;

import com.animania.common.config.AnimaniaConfig;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;

/** Damage bar mirrors the number of salt-lick uses already consumed. */
public final class AnimaniaSaltLickItem extends BlockItem {
    /** Matches the upper bound accepted by the Forge config spec. */
    public static final int MAX_SUPPORTED_USES = 100_000;

    public AnimaniaSaltLickItem(Block block, Properties properties) {
        super(block, properties.durability(MAX_SUPPORTED_USES));
    }

    public static int configuredMaxUses() {
        try { return AnimaniaConfig.SALT_LICK_MAX_USES.get(); }
        catch (IllegalStateException ignored) { return AnimaniaConfig.SALT_LICK_MAX_USES.getDefault(); }
    }

    public static int remainingUses(ItemStack stack) {
        return remainingUses(stack.getDamageValue(), configuredMaxUses());
    }

    public static int remainingUses(int damage, int maximum) {
        int safeMaximum = Math.max(1, maximum);
        return safeMaximum - Math.max(0, Math.min(safeMaximum, damage));
    }

    public static int damageForRemainingUses(int uses, int maximum) {
        int safeMaximum = Math.max(1, maximum);
        return safeMaximum - Math.max(0, Math.min(safeMaximum, uses));
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return stack.getDamageValue() > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        float remaining = remainingUses(stack) / (float) Math.max(1, configuredMaxUses());
        return Math.round(13.0F * Mth.clamp(remaining, 0.0F, 1.0F));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        float remaining = remainingUses(stack) / (float) Math.max(1, configuredMaxUses());
        return Mth.hsvToRgb(Mth.clamp(remaining, 0.0F, 1.0F) / 3.0F, 1.0F, 1.0F);
    }
}
