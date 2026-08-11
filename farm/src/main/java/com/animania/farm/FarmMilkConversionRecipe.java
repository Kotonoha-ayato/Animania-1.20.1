package com.animania.farm;

import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

/** Converts exactly one of the five Animania milk buckets to vanilla milk. */
public final class FarmMilkConversionRecipe extends CustomRecipe {
    public FarmMilkConversionRecipe(ResourceLocation id, CraftingBookCategory category) {
        super(id, category);
    }

    @Override
    public boolean matches(CraftingContainer container, Level level) {
        int milk = 0;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (stack.isEmpty()) continue;
            if (!isAnimaniaMilkBucket(stack)) return false;
            milk++;
        }
        return milk == 1;
    }

    public static boolean isAnimaniaMilkBucket(ItemStack stack) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return id != null && AnimaniaFarm.MOD_ID.equals(id.getNamespace())
                && id.getPath().startsWith("milk_") && id.getPath().endsWith("_bucket");
    }

    @Override public ItemStack assemble(CraftingContainer container, RegistryAccess registries) {
        return new ItemStack(Items.MILK_BUCKET);
    }
    @Override public boolean canCraftInDimensions(int width, int height) { return width * height >= 1; }
    @Override public RecipeSerializer<?> getSerializer() { return FarmRecipes.MILK_CONVERSION.get(); }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingContainer container) {
        return NonNullList.withSize(container.getContainerSize(), ItemStack.EMPTY);
    }
}
