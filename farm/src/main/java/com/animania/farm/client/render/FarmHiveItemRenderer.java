package com.animania.farm.client.render;

import com.animania.farm.FarmHiveItem;
import com.animania.farm.client.model.FarmLegacyPropModels;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** Item renderer shared by the player-made and wild exact hive meshes. */
@OnlyIn(Dist.CLIENT)
public final class FarmHiveItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation("animania_farm", "textures/entity/props/bee_hive.png");
    private final ModelPart hive = FarmLegacyPropModels.create("model_bee_hive");
    private final ModelPart wildHive = FarmLegacyPropModels.create("model_wild_hive");

    public FarmHiveItemRenderer(BlockEntityRenderDispatcher dispatcher, EntityModelSet models) {
        super(dispatcher, models);
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose,
                             MultiBufferSource buffers, int packedLight, int packedOverlay) {
        boolean wild = stack.getItem() instanceof FarmHiveItem item && item.isWild();
        ModelPart model = wild ? wildHive : hive;
        model.getAllParts().forEach(ModelPart::resetPose);
        pose.pushPose();
        pose.translate(0.5D, wild ? 1.0D : 1.5D, 0.5D);
        pose.scale(1.0F, -1.0F, -1.0F);
        if (context == ItemDisplayContext.GUI) pose.mulPose(Axis.YP.rotationDegrees(35.0F));
        // Match block rendering and the legacy inventory renderer.  The hive
        // is an opaque stacked mesh, so its reverse faces must be culled.
        model.render(pose, buffers.getBuffer(RenderType.entityCutout(TEXTURE)),
                packedLight, OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }
}
