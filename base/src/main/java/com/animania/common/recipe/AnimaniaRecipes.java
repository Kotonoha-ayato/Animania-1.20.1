package com.animania.common.recipe;

import com.animania.Animania;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Animania-owned recipe semantics that cannot be represented by static vanilla ingredients. */
public final class AnimaniaRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, Animania.MOD_ID);
    public static final RegistryObject<RecipeSerializer<SlopRecipe>> SLOP = SERIALIZERS.register(
            "slop", () -> new SimpleCraftingRecipeSerializer<>(SlopRecipe::new));

    private AnimaniaRecipes() { }
}
