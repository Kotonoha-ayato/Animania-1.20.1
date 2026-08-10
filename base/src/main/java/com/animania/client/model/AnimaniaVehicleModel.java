package com.animania.client.model;

import com.animania.common.entity.AnimaniaVehicleEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/** Native ModelPart prop model for cart, wagon and tiller. */
public final class AnimaniaVehicleModel extends HierarchicalModel<AnimaniaVehicleEntity> {
    private final ModelPart root;
    private final ModelPart body;

    public AnimaniaVehicleModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-7, -3, -4, 14, 4, 8), PartPose.offset(0, 18, 0));
        root.addOrReplaceChild("wheel_left", CubeListBuilder.create().texOffs(0, 12).addBox(-1, -3, -1, 2, 6, 2), PartPose.offset(7, 19, 0));
        root.addOrReplaceChild("wheel_right", CubeListBuilder.create().texOffs(8, 12).addBox(-1, -3, -1, 2, 6, 2), PartPose.offset(-7, 19, 0));
        return LayerDefinition.create(mesh, 32, 32);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(AnimaniaVehicleEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        body.yRot = -entity.getYRot() * ((float) Math.PI / 180F);
    }
}
