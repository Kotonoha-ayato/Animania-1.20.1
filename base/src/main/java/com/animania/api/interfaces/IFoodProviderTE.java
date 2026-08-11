package com.animania.api.interfaces;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import javax.annotation.Nullable;
import java.util.Set;

/** Item/fluid food-provider contract using Forge's modern fluid stack type. */
public interface IFoodProviderTE {
    boolean canConsume(@Nullable Set<ItemStack> foodItems, @Nullable FluidStack[] fluids);

    boolean canConsume(@Nullable FluidStack fluid, @Nullable Set<ItemStack> foodItems);

    void consumeSolidOrLiquid(int liquidAmount, int itemAmount);

    void consumeSolid(int amount);

    void consumeLiquid(int amount);
}
