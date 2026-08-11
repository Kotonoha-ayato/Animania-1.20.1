package com.animania.extra.client.render;

import com.animania.extra.ExtraHamsterWheelBlockEntity;
import com.animania.extra.client.model.ExtraLegacyPropModels;
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
        wheel = ExtraLegacyPropModels.create("model_hamster_wheel");
        wheelRotor = wheel.getChild("base1").getChild("wheel1");
        hamster = context.bakeLayer(ExtraNativeModelLayers.LAYERS.get("hamster"));
    }

    @Override
    public void render(ExtraHamsterWheelBlockEntity entity, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int packedLight, int packedOverlay) {
        wheel.getAllParts().forEach(ModelPart::resetPose);
        pose.pushPose();
        pose.translate(0.5D, 1.5D, 0.5D);
        pose.scale(1.0F, -1.0F, -1.0F);
        pose.mulPose(Axis.YP.rotationDegrees(entity.getBlockState().getValue(ExtraHamsterWheelBlock.FACING).toYRot()));
        var wheelBuffer = buffers.getBuffer(RenderType.entityCutoutNoCull(WHEEL_TEXTURE));
        wheelRotor.visible = false;
        wheel.render(pose, wheelBuffer, packedLight, OverlayTexture.NO_OVERLAY);
        wheelRotor.visible = true;
        pose.pushPose();
        pose.translate(0.0F, 6.5F / 16.0F, 0.0F);
        wheelRotor.setPos(0.0F, 0.0F, 0.0F);
        wheelRotor.xRot = 0.0F;
        wheelRotor.yRot = 0.0F;
        wheelRotor.zRot = entity.isRunning() && entity.getLevel() != null
                ? -(entity.getLevel().getGameTime() + partialTick) * ((float) Math.PI / 40.0F) : 0.0F;
        wheelRotor.render(pose, wheelBuffer, packedLight, OverlayTexture.NO_OVERLAY);
        pose.popPose();
        if (entity.hasHamster()) {
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
