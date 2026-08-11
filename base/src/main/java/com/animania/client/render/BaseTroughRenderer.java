package com.animania.client.render;

import com.animania.client.model.BaseLegacyModelLayers;
import com.animania.common.AnimaniaBlocks;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import com.animania.common.block.AnimaniaTroughBlock;
import com.mojang.math.Axis;
import net.minecraft.core.Direction;

/** Native replacement for the legacy trough shell and its server-synchronised contents. */
public final class BaseTroughRenderer implements BlockEntityRenderer<AnimaniaBlocks.TroughEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("animania", "textures/entity/tileentities/block_trough.png");
    private static final String[] SHELL = {"block1", "block2", "block3", "block4", "block5", "base1", "base2"};
    private static final String[] FOOD = {"feed", "feed_a", "feed_b", "feed_c", "feed_d", "feed_e", "feed_f", "feed_g", "feed_h",
            "feed_a1", "feed_b1", "feed_c1", "feed_d1", "feed_e1", "feed_f1", "feed_g1", "feed_h1"};
    private final ModelPart model;

    public BaseTroughRenderer(BlockEntityRendererProvider.Context context) {
        model = context.bakeLayer(BaseLegacyModelLayers.LAYERS.get("trough"));
    }

    @Override
    public void render(AnimaniaBlocks.TroughEntity entity, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int packedLight, int packedOverlay) {
        BaseLegacyFacilityRenderSupport.hideAll(model);
        BaseLegacyFacilityRenderSupport.show(model, SHELL);
        if (!entity.getItem(0).isEmpty()) BaseLegacyFacilityRenderSupport.show(model, FOOD);
        FluidStack fluid = entity.fluidSnapshot();
        if (!fluid.isEmpty()) {
            int amount = fluid.getAmount();
            BaseLegacyFacilityRenderSupport.show(model, amount > 666 ? "slop1" : amount > 333 ? "slop2" : "slop3");
        }
        pose.pushPose();
        Direction facing = entity.getBlockState().getValue(AnimaniaTroughBlock.FACING);
        switch (facing) {
            case EAST -> pose.translate(1.5D, 1.5D, 0.5D);
            case WEST -> { pose.translate(-0.5D, 1.5D, 0.5D); pose.mulPose(Axis.YP.rotationDegrees(180)); }
            case NORTH -> { pose.translate(0.5D, 1.5D, -0.5D); pose.mulPose(Axis.YP.rotationDegrees(90)); }
            default -> { pose.translate(0.5D, 1.5D, 1.5D); pose.mulPose(Axis.YP.rotationDegrees(270)); }
        }
        pose.scale(-1.0F, -1.0F, 1.0F);
        BaseLegacyFacilityRenderSupport.render(model, pose, buffers, TEXTURE, packedLight, 1, 1, 1, 1);
        pose.popPose();
    }
}
