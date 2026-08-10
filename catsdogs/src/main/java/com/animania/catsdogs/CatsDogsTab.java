package com.animania.catsdogs;

import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class CatsDogsTab {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, AnimaniaCatsDogs.MOD_ID);
    public static final RegistryObject<CreativeModeTab> MAIN = TABS.register("catsdogs", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.animania_catsdogs"))
            .icon(() -> new ItemStack(CatsDogsContent.ITEM_ENTRIES.get("entity_egg_cat_random").get()))
            .displayItems((parameters, output) -> CatsDogsContent.ITEM_ENTRIES.values().forEach(item -> output.accept(item.get())))
            .build());

    private CatsDogsTab() { }
}
