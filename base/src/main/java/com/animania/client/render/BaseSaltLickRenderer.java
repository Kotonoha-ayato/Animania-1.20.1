package com.animania.client.render;

import com.animania.client.model.BaseLegacyModelLayers;
import com.animania.common.block.AnimaniaSaltLickBlockEntity;
import com.animania.common.config.AnimaniaConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/** Renders the shrinking 1.12 salt-lick geometry with native ModelPart APIs. */
public final class BaseSaltLickRenderer implements BlockEntityRenderer<AnimaniaSaltLickBlockEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("animania", "textures/entity/tileentities/salt_lick.png");
    private final ModelPart model;

    public BaseSaltLickRenderer(BlockEntityRendererProvider.Context context) {
        model = context.bakeLayer(BaseLegacyModelLayers.LAYERS.get("salt_lick"));
    }

    @Override
    public void render(AnimaniaSaltLickBlockEntity entity, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int packedLight, int packedOverlay) {
        float remaining = Math.max(0.05F, entity.usesLeft() / (float) Math.max(1, AnimaniaConfig.SALT_LICK_MAX_USES.get()));
        pose.pushPose();
        pose.translate(0.5D, 1.5D, 0.5D);
        pose.scale(1.0F, -remaining, -1.0F);
        BaseLegacyFacilityRenderSupport.render(model, pose, buffers, TEXTURE, packedLight, 1, 1, 1, 1);
        pose.popPose();
    }
}
