package com.animania.farm;

import com.animania.client.render.AnimaniaAnimalRenderer;
import com.animania.client.render.AnimaniaVehicleRenderer;
import com.animania.common.entity.AnimaniaVehicleEntity;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import com.animania.common.entity.AnimaniaAnimalEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;

final class AnimaniaFarmClient {
    private AnimaniaFarmClient() {
    }

    static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> AnimaniaFarm.ENTITIES.forEach((id, type) -> {
            if (FarmLegacyIds.isVehicle(id)) {
                EntityRenderers.register((EntityType<AnimaniaVehicleEntity>) (EntityType<?>) type.get(), AnimaniaVehicleRenderer::new);
            } else {
                EntityRenderers.register((EntityType<AnimaniaAnimalEntity>) (EntityType<?>) type.get(), AnimaniaAnimalRenderer::new);
            }
        }));
        EntityRenderers.register(FarmContent.BROWN_EGG_PROJECTILE.get(), ThrownItemRenderer::new);
    }
}
