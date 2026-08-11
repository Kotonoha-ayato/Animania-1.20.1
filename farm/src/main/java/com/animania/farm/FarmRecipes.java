package com.animania.farm;

import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Farm-owned dynamic recipes retained from the 1.12 addon. */
public final class FarmRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, AnimaniaFarm.MOD_ID);
    public static final RegistryObject<RecipeSerializer<FarmMilkConversionRecipe>> MILK_CONVERSION =
            SERIALIZERS.register("milk_conversion",
                    () -> new SimpleCraftingRecipeSerializer<>(FarmMilkConversionRecipe::new));

    private FarmRecipes() { }
}
