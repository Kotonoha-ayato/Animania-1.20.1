package com.animania.extra.client.render;

import com.animania.extra.client.model.ExtraLegacyPropModels;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** Item-space renderer for the same exact native mesh used by the placed wheel. */
@OnlyIn(Dist.CLIENT)
public final class ExtraHamsterWheelItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation("animania_extra", "textures/entity/tileentities/hamster_wheel.png");
    private final ModelPart wheel = ExtraLegacyPropModels.create("model_hamster_wheel");

    public ExtraHamsterWheelItemRenderer(BlockEntityRenderDispatcher dispatcher, EntityModelSet models) {
        super(dispatcher, models);
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose,
                             MultiBufferSource buffers, int packedLight, int packedOverlay) {
        wheel.getAllParts().forEach(ModelPart::resetPose);
        pose.pushPose();
        pose.translate(0.5D, 1.5D, 0.5D);
        pose.scale(1.0F, -1.0F, -1.0F);
        if (context == ItemDisplayContext.GUI) {
            pose.mulPose(Axis.YP.rotationDegrees(35.0F));
        }
        wheel.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)),
                packedLight, OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }
}
