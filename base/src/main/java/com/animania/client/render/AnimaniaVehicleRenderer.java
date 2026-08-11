package com.animania.client.render;

import com.animania.client.model.AnimaniaVehicleModel;
import com.animania.common.entity.AnimaniaVehicleEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraftforge.registries.ForgeRegistries;

/** Native renderer for farm pullables; no legacy model runtime dependency. */
public final class AnimaniaVehicleRenderer extends EntityRenderer<AnimaniaVehicleEntity> {
    private final AnimaniaVehicleModel model;

    public AnimaniaVehicleRenderer(EntityRendererProvider.Context context, ModelLayerLocation layer,
                                   AnimationDefinition movementAnimation) {
        super(context);
        this.model = new AnimaniaVehicleModel(context.bakeLayer(layer), movementAnimation);
        this.shadowRadius = 0.55F;
    }

    @Override
    public void render(AnimaniaVehicleEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        poseStack.translate(0.0D, 1.5D, 0.0D);
        poseStack.scale(1.0F, -1.0F, -1.0F);
        model.setupAnim(entity, 0.0F, 0.0F, entity.tickCount + partialTick, entityYaw, 0.0F);
        model.renderToBuffer(poseStack, buffer.getBuffer(model.renderType(getTextureLocation(entity))), packedLight,
                net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(AnimaniaVehicleEntity entity) {
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        return id == null ? new ResourceLocation("animania", "textures/entity/props/cart.png")
                : new ResourceLocation(id.getNamespace(), "textures/entity/" + id.getPath() + ".png");
    }
}
