package com.animania.farm.client.render;

import com.animania.farm.FarmHiveBlockEntity;
import com.animania.farm.FarmHiveBlock;
import com.animania.farm.client.model.FarmLegacyPropModels;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
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
        hive = FarmLegacyPropModels.create("model_bee_hive");
        wildHive = FarmLegacyPropModels.create("model_wild_hive");
    }

    @Override
    public void render(FarmHiveBlockEntity entity, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int packedLight, int packedOverlay) {
        ModelPart model = entity.isWild() ? wildHive : hive;
        ResourceLocation texture = entity.isWild() ? WILD_HIVE : BEE_HIVE;
        model.getAllParts().forEach(ModelPart::resetPose);
        pose.pushPose();
        if (entity.isWild()) {
            switch (entity.getBlockState().getValue(FarmHiveBlock.FACING)) {
                case NORTH -> pose.translate(0.5D, 1.0D, 0.75D);
                case SOUTH -> pose.translate(0.5D, 1.0D, 0.25D);
                case EAST -> pose.translate(0.25D, 1.0D, 0.5D);
                case WEST -> pose.translate(0.75D, 1.0D, 0.5D);
                default -> pose.translate(0.5D, 1.0D, 0.5D);
            }
        } else {
            pose.translate(0.5D, 1.5D, 0.5D);
        }
        pose.scale(1.0F, -1.0F, -1.0F);
        pose.mulPose(Axis.YP.rotationDegrees(entity.getBlockState().getValue(FarmHiveBlock.FACING).toYRot()));
        model.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(texture)), packedLight, OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }
}
