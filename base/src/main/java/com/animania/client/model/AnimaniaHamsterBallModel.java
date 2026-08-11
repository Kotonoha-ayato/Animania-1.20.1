package com.animania.client.model;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.resources.ResourceLocation;

/** Native ModelPart cage matching the 1.12 hamster-ball silhouette. */
public final class AnimaniaHamsterBallModel {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath("animania", "hamster_ball"), "main");

    private AnimaniaHamsterBallModel() { }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        CubeListBuilder cage = CubeListBuilder.create().texOffs(0, 0)
                .addBox(-5.0F, -1.0F, -5.0F, 10.0F, 1.0F, 10.0F)
                .addBox(-5.0F, -6.0F, -5.0F, 1.0F, 8.0F, 10.0F)
                .addBox(4.0F, -6.0F, -5.0F, 1.0F, 8.0F, 10.0F)
                .addBox(-4.0F, -5.0F, -6.0F, 8.0F, 8.0F, 1.0F)
                .addBox(-4.0F, -4.0F, 5.0F, 8.0F, 6.0F, 1.0F)
                .addBox(-3.0F, -7.0F, -4.0F, 6.0F, 1.0F, 8.0F)
                .addBox(-3.0F, 3.0F, -4.0F, 6.0F, 1.0F, 8.0F)
                .addBox(-6.0F, -5.0F, -4.0F, 1.0F, 8.0F, 8.0F)
                .addBox(5.0F, -5.0F, -4.0F, 1.0F, 8.0F, 8.0F)
                .addBox(-3.0F, -8.0F, -3.0F, 6.0F, 1.0F, 6.0F)
                .addBox(-3.0F, 4.0F, -3.0F, 6.0F, 1.0F, 6.0F);
        root.addOrReplaceChild("ball", cage, PartPose.offset(0.0F, 5.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 32);
    }
}
