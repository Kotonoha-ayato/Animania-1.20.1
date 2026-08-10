package com.animania.catsdogs;

import com.animania.client.render.AnimaniaAnimalRenderer;
import com.animania.catsdogs.client.model.CatsDogsLegacyModelLayers;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import com.animania.common.entity.AnimaniaAnimalEntity;
import net.minecraft.world.entity.EntityType;

final class AnimaniaCatsDogsClient {
    private AnimaniaCatsDogsClient() { }
    static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        CatsDogsLegacyModelLayers.LAYERS.forEach((id, layer) -> event.registerLayerDefinition(layer, () -> CatsDogsLegacyModelLayers.create(id)));
    }
    static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> AnimaniaCatsDogs.ENTITIES.forEach((id, type) -> EntityRenderers.register(
                (EntityType<AnimaniaAnimalEntity>) type.get(),
                context -> new AnimaniaAnimalRenderer(context, CatsDogsLegacyModelLayers.LAYERS.get(id)))));
    }
}
