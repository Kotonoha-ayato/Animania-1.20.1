package com.animania.extra;

import com.animania.client.render.AnimaniaAnimalRenderer;
import com.animania.extra.client.model.ExtraLegacyModelLayers;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import com.animania.common.entity.AnimaniaAnimalEntity;
import net.minecraft.world.entity.EntityType;

final class AnimaniaExtraClient {
    private AnimaniaExtraClient() { }
    static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        ExtraLegacyModelLayers.LAYERS.forEach((id, layer) -> event.registerLayerDefinition(layer, () -> ExtraLegacyModelLayers.create(id)));
    }
    static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> AnimaniaExtra.ENTITIES.forEach((id, type) -> EntityRenderers.register(
                (EntityType<AnimaniaAnimalEntity>) type.get(),
                context -> new AnimaniaAnimalRenderer(context, ExtraLegacyModelLayers.LAYERS.get(id)))));
    }
}
