package com.animania.extra;

import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ExtraTab {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, AnimaniaExtra.MOD_ID);
    public static final RegistryObject<CreativeModeTab> MAIN = TABS.register("extra", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.animania_extra"))
            .icon(() -> new ItemStack(ExtraContent.ITEM_ENTRIES.get("hamster_food").get()))
            .displayItems((parameters, output) -> ExtraContent.ITEM_ENTRIES.values().forEach(item -> output.accept(item.get())))
            .build());

    private ExtraTab() { }
}
