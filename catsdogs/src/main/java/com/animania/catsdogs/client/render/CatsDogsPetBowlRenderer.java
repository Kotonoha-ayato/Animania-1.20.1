package com.animania.catsdogs.client.render;

import com.animania.catsdogs.CatsDogsPetBowlBlockEntity;
import com.animania.catsdogs.client.model.CatsDogsNativeModelLayers;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/** Native legacy model replacement renderer for the pet bowl shell. */
public final class CatsDogsPetBowlRenderer implements BlockEntityRenderer<CatsDogsPetBowlBlockEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("animania_catsdogs", "textures/entity/tileentities/pet_bowl.png");
    private final ModelPart model;

    public CatsDogsPetBowlRenderer(BlockEntityRendererProvider.Context context) {
        model = context.bakeLayer(CatsDogsNativeModelLayers.LAYERS.get("model_pet_bowl"));
    }

    @Override
    public void render(CatsDogsPetBowlBlockEntity entity, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int packedLight, int packedOverlay) {
        pose.pushPose();
        pose.translate(0.5D, 1.5D, 0.5D);
        pose.scale(1.0F, -1.0F, -1.0F);
        model.render(pose, buffers.getBuffer(RenderType.entityCutout(TEXTURE)), packedLight, OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }
}
