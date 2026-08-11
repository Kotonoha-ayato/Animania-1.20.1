package com.animania.client.render;

import com.animania.client.model.LegacyAnimalModel;
import com.animania.client.model.LegacyAnimationProfile;
import com.animania.client.model.LegacyPoseDefinition;
import com.animania.client.model.LegacyRenderTransform;
import com.animania.client.AnimaniaClientDiagnostics;
import com.animania.common.entity.AnimaniaAnimalEntity;
import com.mojang.blaze3d.vertex.PoseStack;
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
        this(context, layer, profile, LegacyPoseDefinition.EMPTY, LegacyRenderTransform.EMPTY, modelScale);
    }

    public AnimaniaAnimalRenderer(EntityRendererProvider.Context context, ModelLayerLocation layer,
                                  LegacyAnimationProfile profile, LegacyPoseDefinition sittingPose,
                                  float modelScale) {
        this(context, layer, profile, sittingPose, LegacyRenderTransform.EMPTY, modelScale);
    }

    public AnimaniaAnimalRenderer(EntityRendererProvider.Context context, ModelLayerLocation layer,
                                  LegacyAnimationProfile profile, LegacyPoseDefinition sittingPose,
                                  LegacyRenderTransform renderTransform, float modelScale) {
        super(context, new LegacyAnimalModel(context.bakeLayer(layer), profile, sittingPose), 0.45f);
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
        // Matches RenderDogGeneric/RenderFox: translate first, then scale.
        poseStack.translate(renderTransform.x(), renderTransform.y(), renderTransform.z());
        poseStack.scale(modelScale, modelScale, modelScale);
    }
}
