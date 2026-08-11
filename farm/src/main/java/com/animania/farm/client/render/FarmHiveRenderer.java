package com.animania.farm.client.render;

import com.animania.farm.FarmHiveBlockEntity;
import com.animania.farm.client.model.FarmNativeModelLayers;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/** Native renderer for the player and wild hive legacy model models. */
public final class FarmHiveRenderer implements BlockEntityRenderer<FarmHiveBlockEntity> {
    private static final ResourceLocation BEE_HIVE = new ResourceLocation("animania_farm", "textures/entity/props/bee_hive.png");
    private static final ResourceLocation WILD_HIVE = new ResourceLocation("animania_farm", "textures/entity/props/wild_hive.png");
    private final ModelPart hive;
    private final ModelPart wildHive;

    public FarmHiveRenderer(BlockEntityRendererProvider.Context context) {
        hive = context.bakeLayer(FarmNativeModelLayers.LAYERS.get("model_bee_hive"));
        wildHive = context.bakeLayer(FarmNativeModelLayers.LAYERS.get("model_wild_hive"));
    }

    @Override
    public void render(FarmHiveBlockEntity entity, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int packedLight, int packedOverlay) {
        ModelPart model = entity.isWild() ? wildHive : hive;
        ResourceLocation texture = entity.isWild() ? WILD_HIVE : BEE_HIVE;
        pose.pushPose();
        pose.translate(0.5D, 1.5D, 0.5D);
        pose.scale(1.0F, -1.0F, -1.0F);
        model.render(pose, buffers.getBuffer(RenderType.entityCutout(texture)), packedLight, OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }
}
