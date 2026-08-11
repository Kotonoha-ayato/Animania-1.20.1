package com.animania.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

final class BaseLegacyFacilityRenderSupport {
    private BaseLegacyFacilityRenderSupport() {
    }

    static void hideAll(ModelPart root) {
        root.getAllParts().forEach(part -> part.visible = false);
        root.visible = true;
    }

    static void show(ModelPart root, String... names) {
        for (String name : names) root.getChild(name).visible = true;
    }

    static void render(ModelPart root, PoseStack pose, MultiBufferSource buffers, ResourceLocation texture,
                       int packedLight, float red, float green, float blue, float alpha) {
        root.render(pose, buffers.getBuffer(alpha < 1.0F ? RenderType.entityTranslucent(texture) : RenderType.entityCutout(texture)),
                packedLight, OverlayTexture.NO_OVERLAY, red, green, blue, alpha);
    }
}
