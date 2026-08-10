package com.animania.farm;

import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class FarmTab {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, AnimaniaFarm.MOD_ID);
    public static final RegistryObject<CreativeModeTab> MAIN = TABS.register("farm", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.animania_farm"))
            .icon(() -> new ItemStack(FarmContent.ITEM_ENTRIES.get("truffle").get()))
            .displayItems((parameters, output) -> {
                FarmContent.ITEM_ENTRIES.values().forEach(item -> output.accept(item.get()));
                FarmFluids.ALL.values().forEach(fluid -> output.accept(fluid.bucket.get()));
            })
            .build());

    private FarmTab() { }
}
