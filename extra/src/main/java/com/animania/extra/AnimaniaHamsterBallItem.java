package com.animania.extra;

import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Modern replacement for the 1.12 metadata hamster-ball items.
 *
 * <p>The clear ball uses the sentinel colour {@value #CLEAR_COLOR}; the
 * coloured ball stores a vanilla dye id in the stack tag.  Keeping the colour
 * in NBT means the item remains stable across saves and does not rely on the
 * removed damage-value/subtype API.</p>
 */
public final class AnimaniaHamsterBallItem extends Item {
    public static final String COLOR_TAG = "BallColor";
    public static final int CLEAR_COLOR = 16;
    private final boolean colored;

    public AnimaniaHamsterBallItem(boolean colored) {
        super(new Item.Properties().stacksTo(1));
        this.colored = colored;
    }

    public boolean isColored() {
        return colored;
    }

    public int color(ItemStack stack) {
        if (!colored) return CLEAR_COLOR;
        return colorOf(stack);
    }

    @Override
    public String getDescriptionId(ItemStack stack) {
        if (!colored) return "item.animania.hamster_ball_clear";
        String colorName = DyeColor.byId(color(stack)).getName();
        // Animania 1.12 used "silver" for vanilla's modern light_gray dye.
        if ("light_gray".equals(colorName)) colorName = "silver";
        return "item.animania.hamster_ball_" + colorName;
    }

    public static int colorOf(ItemStack stack) {
        if (stack.isEmpty() || !stack.hasTag() || !stack.getTag().contains(COLOR_TAG)) return 0;
        return clamp(stack.getTag().getInt(COLOR_TAG));
    }

    public static ItemStack stackForColor(Item item, int color) {
        ItemStack stack = new ItemStack(item);
        if (color != CLEAR_COLOR) stack.getOrCreateTag().putInt(COLOR_TAG, clamp(color));
        return stack;
    }

    /** Colour handler value for the translucent wire texture. */
    public int tintColor(ItemStack stack, int tintIndex) {
        if (tintIndex != 0) return 0xFFFFFFFF;
        if (!colored) return 0xFFFFFFFF;
        return 0xFF000000 | DyeColor.byId(color(stack)).getFireworkColor();
    }

    private static int clamp(int color) {
        return Math.max(0, Math.min(15, color));
    }
}
