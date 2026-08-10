package com.animania.extra;

import com.animania.client.render.AnimaniaAnimalRenderer;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import com.animania.common.entity.AnimaniaAnimalEntity;
import net.minecraft.world.entity.EntityType;

final class AnimaniaExtraClient {
    private AnimaniaExtraClient() { }
    static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> AnimaniaExtra.ENTITIES.values().forEach(type -> EntityRenderers.register((EntityType<AnimaniaAnimalEntity>) type.get(), AnimaniaAnimalRenderer::new)));
    }
}
