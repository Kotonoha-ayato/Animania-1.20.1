package com.animania.client.model;

import com.animania.common.entity.AnimaniaAnimalEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.item.DyeColor;

import java.util.ArrayList;
import java.util.List;

/** Runtime wrapper for the breed-specific native layers converted from 1.12. */
public final class LegacyAnimalModel extends HierarchicalModel<AnimaniaAnimalEntity> {
    private final ModelPart root;
    private final List<ModelPart> heads;
    private final List<ModelPart> leftLegs;
    private final List<ModelPart> rightLegs;
    private final List<ModelPart> tails;
    private final List<ModelPart> wings;
    private final List<ModelPart> bodies;
    private final List<ModelPart> privateParts;
    private final List<ModelPart> coloredParts;
    private final List<ResolvedPose> sittingPose;
    private final List<ResolvedPose> sleepingPose;
    private final ModelPart petLookPart;
    private final LegacyPetAnimationDefinition petAnimation;
    private float woolRed = 1.0F;
    private float woolGreen = 1.0F;
    private float woolBlue = 1.0F;

    public LegacyAnimalModel(ModelPart root, LegacyAnimationProfile profile) {
        this(root, profile, LegacyPoseDefinition.EMPTY, LegacyPetAnimationDefinition.EMPTY);
    }

    public LegacyAnimalModel(ModelPart root, LegacyAnimationProfile profile, LegacyPoseDefinition sittingPose) {
        this(root, profile, sittingPose, LegacyPetAnimationDefinition.EMPTY);
    }

    public LegacyAnimalModel(ModelPart root, LegacyAnimationProfile profile, LegacyPoseDefinition sittingPose,
                             LegacyPetAnimationDefinition petAnimation) {
        this.root = root;
        this.heads = resolve(root, profile.heads());
        this.leftLegs = resolve(root, profile.leftLegs());
        this.rightLegs = resolve(root, profile.rightLegs());
        this.tails = resolve(root, profile.tails());
        this.wings = resolve(root, profile.wings());
        this.bodies = resolve(root, profile.bodies());
        this.privateParts = resolve(root, profile.privateParts());
        this.coloredParts = resolve(root, profile.coloredParts());
        this.sittingPose = resolvePose(root, sittingPose);
        this.sleepingPose = resolvePose(root, petAnimation.sleepingPose());
        List<ModelPart> look = resolve(root, new String[]{petAnimation.lookPart()});
        this.petLookPart = look.isEmpty() ? null : look.get(0);
        this.petAnimation = petAnimation;
    }

    @Override
    public ModelPart root() {
        return root;
    }

    public void translatePrimaryHead(PoseStack poseStack) {
        if (!heads.isEmpty()) heads.get(0).translateAndRotate(poseStack);
    }

