package com.animania.client.render;

import com.animania.client.model.AnimaniaHamsterBallModel;
import com.animania.client.model.LegacyAnimalModel;
import com.animania.common.entity.AnimaniaAnimalEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;

/** Renders the translucent cage without coupling Base to Extra's Java code. */
public final class AnimaniaHamsterBallLayer extends RenderLayer<AnimaniaAnimalEntity, LegacyAnimalModel> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            "animania_extra", "textures/entity/rodents/hamster_ball.png");
    private final ModelPart ball;

    public AnimaniaHamsterBallLayer(RenderLayerParent<AnimaniaAnimalEntity, LegacyAnimalModel> parent,
                                    ModelPart modelRoot) {
        super(parent);
        this.ball = modelRoot.getChild("ball");
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffers, int packedLight,
                       AnimaniaAnimalEntity entity, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (!entity.isHamster() || !entity.isInBall()) return;
        ball.resetPose();
        ball.xRot = (entity.tickCount + partialTick) * 0.2F;
        int color = entity.getBallColor();
        float red = 1.0F, green = 1.0F, blue = 1.0F;
        if (color != 16) {
            int rgb = DyeColor.byId(Math.max(0, Math.min(15, color))).getFireworkColor();
            red = ((rgb >> 16) & 0xFF) / 255.0F;
            green = ((rgb >> 8) & 0xFF) / 255.0F;
            blue = (rgb & 0xFF) / 255.0F;
        }
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityTranslucentCull(TEXTURE));
        ball.render(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY, red, green, blue, 0.72F);
    }
}
