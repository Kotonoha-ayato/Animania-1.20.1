package com.animania.client.model;

import com.animania.common.entity.AnimaniaAnimalEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

/** Native ModelPart model shared by all variant registrations. */
public class AnimaniaAnimalModel extends HierarchicalModel<AnimaniaAnimalEntity> {
    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart legFrontLeft;
    private final ModelPart legFrontRight;
    private final ModelPart legBackLeft;
    private final ModelPart legBackRight;
    private final ModelPart tail;
    private final ModelPart earLeft;
    private final ModelPart earRight;
    private final ModelPart muzzle;
    private final ModelPart hornLeft;
    private final ModelPart hornRight;
    private final ModelPart wingLeft;
    private final ModelPart wingRight;

    public AnimaniaAnimalModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.head = root.getChild("head");
        this.legFrontLeft = root.getChild("leg_front_left");
        this.legFrontRight = root.getChild("leg_front_right");
        this.legBackLeft = root.getChild("leg_back_left");
        this.legBackRight = root.getChild("leg_back_right");
        this.tail = root.getChild("tail");
        this.earLeft = root.getChild("ear_left");
        this.earRight = root.getChild("ear_right");
        this.muzzle = root.getChild("muzzle");
        this.hornLeft = root.getChild("horn_left");
        this.hornRight = root.getChild("horn_right");
        this.wingLeft = root.getChild("wing_left");
        this.wingRight = root.getChild("wing_right");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-4, -3, -6, 8, 6, 12), PartPose.offset(0, 14, 0));
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 18).addBox(-3, -3, -4, 6, 6, 6), PartPose.offset(0, 10, -6));
        root.addOrReplaceChild("leg_front_left", CubeListBuilder.create().texOffs(0, 30).addBox(-1, 0, -1, 2, 6, 2), PartPose.offset(3, 17, -4));
        root.addOrReplaceChild("leg_front_right", CubeListBuilder.create().texOffs(8, 30).addBox(-1, 0, -1, 2, 6, 2), PartPose.offset(-3, 17, -4));
        root.addOrReplaceChild("leg_back_left", CubeListBuilder.create().texOffs(16, 30).addBox(-1, 0, -1, 2, 6, 2), PartPose.offset(3, 17, 4));
        root.addOrReplaceChild("leg_back_right", CubeListBuilder.create().texOffs(24, 30).addBox(-1, 0, -1, 2, 6, 2), PartPose.offset(-3, 17, 4));
        // The old breeds used distinct ears, muzzles, horns, tails and wings.
        // Keep these as native ModelPart branches and toggle the silhouette
        // from the stable registry ID instead of using a model library.
        root.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(32, 0).addBox(-1, -1, 0, 2, 2, 6), PartPose.offset(0, 13, 6));
        root.addOrReplaceChild("ear_left", CubeListBuilder.create().texOffs(32, 18).addBox(-1, -3, -1, 2, 3, 2), PartPose.offset(3, 8, -6));
        root.addOrReplaceChild("ear_right", CubeListBuilder.create().texOffs(40, 18).addBox(-1, -3, -1, 2, 3, 2), PartPose.offset(-3, 8, -6));
        root.addOrReplaceChild("muzzle", CubeListBuilder.create().texOffs(32, 24).addBox(-2, -2, -3, 4, 3, 3), PartPose.offset(0, 11, -8));
        root.addOrReplaceChild("horn_left", CubeListBuilder.create().texOffs(48, 0).addBox(-1, -3, -1, 2, 3, 2), PartPose.offset(2, 7, -5));
        root.addOrReplaceChild("horn_right", CubeListBuilder.create().texOffs(56, 0).addBox(-1, -3, -1, 2, 3, 2), PartPose.offset(-2, 7, -5));
        root.addOrReplaceChild("wing_left", CubeListBuilder.create().texOffs(32, 30).addBox(0, 0, -3, 4, 5, 1), PartPose.offset(4, 11, 0));
        root.addOrReplaceChild("wing_right", CubeListBuilder.create().texOffs(42, 30).addBox(-4, 0, -3, 4, 5, 1), PartPose.offset(-4, 11, 0));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(AnimaniaAnimalEntity animal, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        head.yRot = netHeadYaw * ((float) Math.PI / 180F);
        head.xRot = headPitch * ((float) Math.PI / 180F);
        configureSilhouette(animal, ageInTicks, limbSwingAmount);
        if (animal.getEatingTicks() > 0) {
            applyStatic(AnimaniaAnimations.EAT);
        } else if (animal.isSleeping()) {
            applyStatic(AnimaniaAnimations.SLEEP);
        } else if (animal.getPlayGoal() != null && animal.isPlaying()) {
            applyStatic(AnimaniaAnimations.PLAY);
        } else if (animal.isInLove()) {
            applyStatic(AnimaniaAnimations.BREED);
        } else if (animal.getThirst() < 25) {
            applyStatic(AnimaniaAnimations.DRINK);
        } else if (animal.getHunger() < 25) {
            applyStatic(AnimaniaAnimations.EAT);
        } else {
            animateWalk(limbSwingAmount > 0.75F ? AnimaniaAnimations.RUN : AnimaniaAnimations.WALK,
                    limbSwing, limbSwingAmount, 2.0F, 2.5F);
        }
    }

    private void configureSilhouette(AnimaniaAnimalEntity animal, float ageInTicks, float limbSwingAmount) {
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(animal.getType());
        String path = id == null ? "" : id.getPath();
        boolean companion = id != null && "animania_catsdogs".equals(id.getNamespace());
        boolean bird = path.startsWith("hen_") || path.startsWith("rooster_") || path.startsWith("chick_")
                || path.startsWith("peahen_") || path.startsWith("peacock_") || path.startsWith("peachick_");
        boolean rabbit = path.startsWith("doe_") || path.startsWith("buck_") || path.startsWith("kit_");
        boolean rodent = path.equals("hamster") || path.startsWith("ferret_") || path.startsWith("hedgehog");
        tail.visible = !path.equals("toad") && !path.equals("frog") && !path.equals("dartfrog");
        earLeft.visible = companion || rabbit || rodent;
        earRight.visible = earLeft.visible;
        muzzle.visible = companion || path.startsWith("cow_") || path.startsWith("bull_") || path.startsWith("calf_")
                || path.startsWith("pig") || path.startsWith("sow_") || path.startsWith("hog_")
                || path.startsWith("goat") || path.startsWith("doe_") || path.startsWith("buck_")
                || path.startsWith("ewe_") || path.startsWith("ram_") || path.startsWith("lamb_")
                || path.startsWith("mare_") || path.startsWith("stallion_") || path.startsWith("foal_");
        hornLeft.visible = path.startsWith("bull_") || path.startsWith("buck_") || path.startsWith("goat") || path.startsWith("ram_");
        hornRight.visible = hornLeft.visible;
        wingLeft.visible = bird;
        wingRight.visible = bird;
        if (tail.visible) tail.yRot = (float) Math.sin(ageInTicks * 0.12F) * 0.12F;
        if (wingLeft.visible) {
            float flap = (float) Math.sin(ageInTicks * 0.35F) * Math.min(0.45F, limbSwingAmount * 0.25F);
            wingLeft.zRot = flap;
            wingRight.zRot = -flap;
        }
    }
}
