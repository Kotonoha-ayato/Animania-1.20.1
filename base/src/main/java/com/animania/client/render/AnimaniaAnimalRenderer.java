package com.animania.client.render;

import com.animania.client.model.AnimaniaAnimalModel;
import com.animania.common.entity.AnimaniaAnimalEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

/** Client-only renderer using native ModelPart animation and legacy-compatible IDs. */
public class AnimaniaAnimalRenderer extends MobRenderer<AnimaniaAnimalEntity, AnimaniaAnimalModel> {
    public AnimaniaAnimalRenderer(EntityRendererProvider.Context context) {
        super(context, new AnimaniaAnimalModel(context.bakeLayer(com.animania.client.AnimaniaClient.ANIMAL_LAYER)), 0.45f);
    }

    @Override
    public ResourceLocation getTextureLocation(AnimaniaAnimalEntity entity) {
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (id == null) return new ResourceLocation("animania", "textures/entity/default.png");
        return new ResourceLocation(id.getNamespace(), "textures/entity/" + id.getPath() + ".png");
    }

    @Override
    protected void scale(AnimaniaAnimalEntity entity, PoseStack poseStack, float partialTickTime) {
        if (entity.isBaby()) poseStack.scale(0.55f, 0.55f, 0.55f);
    }
}
