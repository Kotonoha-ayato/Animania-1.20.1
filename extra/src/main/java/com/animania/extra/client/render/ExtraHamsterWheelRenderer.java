package com.animania.extra.client.render;

import com.animania.extra.ExtraHamsterWheelBlockEntity;
import com.animania.extra.client.model.ExtraNativeModelLayers;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.animania.extra.ExtraHamsterWheelBlock;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/** Native animated renderer for the legacy hamster wheel and runner model. */
public final class ExtraHamsterWheelRenderer implements BlockEntityRenderer<ExtraHamsterWheelBlockEntity> {
    private static final ResourceLocation WHEEL_TEXTURE = new ResourceLocation("animania_extra", "textures/entity/tileentities/hamster_wheel.png");
    private static final ResourceLocation HAMSTER_TEXTURE = new ResourceLocation("animania_extra", "textures/entity/rodents/hamster_tarou.png");
    private final ModelPart wheel;
    private final ModelPart wheelRotor;
    private final ModelPart hamster;

    public ExtraHamsterWheelRenderer(BlockEntityRendererProvider.Context context) {
        wheel = context.bakeLayer(ExtraNativeModelLayers.LAYERS.get("model_hamster_wheel"));
        wheelRotor = wheel.getChild("base1").getChild("wheel1");
        hamster = context.bakeLayer(ExtraNativeModelLayers.LAYERS.get("hamster"));
    }

    @Override
    public void render(ExtraHamsterWheelBlockEntity entity, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int packedLight, int packedOverlay) {
        wheel.resetPose();
        if (entity.isRunning() && entity.getLevel() != null) {
            wheelRotor.zRot += (entity.getLevel().getGameTime() + partialTick) * 0.35F;
        }
        pose.pushPose();
        pose.translate(0.5D, 1.5D, 0.5D);
        pose.scale(1.0F, -1.0F, -1.0F);
        pose.mulPose(Axis.YP.rotationDegrees(entity.getBlockState().getValue(ExtraHamsterWheelBlock.FACING).toYRot()));
        wheel.render(pose, buffers.getBuffer(RenderType.entityCutout(WHEEL_TEXTURE)), packedLight, OverlayTexture.NO_OVERLAY);
        if (entity.isRunning()) {
            pose.pushPose();
            pose.scale(0.5F, 0.5F, 0.5F);
            pose.translate(0.0D, 0.9D, 0.0D);
            pose.mulPose(Axis.YP.rotationDegrees(-90.0F));
            hamster.render(pose, buffers.getBuffer(RenderType.entityCutout(HAMSTER_TEXTURE)), packedLight, OverlayTexture.NO_OVERLAY);
            pose.popPose();
        }
        pose.popPose();
    }
}
