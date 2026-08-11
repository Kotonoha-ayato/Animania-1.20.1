package com.animania.client.render;

import com.animania.client.model.LegacyAnimalModel;
import com.animania.common.entity.AnimaniaAnimalEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Native 1.20.1 replacement for the 1.12 LayerBlinking renderer.
 *
 * The old implementation rendered the already-posed model twice with
 * transparent left/right eyelid textures.  Keeping that contract is
 * important: the blink images contain only the eyelid pixels and must not
 * replace the breed coat texture.  The timer is server state and is synced
 * through SynchedEntityData by {@link AnimaniaAnimalEntity}.
 */
public final class AnimaniaBlinkingLayer extends RenderLayer<AnimaniaAnimalEntity, LegacyAnimalModel> {
    public AnimaniaBlinkingLayer(RenderLayerParent<AnimaniaAnimalEntity, LegacyAnimalModel> parent) {
        super(parent);
    }

    /** Returns the two transparent eyelid textures for a registered ID. */
    public static ResourceLocation[] texturesFor(ResourceLocation id) {
        if (id == null) return null;
        String namespace = id.getNamespace();
        String path = id.getPath();
        if ("animania_farm".equals(namespace)) {
            String base;
            if (path.startsWith("cow_")) base = "cows/cow_blink";
            else if (path.startsWith("bull_")) base = "cows/bull_blink";
            else if (path.startsWith("calf_")) base = "cows/calf_blink";
            else if (path.startsWith("doe_angora") || path.startsWith("buck_angora") || path.startsWith("kid_angora")) base = "goats/angora_blink";
            else if (path.startsWith("doe_") || path.startsWith("buck_") || path.startsWith("kid_")) base = "goats/goats_blink";
            else if (path.startsWith("mare_") || path.startsWith("stallion_") || path.startsWith("foal_")) base = "horses/horse_blink";
            else if (path.startsWith("piglet_")) base = "pigs/piglet_blink";
            else if (path.startsWith("sow_") || path.startsWith("hog_")) base = path.endsWith("_hampshire") ? "pigs/hampshire_blink" : "pigs/pig_blink";
            else if (path.startsWith("hen_") || path.startsWith("rooster_")) base = "chickens/chicken_blink";
            else if (path.startsWith("chick_")) base = "chickens/chick_blink";
            else if (path.startsWith("ewe_") || path.startsWith("ram_") || path.startsWith("lamb_")) base = "sheep/sheep_blink";
            else return null;
            return pair(namespace, base);
        }
        if ("animania_extra".equals(namespace)) {
            String base;
            if (path.equals("hamster")) base = "rodents/hamster_blink";
            else if (path.startsWith("ferret_")) base = "rodents/ferret_blink";
            else if (path.startsWith("hedgehog")) base = "rodents/hedgehog_blink";
            else if (path.startsWith("doe_") || path.startsWith("buck_") || path.startsWith("kit_")) base = "rabbits/rabbit_blink";
            else if (path.startsWith("peachick_")) base = "peacocks/peachick_blink";
            else if (path.startsWith("peacock_")) base = "peacocks/peacock_blink";
            else if (path.startsWith("peahen_")) base = "peacocks/peafowl_blink";
            else return null;
            return pair(namespace, base);
        }
        if ("animania_catsdogs".equals(namespace)) {
            if (path.startsWith("tom_") || path.startsWith("queen_") || path.startsWith("kitten_")) {
                String breed = path.substring(path.indexOf('_') + 1);
                String base = (breed.equals("ragdoll") || breed.equals("norwegian")) ? "cats/blink_2" : "cats/blink_1";
                return pair(namespace, base);
            }
            if (path.startsWith("male_") || path.startsWith("female_") || path.startsWith("puppy_")) {
                String breed = path.substring(path.indexOf('_') + 1);
                String base = switch (breed) {
                    case "blood_hound" -> "dogs/blink_blood_hound";
                    case "chihuahua" -> "dogs/blink_chihuahua";
                    case "corgi" -> "dogs/blink_corgi";
                    case "dachshund" -> "dogs/blink_dachshund";
                    case "fox" -> "dogs/blink_fox";
                    case "greyhound" -> "dogs/blink_greyhound";
                    case "pomeranian" -> "dogs/blink_pomeranian";
                    case "poodle" -> "dogs/blink_poodle";
                    case "pug" -> "dogs/blink_pug";
                    default -> "dogs/blink_collie";
                };
                // Cat eyelids were supplied as two transparent layers. The
                // original dog renderers instead use one full-size overlay
                // (for example blink_collie.png), so appending _left/_right
                // requested files that have never existed and flashed the
                // missing-texture checkerboard for the blink duration.
                return single(namespace, base);
            }
        }
        return null;
    }

    private static ResourceLocation[] pair(String namespace, String base) {
        return new ResourceLocation[]{
                ResourceLocation.fromNamespaceAndPath(namespace, "textures/entity/" + base + "_left.png"),
                ResourceLocation.fromNamespaceAndPath(namespace, "textures/entity/" + base + "_right.png")};
    }

    private static ResourceLocation[] single(String namespace, String base) {
        return new ResourceLocation[]{
                ResourceLocation.fromNamespaceAndPath(namespace, "textures/entity/" + base + ".png")};
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffers, int packedLight,
                       AnimaniaAnimalEntity entity, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        int timer = entity.getBlinkTimer();
        if (timer < 0 || timer >= 7) return;
        ResourceLocation[] textures = texturesFor(ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()));
        if (textures == null) return;
        for (ResourceLocation texture : textures) {
            VertexConsumer consumer = buffers.getBuffer(RenderType.entityTranslucent(texture));
            getParentModel().renderToBuffer(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY,
                    1.0F, 1.0F, 1.0F, 1.0F);
        }
    }
}
