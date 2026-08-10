package com.animania.farm;

import com.animania.client.render.AnimaniaAnimalRenderer;
import com.animania.client.render.AnimaniaVehicleRenderer;
import com.animania.farm.client.model.FarmLegacyModelLayers;
import com.animania.common.entity.AnimaniaVehicleEntity;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import com.animania.common.entity.AnimaniaAnimalEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraftforge.client.event.EntityRenderersEvent;

final class AnimaniaFarmClient {
    private AnimaniaFarmClient() {
    }

    static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        FarmLegacyModelLayers.LAYERS.forEach((id, layer) -> event.registerLayerDefinition(layer, () -> FarmLegacyModelLayers.create(id)));
    }

    static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> AnimaniaFarm.ENTITIES.forEach((id, type) -> {
            if (FarmLegacyIds.isVehicle(id)) {
                EntityRenderers.register((EntityType<AnimaniaVehicleEntity>) (EntityType<?>) type.get(), AnimaniaVehicleRenderer::new);
            } else {
                EntityRenderers.register((EntityType<AnimaniaAnimalEntity>) (EntityType<?>) type.get(),
                        context -> new AnimaniaAnimalRenderer(context, FarmLegacyModelLayers.LAYERS.get(id)));
            }
        }));
        EntityRenderers.register(FarmContent.BROWN_EGG_PROJECTILE.get(), ThrownItemRenderer::new);
    }
}
