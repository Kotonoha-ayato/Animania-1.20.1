package com.animania.catsdogs.client.render;

import com.animania.catsdogs.CatsDogsPetFacilityBlockEntity;
import com.animania.catsdogs.client.model.CatsDogsLegacyPropModels;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.Map;

/** Native renderer for all six legacy legacy model pet facilities. */
public final class CatsDogsPetFacilityRenderer implements BlockEntityRenderer<CatsDogsPetFacilityBlockEntity> {
    private final Map<String, ModelPart> models = new LinkedHashMap<>();

    public CatsDogsPetFacilityRenderer(BlockEntityRendererProvider.Context context) {
        for (String id : new String[]{"cat_bed_1", "cat_bed_2", "cat_tower", "dog_house", "dog_pillow", "litter_box"}) {
            models.put(id, CatsDogsLegacyPropModels.create("model_" + id));
        }
    }

    @Override
    public void render(CatsDogsPetFacilityBlockEntity entity, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int packedLight, int packedOverlay) {
        String id = entity.facilityType();
        ModelPart model = models.get(id);
        if (model == null) return;
        pose.pushPose();
        pose.translate(0.5D, 1.5D, 0.5D);
        pose.scale(1.0F, -1.0F, -1.0F);
        pose.mulPose(Axis.YP.rotationDegrees(entity.getBlockState()
                .getValue(HorizontalDirectionalBlock.FACING).toYRot()));
        ResourceLocation texture = new ResourceLocation("animania_catsdogs", "textures/entity/tileentities/" + id + ".png");
        model.render(pose, buffers.getBuffer(RenderType.entityCutout(texture)), packedLight, OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }
}
