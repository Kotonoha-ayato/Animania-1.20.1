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
        hideGoatHornBudArtifacts(entity);
        if (entity.isPigAnimal()) applyPigRestPose(entity.registryPath(), showPrivateParts);
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
            if (petAnimation.active()) {
                // Exact 1.12 Cats & Dogs tail cycle.  The two low-frequency
                // sine terms create the characteristic uneven wag; the old
                // generic approximation visibly changed both phase and arc.
                float tailYaw = Mth.sin(ageInTicks * 3.141593F * 0.05F)
                        * Mth.sin(ageInTicks * 3.141593F * 0.03F * 0.05F)
                        * 0.15F * 3.141593F;
                tails.forEach(part -> part.yRot += tailYaw);
            } else if (!entity.isPigAnimal()) {
                // Pig tails are an authored curled rest-pose in the legacy
                // models, not the generic idle-tail animation.
                tails.forEach(part -> part.yRot += Mth.sin(ageInTicks * 0.12F) * 0.18F);
            }
        }
        float flap = Mth.sin(ageInTicks * 0.55F) * (0.08F + limbSwingAmount * 0.45F);
        for (int i = 0; i < wings.size(); i++) wings.get(i).zRot += (i & 1) == 0 ? flap : -flap;

        if (entity.isHamster()) setupHamsterPose(entity, ageInTicks);
        else if (entity.isSitting()) applyPose(sittingPose);
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
        if (entity.getCrowDuration() > 0 && entity.registryPath().startsWith("rooster_")) {
            ModelPart neck = child("neck");
            if (neck != null) {
                int duration = entity.getCrowDuration();
                neck.xRot = duration < 10 ? -(duration * 0.005F)
                        : duration >= 40 ? -0.5742105F + duration * 0.005F : -0.5742105F;
            }
        }
    }

    /** Applies the final positions written by ModelHamster#setLivingAnimations in 1.12. */
    private void setupHamsterPose(AnimaniaAnimalEntity entity, float ageInTicks) {
        // The Java model is deliberately centred at X=-1.5: its body cube is
        // -4.02..0.98. The generated layer lost the matching head offset,
        // which made an otherwise symmetric head look displaced.
        heads.forEach(part -> part.x = -1.5F);
        for (int i = 0; i < 5; i++) {
            if (root.hasChild("hamster_cheek_right" + i))
                root.getChild("hamster_cheek_right" + i).visible = i < entity.getHamsterFoodStack();
            if (root.hasChild("hamster_cheek_left" + i))
                root.getChild("hamster_cheek_left" + i).visible = i < entity.getHamsterFoodStack();
        }
        ModelPart body = child("hamster_body");
        ModelPart tail = child("hamster_tail");
        ModelPart backRight = child("hamster_leg_back_right");
        ModelPart backLeft = child("hamster_leg_back_left");
        ModelPart frontRight = child("hamster_leg_front_right");
        ModelPart frontLeft = child("hamster_leg_front_left");
        if (entity.isSitting()) {
            if (body != null) body.xRot = 1.0F;
            heads.forEach(part -> { part.y = 16.0F; part.z = -1.5F; });
            if (backRight != null) backRight.loadPose(net.minecraft.client.model.geom.PartPose.offsetAndRotation(-3.5F, 24.5F, 2.0F, -1.570796F, 0.8F, 0.0F));
            if (backLeft != null) backLeft.loadPose(net.minecraft.client.model.geom.PartPose.offsetAndRotation(2.5F, 24.5F, 3.5F, -1.570796F, -0.8F, 0.0F));
            if (frontRight != null) frontRight.setPos(-2.0F, 21.0F, -0.5F);
            if (frontLeft != null) frontLeft.setPos(2.0F, 21.0F, -0.5F);
            if (tail != null) tail.setPos(0.0F, 17.0F, 2.0F);
        } else if (entity.isHamsterStanding()) {
            heads.forEach(part -> { part.y = 9.6F; part.z = 4.5F; });
            if (body != null) { body.setPos(0.0F, 15.1F, 4.5F); body.xRot = Mth.cos(80.0F * Mth.DEG_TO_RAD); }
            if (backRight != null) backRight.setPos(-2.0F, 20.6F, 6.0F);
            if (backLeft != null) backLeft.setPos(2.0F, 20.6F, 6.0F);
            if (frontRight != null) frontRight.loadPose(net.minecraft.client.model.geom.PartPose.offsetAndRotation(-2.0F, 14.6F, 3.0F, Mth.cos(150.0F * Mth.DEG_TO_RAD), Mth.sin(-10.0F * Mth.DEG_TO_RAD), 0.0F));
            if (frontLeft != null) frontLeft.loadPose(net.minecraft.client.model.geom.PartPose.offsetAndRotation(2.0F, 14.6F, 3.0F, Mth.cos(150.0F * Mth.DEG_TO_RAD), Mth.sin(10.0F * Mth.DEG_TO_RAD), 0.0F));
            if (tail != null) tail.setPos(0.0F, 14.6F, 2.0F);
        }
        if (tail != null) {
            tail.xRot = 1.570796F;
            tail.zRot = entity.isTamed()
                    ? Mth.sin(ageInTicks * 3.141593F * 0.05F) * Mth.sin(ageInTicks * 3.141593F * 11.0F * 0.05F) * 0.15F * 3.141593F
                    : 0.0F;
        }
    }

    /**
     * Restores the constant pose that the 1.12 pig Java models assigned from
     * {@code setRotationAngles}.  It is deliberately applied after resetPose:
     * those models did not store this pose in their constructors, so baking
     * the geometry alone leaves every pig body upright.
     */
    private void applyPigRestPose(String id, boolean showPrivateParts) {
        boolean piglet = id.startsWith("piglet_");
        boolean largeBlack = id.endsWith("large_black");
        setRotation(child("body"), Mth.HALF_PI, 0.0F, 0.0F);

        float earX = largeBlack ? 0.5235987F : -0.2617994F;
        float earY = largeBlack ? 0.5235987F : 0.3490658F;
        float earZ = largeBlack ? 0.8726646F : 0.6981317F;
        for (String name : new String[]{"ear1", "ear1a", "ear1b"})
            setRotation(child("head/" + name), earX, earY, earZ);
        for (String name : new String[]{"ear2", "ear2a", "ear2b"})
            setRotation(child("head/" + name), earX, -earY, -earZ);

        if (piglet) {
            setRotation(child("tail1/tail1a"), 1.5F, 1.5F, 0.0F);
            return;
        }

        setRotation(child("tail1"), 0.1409582F, 0.2046205F, 0.0F);
        setRotation(child("tail1/tail1a"), 1.429837F, -2.936972F, -Mth.PI);
        for (int number = 1; number <= 6; number++)
            setRotation(child("nipple" + number), Mth.HALF_PI, 0.0F, 0.0F);
        if (showPrivateParts) setRotation(child("block_a"), 0.2617994F, 0.0F, 0.0F);
    }

    /**
     * The 1.12 goat models used two tiny dark horn-bud cubes as editor aids.
     * In a ModelPart layer their 3x1x3 faces become visible as a floating
     * black pixel above every goat's head.  Horns remain separate geometry;
     * only these shared artefact nodes are suppressed.
     */
    private void hideGoatHornBudArtifacts(AnimaniaAnimalEntity entity) {
        String id = entity.registryPath();
        if (!id.startsWith("buck_") && !id.startsWith("doe_") && !id.startsWith("kid_")) return;
        hide(child("head_node/bud__r"));
        hide(child("head_node/bud__l"));
    }

    private ModelPart child(String path) {
        ModelPart current = root;
        for (String segment : path.split("/")) {
            if (!current.hasChild(segment)) return null;
            current = current.getChild(segment);
        }
        return current;
    }

    private static void setRotation(ModelPart part, float xRot, float yRot, float zRot) {
        if (part == null) return;
        part.xRot = xRot;
        part.yRot = yRot;
        part.zRot = zRot;
    }

    private static void hide(ModelPart part) {
        if (part != null) part.visible = false;
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
