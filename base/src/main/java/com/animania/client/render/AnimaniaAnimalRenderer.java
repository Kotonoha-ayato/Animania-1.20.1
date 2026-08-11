package com.animania.client.render;

import com.animania.client.model.LegacyAnimalModel;
import com.animania.client.model.LegacyAnimationProfile;
import com.animania.client.model.LegacyPoseDefinition;
import com.animania.client.model.LegacyRenderTransform;
import com.animania.client.model.LegacyPetAnimationDefinition;
import com.animania.client.AnimaniaClientDiagnostics;
import com.animania.common.entity.AnimaniaAnimalEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

/** Client-only renderer using native ModelPart animation and legacy-compatible IDs. */
public class AnimaniaAnimalRenderer extends MobRenderer<AnimaniaAnimalEntity, LegacyAnimalModel> {
    private final float modelScale;
    private final LegacyRenderTransform renderTransform;

    public AnimaniaAnimalRenderer(EntityRendererProvider.Context context, ModelLayerLocation layer,
                                  LegacyAnimationProfile profile, float modelScale) {
        this(context, layer, profile, LegacyPoseDefinition.EMPTY, LegacyPetAnimationDefinition.EMPTY,
                LegacyRenderTransform.EMPTY, modelScale);
    }

    public AnimaniaAnimalRenderer(EntityRendererProvider.Context context, ModelLayerLocation layer,
                                  LegacyAnimationProfile profile, LegacyPoseDefinition sittingPose,
                                  float modelScale) {
        this(context, layer, profile, sittingPose, LegacyPetAnimationDefinition.EMPTY,
                LegacyRenderTransform.EMPTY, modelScale);
    }

    public AnimaniaAnimalRenderer(EntityRendererProvider.Context context, ModelLayerLocation layer,
                                  LegacyAnimationProfile profile, LegacyPoseDefinition sittingPose,
                                  LegacyRenderTransform renderTransform, float modelScale) {
        this(context, layer, profile, sittingPose, LegacyPetAnimationDefinition.EMPTY, renderTransform, modelScale);
    }

    public AnimaniaAnimalRenderer(EntityRendererProvider.Context context, ModelLayerLocation layer,
                                  LegacyAnimationProfile profile, LegacyPoseDefinition sittingPose,
                                  LegacyPetAnimationDefinition petAnimation,
                                  LegacyRenderTransform renderTransform, float modelScale) {
        super(context, new LegacyAnimalModel(context.bakeLayer(layer), profile, sittingPose, petAnimation), 0.45f);
        this.modelScale = modelScale;
        this.renderTransform = renderTransform;
        addLayer(new AnimaniaHamsterBallLayer(this, context.bakeLayer(com.animania.client.model.AnimaniaHamsterBallModel.LAYER)));
        addLayer(new AnimaniaBlinkingLayer(this));
        addLayer(new AnimaniaMudLayer(this));
        addLayer(new AnimaniaMooshroomLayer(this, context.getBlockRenderDispatcher()));
    }

    @Override
    public ResourceLocation getTextureLocation(AnimaniaAnimalEntity entity) {
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        ResourceLocation defaultTexture = ResourceLocation.fromNamespaceAndPath("animania", "textures/entity/default.png");
        if (id == null) {
            AnimaniaClientDiagnostics.textureResolution(defaultTexture, defaultTexture, defaultTexture, "unregistered_entity");
            return defaultTexture;
        }
        ResourceLocation resolved = LegacyAnimalTextures.resolve(id, entity);
        // The preserved 1.12 tree uses nested family directories, while some
        // modern addon packs intentionally keep an ID-named fallback at the
        // entity root.  Never hand the renderer a missing location: that is
        // the direct cause of the purple/black checkerboard seen for a bad
        // variant or an old save carrying an obsolete variant string.
        if (Minecraft.getInstance().getResourceManager().getResource(resolved).isPresent()) {
            AnimaniaClientDiagnostics.textureResolution(id, resolved, resolved, "requested");
            return resolved;
        }
        ResourceLocation flat = ResourceLocation.fromNamespaceAndPath(id.getNamespace(),
                "textures/entity/" + id.getPath() + ".png");
        if (Minecraft.getInstance().getResourceManager().getResource(flat).isPresent()) {
            AnimaniaClientDiagnostics.textureResolution(id, resolved, flat, "flat_fallback");
            return flat;
        }
        if (Minecraft.getInstance().getResourceManager().getResource(defaultTexture).isPresent()) {
            AnimaniaClientDiagnostics.textureResolution(id, resolved, defaultTexture, "default_fallback");
            return defaultTexture;
        }
        AnimaniaClientDiagnostics.textureResolution(id, resolved, defaultTexture, "default_missing");
        return defaultTexture;
    }

    @Override
    protected void scale(AnimaniaAnimalEntity entity, PoseStack poseStack, float partialTickTime) {
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        boolean pets = id != null && "animania_catsdogs".equals(id.getNamespace());
        String path = pets ? id.getPath() : "";
        boolean cat = path.startsWith("queen_") || path.startsWith("tom_") || path.startsWith("kitten_");
        boolean fox = path.endsWith("_fox");
        boolean child = path.startsWith("puppy_") || path.startsWith("kitten_");

        // RenderDogGeneric applies its factory translation before scaling;
        // RenderFox applies the 0.1 Y translation at the very end instead.
        if (!fox) poseStack.translate(renderTransform.x(), renderTransform.y(), renderTransform.z());
        float scale = child ? modelScale * (1.0F + 0.8F * entity.growthProgress()) : modelScale;
        poseStack.scale(scale, scale, scale);

        if (pets && entity.isSleeping()) {
            if (cat) {
                poseStack.translate(-0.25F, entity.getBbHeight() - 1.45F, -0.25F);
                poseStack.mulPose(Axis.ZP.rotationDegrees(6.0F));
                poseStack.translate(0.0F, 0.6F, 0.0F);
                if (child) poseStack.translate(0.0F, 0.4F, 0.0F);
            } else if (fox) {
                poseStack.translate(-0.25F, entity.getBbHeight() - 0.9F, -0.25F);
                poseStack.mulPose(Axis.ZP.rotationDegrees(6.0F));
                poseStack.translate(0.0F, -0.3F, 0.0F);
            } else {
                poseStack.translate(0.0F, -0.1F, 0.0F);
            }
        }
        if (fox) poseStack.translate(renderTransform.x(), renderTransform.y(), renderTransform.z());
    }
}