    @Override
    public void setupAnim(AnimaniaAnimalEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        float[] wool = DyeColor.byId(entity.getWoolColor()).getTextureDiffuseColors();
        woolRed = wool[0];
        woolGreen = wool[1];
        woolBlue = wool[2];
        boolean showPrivateParts;
        try {
            showPrivateParts = com.animania.common.config.AnimaniaConfig.SHOW_PARTS.get();
        } catch (IllegalStateException ignored) {
            showPrivateParts = false;
        }
        for (ModelPart part : privateParts) part.visible = showPrivateParts;
        if (petAnimation.active() && petLookPart != null && !entity.isSleeping()
                && (petAnimation.lookWhileSitting() || !entity.isSitting())) {
            petLookPart.xRot = headPitch * petAnimation.pitchScale() + petAnimation.pitchOffset();
            petLookPart.yRot = netHeadYaw * petAnimation.yawScale();
        } else if (!petAnimation.active()) {
            float headX = headPitch * Mth.DEG_TO_RAD;
            float headY = netHeadYaw * Mth.DEG_TO_RAD;
            heads.forEach(part -> { part.xRot += headX; part.yRot += headY; });
        }

        if (!entity.isSleeping()) {
            float stride = Mth.cos(limbSwing * 0.6662F) * petAnimation.strideScale() * limbSwingAmount;
            leftLegs.forEach(part -> part.xRot += stride);
            rightLegs.forEach(part -> part.xRot -= stride);
            tails.forEach(part -> part.yRot += Mth.sin(ageInTicks * 0.12F) * 0.18F);
        }
        float flap = Mth.sin(ageInTicks * 0.55F) * (0.08F + limbSwingAmount * 0.45F);
        for (int i = 0; i < wings.size(); i++) wings.get(i).zRot += (i & 1) == 0 ? flap : -flap;

        if (entity.isSitting()) applyPose(sittingPose);
        else if (entity.isSleeping() && petAnimation.active()) applyPose(sleepingPose);

        if (entity.getEatingTicks() > 0) {
            heads.forEach(part -> part.xRot += 0.9F);
        } else if (entity.isSpooked()) {
            // Fainting goats collapse sideways for the one-second legacy
            // collision timer; the state is synchronized from the server.
            bodies.forEach(part -> part.zRot += 1.25F);
            heads.forEach(part -> part.zRot += 0.25F);
        } else if (entity.isFighting()) {
            heads.forEach(part -> part.xRot -= 0.35F);
            bodies.forEach(part -> part.xRot += 0.08F);
        } else if (entity.isSleeping() && !petAnimation.active()) {
            bodies.forEach(part -> part.zRot += 0.12F);
            heads.forEach(part -> part.xRot += 0.35F);
        } else if (entity.getPlayGoal() != null && entity.isPlaying()) {
            bodies.forEach(part -> part.y += Mth.sin(ageInTicks * 0.7F) * 0.8F);
            tails.forEach(part -> part.yRot += Mth.sin(ageInTicks * 0.8F) * 0.45F);
        } else if (entity.isInLove()) {
            tails.forEach(part -> part.yRot += Mth.sin(ageInTicks * 0.9F) * 0.55F);
        } else if (entity.getThirst() < 25) {
            heads.forEach(part -> part.xRot += 0.75F + Mth.sin(ageInTicks * 0.18F) * 0.08F);
        } else if (entity.getHunger() < 25) {
            heads.forEach(part -> part.xRot += 0.35F + Mth.sin(ageInTicks * 0.45F) * 0.12F);
        }
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer consumer, int packedLight, int packedOverlay,
                               float red, float green, float blue, float alpha) {
        if (coloredParts.isEmpty()) {
            root.render(pose, consumer, packedLight, packedOverlay, red, green, blue, alpha);
            return;
        }
        try {
            coloredParts.forEach(part -> part.skipDraw = true);
            root.render(pose, consumer, packedLight, packedOverlay, red, green, blue, alpha);
            root.getAllParts().forEach(part -> part.skipDraw = true);
            coloredParts.forEach(part -> part.skipDraw = false);
            root.render(pose, consumer, packedLight, packedOverlay,
                    red * woolRed, green * woolGreen, blue * woolBlue, alpha);
        } finally {
            root.getAllParts().forEach(part -> part.skipDraw = false);
        }
    }

    private static List<ModelPart> resolve(ModelPart root, String[] paths) {
        List<ModelPart> result = new ArrayList<>(paths.length);
        for (String path : paths) {
            ModelPart current = root;
            boolean valid = true;
            for (String segment : path.split("/")) {
                if (!current.hasChild(segment)) { valid = false; break; }
                current = current.getChild(segment);
            }
            if (valid) result.add(current);
        }
        return result;
    }

    private static List<ResolvedPose> resolvePose(ModelPart root, LegacyPoseDefinition definition) {
        LegacyPartPose[] definitions = definition.parts();
        List<ResolvedPose> result = new ArrayList<>(definitions.length);
        for (LegacyPartPose pose : definitions) {
            List<ModelPart> resolved = resolve(root, new String[]{pose.path()});
            if (!resolved.isEmpty()) result.add(new ResolvedPose(resolved.get(0), pose));
        }
        return result;
    }

    private static void applyPose(List<ResolvedPose> poses) {
        for (ResolvedPose resolved : poses) {
            ModelPart part = resolved.part();
            LegacyPartPose pose = resolved.pose();
            if (Float.isFinite(pose.x())) part.x = pose.x();
            if (Float.isFinite(pose.y())) part.y = pose.y();
            if (Float.isFinite(pose.z())) part.z = pose.z();
            if (Float.isFinite(pose.xRot())) part.xRot = pose.xRot();
            if (Float.isFinite(pose.yRot())) part.yRot = pose.yRot();
            if (Float.isFinite(pose.zRot())) part.zRot = pose.zRot();
        }
    }

    private record ResolvedPose(ModelPart part, LegacyPartPose pose) {}
}
