package com.animania.client.render;

import com.animania.client.model.LegacyAnimalModel;
import com.animania.common.entity.AnimaniaAnimalEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.registries.ForgeRegistries;

/** Native red-mushroom geometry for the adult cow/bull Mooshroom renderers. */
public final class AnimaniaMooshroomLayer extends RenderLayer<AnimaniaAnimalEntity, LegacyAnimalModel> {
    private final BlockRenderDispatcher blocks;

    public AnimaniaMooshroomLayer(RenderLayerParent<AnimaniaAnimalEntity, LegacyAnimalModel> parent,
                                  BlockRenderDispatcher blocks) {
        super(parent);
        this.blocks = blocks;
    }

    public static boolean supports(ResourceLocation id) {
        return id != null && "animania_farm".equals(id.getNamespace())
                && ("cow_mooshroom".equals(id.getPath()) || "bull_mooshroom".equals(id.getPath()));
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, AnimaniaAnimalEntity entity,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        if (entity.isBaby() || entity.isInvisible()
                || !supports(ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()))) return;
        pose.pushPose();
        pose.translate(0.2F, -0.35F, 0.5F);
        renderMushroom(pose, buffers, light, -48.0F);
        pose.popPose();
        pose.pushPose();
        pose.translate(0.3F, -0.35F, -0.1F);
        renderMushroom(pose, buffers, light, 42.0F);
        pose.popPose();
        pose.pushPose();
        getParentModel().translatePrimaryHead(pose);
        pose.translate(0.0F, -0.7F, -0.2F);
        renderMushroom(pose, buffers, light, -78.0F);
        pose.popPose();
    }

    private void renderMushroom(PoseStack pose, MultiBufferSource buffers, int light, float rotation) {
        pose.mulPose(Axis.YP.rotationDegrees(rotation));
        pose.scale(-1.0F, -1.0F, 1.0F);
        pose.translate(-0.5F, -0.5F, -0.5F);
        blocks.renderSingleBlock(Blocks.RED_MUSHROOM.defaultBlockState(), pose, buffers,
                light, OverlayTexture.NO_OVERLAY);
    }
}
