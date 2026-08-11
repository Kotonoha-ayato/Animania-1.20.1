package com.animania.catsdogs.client.render;

import com.animania.catsdogs.CatsDogsPetBowlBlockEntity;
import com.animania.catsdogs.client.model.CatsDogsPetBowlModel;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** Native renderer for the active 1.12 Java pet-bowl shell and kibble model. */
public final class CatsDogsPetBowlRenderer implements BlockEntityRenderer<CatsDogsPetBowlBlockEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("animania_catsdogs", "textures/entity/tileentities/pet_bowl.png");
    private final CatsDogsPetBowlModel model;

    public CatsDogsPetBowlRenderer(BlockEntityRendererProvider.Context context) {
        model = new CatsDogsPetBowlModel(context.bakeLayer(CatsDogsPetBowlModel.LAYER));
    }

    @Override
    public void render(CatsDogsPetBowlBlockEntity entity, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int packedLight, int packedOverlay) {
        pose.pushPose();
        pose.translate(0.5D, 1.5D, 0.5D);
        pose.scale(1.0F, -1.0F, -1.0F);
        var consumer = buffers.getBuffer(RenderType.entityCutout(TEXTURE));
        model.renderShell(pose, consumer, packedLight, OverlayTexture.NO_OVERLAY);

        ItemStack food = entity.getItem(0);
        if (!food.isEmpty()) {
            int color = Minecraft.getInstance().getItemColors().getColor(food, 0);
            if (color == -1) color = 0xA8783F;
            float red = ((color >> 16) & 255) / 255.0F;
            float green = ((color >> 8) & 255) / 255.0F;
            float blue = (color & 255) / 255.0F;
            pose.pushPose();
            pose.scale(1.2F, 1.2F, 1.2F);
            pose.translate(0.0F, -0.12F - (food.getCount() - 1) * 0.04F, 0.0F);
            model.renderFood(pose, consumer, packedLight, OverlayTexture.NO_OVERLAY, red, green, blue);
            pose.popPose();
        }
        pose.popPose();
    }
}
