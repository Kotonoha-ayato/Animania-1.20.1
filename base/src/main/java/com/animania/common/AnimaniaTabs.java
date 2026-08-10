package com.animania.common;

import com.animania.Animania;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/** Shared creative tab; addons expose their own tabs so each JAR is useful alone. */
public final class AnimaniaTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Animania.MOD_ID);
    public static final RegistryObject<CreativeModeTab> MAIN = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.animania"))
            .icon(() -> new ItemStack(AnimaniaItems.MANUAL.get()))
            .displayItems((parameters, output) -> {
                output.accept(AnimaniaItems.MANUAL.get());
                output.accept(AnimaniaItems.ENTITY_EGG_RANDOM.get());
                output.accept(AnimaniaItems.HAY.get());
                output.accept(AnimaniaItems.SALT.get());
                output.accept(AnimaniaItems.CHEESE.get());
                output.accept(AnimaniaItems.WATER_BOTTLE.get());
                output.accept(AnimaniaBlocks.TROUGH.get());
                output.accept(AnimaniaBlocks.NEST.get());
                output.accept(AnimaniaBlocks.CHEESE_MOLD.get());
                output.accept(AnimaniaBlocks.PET_BOWL.get());
                output.accept(AnimaniaBlocks.SALT_LICK.get());
                output.accept(AnimaniaBlocks.MUD.get());
                output.accept(AnimaniaBlocks.STRAW.get());
                output.accept(AnimaniaBlocks.HAMSTER_WHEEL.get());
            }).build());

    private AnimaniaTabs() { }
}
