package com.animania.extra.client.model;

// Generated from archived LGPL-3.0 legacy native JSON; no legacy native runtime dependency.
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;

public final class ExtraNativeModelLayers {
    public static final Map<String, ModelLayerLocation> LAYERS = new LinkedHashMap<>();
    static {
        LAYERS.put("model_hamster_wheel", new ModelLayerLocation(new ResourceLocation("animania_extra", "native/model_hamster_wheel"), "main"));
        LAYERS.put("hamster", new ModelLayerLocation(new ResourceLocation("animania_extra", "native/hamster"), "main"));
    }
    private ExtraNativeModelLayers() {}
    public static LayerDefinition create(String id) {
        return switch (id) {
            case "model_hamster_wheel" -> model_hamster_wheel();
            case "hamster" -> hamster();
            default -> throw new IllegalArgumentException("Unknown legacy native model " + id);
        };
    }
    private static LayerDefinition model_hamster_wheel() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition base1 = root.addOrReplaceChild("base1", CubeListBuilder.create().texOffs(12, 1).addBox(-1.0F, 0.0F, -0.5F, 1.0F, 13.0F, 1.0F), PartPose.offsetAndRotation(6.0F, 24.0F, 6.0F, 0.0F, 0.0F, 1.570796F));
        PartDefinition base2 = base1.addOrReplaceChild("base2", CubeListBuilder.create().texOffs(12, 1).addBox(-0.5F, -0.5F, -0.5F, 1.0F, 13.0F, 1.0F), PartPose.offsetAndRotation(-10.5F, -0.5F, 0.0F, 0.0F, 0.0F, -2.094395F));
        PartDefinition axel1 = base1.addOrReplaceChild("axel1", CubeListBuilder.create().texOffs(12, 1).addBox(-0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(-10.5F, -0.5F, -1.0F, 0.0F, 0.0F, -2.094395F));
        PartDefinition base3 = base1.addOrReplaceChild("base3", CubeListBuilder.create().texOffs(12, 1).addBox(-1.0F, 0.0F, -0.5F, 1.0F, 13.0F, 1.0F), PartPose.offsetAndRotation(0.5F, -6.499999F, -12.0F, 0.0F, 0.0F, -0.0F));
        PartDefinition base4 = base3.addOrReplaceChild("base4", CubeListBuilder.create().texOffs(12, 1).addBox(-0.5F, -0.5F, -0.5F, 1.0F, 13.0F, 1.0F), PartPose.offsetAndRotation(-10.5F, -0.5F, 0.0F, 0.0F, 0.0F, -2.094395F));
        PartDefinition axel12 = base3.addOrReplaceChild("axel12", CubeListBuilder.create().texOffs(12, 1).addBox(-0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(-10.5F, -0.499998F, 1.0F, 0.0F, 0.0F, -2.094395F));
        PartDefinition base5 = base1.addOrReplaceChild("base5", CubeListBuilder.create().texOffs(4, 13).addBox(-0.5F, -0.5F, -6.0F, 1.0F, 1.0F, 12.0F), PartPose.offsetAndRotation(-0.000001F, 6.000001F, -6.0F, 0.0F, 0.0F, -1.570797F));
        PartDefinition wheel1 = base1.addOrReplaceChild("wheel1", CubeListBuilder.create().texOffs(19, 2).addBox(-3.0F, -0.5F, -5.0F, 6.0F, 1.0F, 10.0F), PartPose.offsetAndRotation(-17.0F, -0.499997F, -6.0F, 0.0F, 0.0F, -1.570797F));
        PartDefinition wheel2 = wheel1.addOrReplaceChild("wheel2", CubeListBuilder.create().texOffs(19, 2).addBox(0.0F, 0.0F, -5.0F, 6.0F, 1.0F, 10.0F), PartPose.offsetAndRotation(3.0F, -0.5F, 0.0F, 0.0F, 0.0F, 0.785398F));
        PartDefinition wheel3 = wheel2.addOrReplaceChild("wheel3", CubeListBuilder.create().texOffs(19, 2).addBox(0.0F, 0.0F, -5.0F, 6.0F, 1.0F, 10.0F), PartPose.offsetAndRotation(3.0F, -0.5F, 0.0F, 0.0F, 0.0F, 0.785398F));
        PartDefinition wheel4 = wheel3.addOrReplaceChild("wheel4", CubeListBuilder.create().texOffs(19, 2).addBox(0.0F, 0.0F, -5.0F, 6.0F, 1.0F, 10.0F), PartPose.offsetAndRotation(3.0F, -0.5F, 0.0F, 0.0F, 0.0F, 0.785398F));
        PartDefinition wheel5 = wheel4.addOrReplaceChild("wheel5", CubeListBuilder.create().texOffs(19, 2).addBox(0.0F, 0.0F, -5.0F, 6.0F, 1.0F, 10.0F), PartPose.offsetAndRotation(3.0F, -0.5F, 0.0F, 0.0F, 0.0F, 0.785398F));
        PartDefinition wheel6 = wheel5.addOrReplaceChild("wheel6", CubeListBuilder.create().texOffs(19, 2).addBox(0.0F, 0.0F, -5.0F, 6.0F, 1.0F, 10.0F), PartPose.offsetAndRotation(3.0F, -0.5F, 0.0F, 0.0F, 0.0F, 0.785398F));
        PartDefinition wheel7 = wheel6.addOrReplaceChild("wheel7", CubeListBuilder.create().texOffs(19, 2).addBox(0.0F, 0.0F, -5.0F, 6.0F, 1.0F, 10.0F), PartPose.offsetAndRotation(3.0F, -0.5F, 0.0F, 0.0F, 0.0F, 0.785398F));
        PartDefinition wheel8 = wheel7.addOrReplaceChild("wheel8", CubeListBuilder.create().texOffs(19, 2).addBox(0.0F, 0.0F, -5.0F, 6.0F, 1.0F, 10.0F), PartPose.offsetAndRotation(3.0F, -0.5F, 0.0F, 0.0F, 0.0F, 0.785398F));
        PartDefinition stick = wheel1.addOrReplaceChild("stick", CubeListBuilder.create().texOffs(13, 2).addBox(-0.5F, -6.5F, 0.0F, 1.0F, 13.0F, 0.0F), PartPose.offsetAndRotation(-0.000001F, 6.5F, 4.75F, 0.0F, 0.0F, 0.0F));
        PartDefinition stick2 = stick.addOrReplaceChild("stick2", CubeListBuilder.create().texOffs(13, 2).addBox(-0.5F, -6.5F, 0.0F, 1.0F, 13.0F, 0.0F), PartPose.offsetAndRotation(-0.000001F, 0.0F, -9.5F, 0.0F, 0.0F, 0.0F));
        PartDefinition axel1b = base1.addOrReplaceChild("axel1b", CubeListBuilder.create().texOffs(0, 0).addBox(-0.5F, -0.5F, -1.0F, 1.0F, 1.0F, 2.0F), PartPose.offsetAndRotation(-10.5F, -0.5F, 1.0F, 0.0F, 0.0F, -2.094395F));
        return LayerDefinition.create(mesh, 64, 32);
    }
    private static LayerDefinition hamster() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition hamster_body = root.addOrReplaceChild("hamster_body", CubeListBuilder.create().texOffs(28, 8).addBox(-2.5F, -4.0F, -2.5F, 5.0F, 8.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 20.0F, 0.0F, 1.570796F, 0.0F, 0.0F));
        PartDefinition hamster_ear_right = hamster_body.addOrReplaceChild("hamster_ear_right", CubeListBuilder.create().texOffs(10, 15).addBox(-0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(1.5F, -5.0F, 3.749999F, -1.570796F, 0.0F, 0.0F));
        PartDefinition hamster_ear_left = hamster_ear_right.addOrReplaceChild("hamster_ear_left", CubeListBuilder.create().texOffs(10, 18).addBox(-0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(-3.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition tail = hamster_ear_right.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(10, 15).addBox(-0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(-1.5F, 4.0F, 9.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition hamsterleg1 = hamster_body.addOrReplaceChild("hamsterleg1", CubeListBuilder.create().texOffs(0, 16).addBox(-0.5F, -1.0F, -0.5F, 1.0F, 2.0F, 1.0F), PartPose.offsetAndRotation(-1.25F, -2.75F, -3.000001F, -1.570796F, 0.0F, 0.0F));
        PartDefinition hamsterleg2 = hamsterleg1.addOrReplaceChild("hamsterleg2", CubeListBuilder.create().texOffs(0, 16).addBox(-0.5F, -1.0F, -0.5F, 1.0F, 2.0F, 1.0F), PartPose.offsetAndRotation(2.5F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition hamster_head = hamster_body.addOrReplaceChild("hamster_head", CubeListBuilder.create().texOffs(0, 0).addBox(-2.5F, -2.5F, -2.5F, 5.0F, 5.0F, 5.0F), PartPose.offsetAndRotation(0.0F, -4.5F, 0.999999F, -1.570796F, 0.0F, 0.0F));
        PartDefinition hamster_nose = hamster_head.addOrReplaceChild("hamster_nose", CubeListBuilder.create().texOffs(0, 25).addBox(-1.5F, -1.0F, -0.5F, 3.0F, 2.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 1.0F, -2.1F, 0.0F, 0.0F, 0.0F));
        PartDefinition hamsterleg3 = hamster_body.addOrReplaceChild("hamsterleg3", CubeListBuilder.create().texOffs(0, 16).addBox(-0.5F, -1.0F, -0.5F, 1.0F, 2.0F, 1.0F), PartPose.offsetAndRotation(-1.25F, 2.5F, -3.0F, -1.570796F, 0.0F, 0.0F));
        PartDefinition hamsterleg4 = hamsterleg3.addOrReplaceChild("hamsterleg4", CubeListBuilder.create().texOffs(0, 16).addBox(-0.5F, -1.0F, -0.5F, 1.0F, 2.0F, 1.0F), PartPose.offsetAndRotation(2.5F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 32);
    }
}
