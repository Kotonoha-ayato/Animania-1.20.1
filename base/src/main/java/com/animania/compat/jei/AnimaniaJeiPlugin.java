package com.animania.compat.jei;

import com.animania.Animania;
import com.animania.common.AnimaniaItems;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** JEI's optional plugin; vanilla data-pack recipes are indexed automatically. */
@JeiPlugin
@OnlyIn(Dist.CLIENT)
public final class AnimaniaJeiPlugin implements IModPlugin {
    private static final ResourceLocation UID = new ResourceLocation(Animania.MOD_ID, "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addItemStackInfo(AnimaniaItems.MANUAL.get().getDefaultInstance(),
                Component.translatable("jei.animania.manual_info"));
        // Vanilla data-pack recipes are discovered by JEI itself. Add concise
        // descriptions for the legacy/custom processing endpoints (hives,
        // cheese moulds, pet facilities, vehicles and all addon items) so
        // optional JEI users can still discover interactions without a hard
        // dependency on addon classes.
        ForgeRegistries.ITEMS.forEach(item -> {
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
            if (id == null || !(id.getNamespace().equals("animania") || id.getNamespace().startsWith("animania_"))) return;
            registration.addItemStackInfo(new ItemStack(item),
                    Component.translatable("jei.animania.registry_info", Component.literal(id.toString())));
        });
    }
}
