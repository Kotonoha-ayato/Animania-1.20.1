package com.animania.client;

import com.animania.client.model.AnimaniaAnimalModel;
import com.animania.client.model.AnimaniaVehicleModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.ResourceLocation;

public final class AnimaniaClient {
    public static final ModelLayerLocation ANIMAL_LAYER = new ModelLayerLocation(new ResourceLocation("animania", "animal"), "main");
    public static final ModelLayerLocation VEHICLE_LAYER = new ModelLayerLocation(new ResourceLocation("animania", "vehicle"), "main");

    private AnimaniaClient() {
    }

    public static void registerLayers(net.minecraftforge.client.event.EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(ANIMAL_LAYER, AnimaniaAnimalModel::createBodyLayer);
        event.registerLayerDefinition(VEHICLE_LAYER, AnimaniaVehicleModel::createBodyLayer);
    }
}
