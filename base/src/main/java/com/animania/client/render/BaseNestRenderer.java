package com.animania.client.render;

import com.animania.client.model.BaseLegacyModelLayers;
import com.animania.common.AnimaniaBlocks;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

/** Native replacement for the legacy nest renderer, including visible stored eggs. */
public final class BaseNestRenderer implements BlockEntityRenderer<AnimaniaBlocks.NestEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("animania", "textures/entity/tileentities/block_nest_white.png");
    private static final String[] SHELL = {"nest1", "fluff3", "fluff1", "nest2", "nest3", "nest4", "nest5",
            "nest6", "nest7", "nest8", "block", "fluff2", "fluff4", "fluff5"};
    private final ModelPart model;

    public BaseNestRenderer(BlockEntityRendererProvider.Context context) {
        model = context.bakeLayer(BaseLegacyModelLayers.LAYERS.get("nest"));
    }

    @Override
    public void render(AnimaniaBlocks.NestEntity entity, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int packedLight, int packedOverlay) {
        BaseLegacyFacilityRenderSupport.hideAll(model);
        BaseLegacyFacilityRenderSupport.show(model, SHELL);
        ItemStack eggs = entity.getItem(0);
        if (!eggs.isEmpty()) showEggs(eggs, Math.min(3, eggs.getCount()));
        pose.pushPose();
        pose.translate(0.5D, 1.5D, 0.5D);
        pose.scale(-1.0F, -1.0F, 1.0F);
        BaseLegacyFacilityRenderSupport.render(model, pose, buffers, TEXTURE, packedLight, 1, 1, 1, 1);
        pose.popPose();
    }

    private void showEggs(ItemStack stack, int count) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        String path = id == null ? "" : id.getPath();
        String prefix = path.contains("peacock") && path.contains("white") ? "w_egg"
                : path.contains("peacock") || path.contains("blue") ? "bl_egg"
                : path.contains("brown") ? "b_egg" : "egg";
        for (int egg = 1; egg <= count; egg++) {
            BaseLegacyFacilityRenderSupport.show(model, prefix + egg, prefix + egg + "a", prefix + egg + "b", prefix + egg + "c");
        }
    }
}
