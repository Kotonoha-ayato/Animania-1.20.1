package com.animania.client.render;

import com.animania.common.config.AnimaniaConfig;
import com.animania.common.entity.AnimaniaAnimalEntity;
import com.animania.common.item.AnimaniaEntityEggItem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Native 1.20.1 replacement for RenderAnimatedEgg and
 * AnimatedEggModelWrapper.  It previews the actual registered child model in
 * inventory/GUI transforms without creating an entity in the world.
 */
@OnlyIn(Dist.CLIENT)
public final class AnimaniaEggItemRenderer extends BlockEntityWithoutLevelRenderer {
    public AnimaniaEggItemRenderer(net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher dispatcher,
                                   net.minecraft.client.model.geom.EntityModelSet models) {
        super(dispatcher, models);
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack,
                             MultiBufferSource buffers, int packedLight, int packedOverlay) {
        if (!(stack.getItem() instanceof AnimaniaEntityEggItem egg)) return;
        Level level = Minecraft.getInstance().level;
        if (level == null) return;
        AnimaniaAnimalEntity preview = egg.createPreview(level);
        if (preview == null) return;
        poseStack.pushPose();
        float extent = Math.max(preview.getBbWidth(), preview.getBbHeight());
        float scale = extent <= 0.0F ? 0.5F : 0.62F / extent;
        switch (displayContext) {
            case GUI -> {
                poseStack.translate(0.5D, 0.08D, 0.5D);
                poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(rotationDegrees()));
                poseStack.scale(scale, scale, scale);
            }
            case GROUND -> {
                poseStack.translate(0.5D, 0.02D, 0.5D);
                poseStack.scale(scale * 0.82F, scale * 0.82F, scale * 0.82F);
            }
            default -> {
                poseStack.translate(0.5D, 0.1D, 0.5D);
                poseStack.scale(scale * 0.82F, scale * 0.82F, scale * 0.82F);
            }
        }
        preview.tickCount = 1;
        Minecraft.getInstance().getEntityRenderDispatcher().render(
                preview, 0.0D, 0.0D, 0.0D, preview.getYRot(), 1.0F,
                poseStack, buffers, packedLight);
        poseStack.popPose();
    }

    private static float rotationDegrees() {
        if (!AnimaniaConfig.FANCY_EGGS_ROTATE.get()) return 20.0F;
        long millis = System.currentTimeMillis() % 5000L;
        return (millis / 5000.0F) * 360.0F;
    }
}
