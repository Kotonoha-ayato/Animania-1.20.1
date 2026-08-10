package com.animania;

import com.animania.common.AnimaniaBlocks;
import com.animania.common.AnimaniaItems;
import com.animania.common.AnimaniaFluids;
import com.animania.common.AnimaniaTabs;
import com.animania.common.config.AnimaniaConfig;
import com.animania.common.advancement.FeedAnimalTrigger;
import com.animania.network.AnimaniaNetwork;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.event.RegisterGameTestsEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.data.event.GatherDataEvent;

/** Entry point for the shared Animania API and content. */
@Mod(Animania.MOD_ID)
public final class Animania {
    public static final String MOD_ID = "animania";
    public static final String VERSION = "3.0.0";

    public Animania() {
        FeedAnimalTrigger.bootstrap();
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        AnimaniaItems.ITEMS.register(modBus);
        AnimaniaFluids.FLUID_TYPES.register(modBus);
        AnimaniaFluids.FLUIDS.register(modBus);
        AnimaniaFluids.BLOCKS.register(modBus);
        AnimaniaBlocks.BLOCKS.register(modBus);
        AnimaniaBlocks.ITEMS.register(modBus);
        AnimaniaBlocks.BLOCK_ENTITIES.register(modBus);
        AnimaniaTabs.TABS.register(modBus);
        AnimaniaNetwork.register();
        modBus.addListener(this::registerGameTests);
        // Do not resolve TOP's optional classes unless TOP is actually loaded;
        // datagen and dedicated-server classpaths intentionally omit it.
        if (ModList.get().isLoaded("theoneprobe")) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.animania.compat.top.AnimaniaTopProbeCompat.bootstrap());
        }
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, AnimaniaConfig.COMMON_SPEC);
        MinecraftForge.EVENT_BUS.register(new AnimaniaServerEvents());
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> modBus.addListener(com.animania.client.AnimaniaClient::registerLayers));
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> modBus.addListener(com.animania.client.AnimaniaClient::clientSetup));
        modBus.addListener(this::gatherData);
    }

    private void gatherData(GatherDataEvent event) {
        event.getGenerator().addProvider(event.includeServer(), new com.animania.data.AnimaniaDataProvider(event.getGenerator().getPackOutput()));
    }

    private void registerGameTests(RegisterGameTestsEvent event) {
        event.register(com.animania.gametest.AnimaniaBaseGameTests.class);
    }
}
