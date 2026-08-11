package com.animania.client.model;

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

public final class BaseNativeModelLayers {
    public static final Map<String, ModelLayerLocation> LAYERS = new LinkedHashMap<>();
    static {
        LAYERS.put("player", new ModelLayerLocation(new ResourceLocation("animania", "native/player"), "main"));
        LAYERS.put("player_sit", new ModelLayerLocation(new ResourceLocation("animania", "native/player_sit"), "main"));
    }
    private BaseNativeModelLayers() {}
    public static LayerDefinition create(String id) {
        return switch (id) {
            case "player" -> player();
            case "player_sit" -> player_sit();
            default -> throw new IllegalArgumentException("Unknown legacy native model " + id);
        };
    }
    private static LayerDefinition player() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition pelvis = root.addOrReplaceChild("pelvis", CubeListBuilder.create().texOffs(16, 24).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 4.0F, 4.0F), PartPose.offsetAndRotation(0.0F, 8.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition spine = pelvis.addOrReplaceChild("spine", CubeListBuilder.create().texOffs(16, 20).addBox(-4.0F, -4.0F, -3.0F, 8.0F, 4.0F, 4.0F), PartPose.offsetAndRotation(0.0F, -2.0F, 1.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition chest = spine.addOrReplaceChild("chest", CubeListBuilder.create().texOffs(16, 16).addBox(-4.0F, -6.0F, -3.0F, 8.0F, 4.0F, 4.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition neck = chest.addOrReplaceChild("neck", CubeListBuilder.create().texOffs(15, -1).addBox(-2.0F, -0.5F, -1.5F, 4.0F, 1.0F, 3.0F), PartPose.offsetAndRotation(0.0F, -2.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition head = neck.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F), PartPose.offsetAndRotation(0.0F, -0.5F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition headwear = head.addOrReplaceChild("headwear", CubeListBuilder.create().texOffs(32, 0).addBox(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition left_ear = head.addOrReplaceChild("left_ear", CubeListBuilder.create().texOffs(24, 0).addBox(-3.0F, -3.0F, -0.5F, 6.0F, 6.0F, 1.0F), PartPose.offsetAndRotation(6.0F, -6.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition right_ear = head.addOrReplaceChild("right_ear", CubeListBuilder.create().texOffs(24, 0).addBox(-3.0F, -3.0F, -0.5F, 6.0F, 6.0F, 1.0F), PartPose.offsetAndRotation(-6.0F, -6.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition upper__arm__right = chest.addOrReplaceChild("upper__arm__right", CubeListBuilder.create().texOffs(40, 16).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 4.0F, 4.0F), PartPose.offsetAndRotation(-5.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition elbow__right = upper__arm__right.addOrReplaceChild("elbow__right", CubeListBuilder.create().texOffs(40, 20).addBox(-2.0F, 0.0F, -4.0F, 4.0F, 2.0F, 4.0F), PartPose.offsetAndRotation(0.0F, 2.0F, 2.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition forearm__right = elbow__right.addOrReplaceChild("forearm__right", CubeListBuilder.create().texOffs(40, 21).addBox(-2.0F, 0.0F, -4.0F, 4.0F, 4.0F, 4.0F), PartPose.offsetAndRotation(0.0F, 1.0F, 2.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition hand__right = forearm__right.addOrReplaceChild("hand__right", CubeListBuilder.create().texOffs(40, 32).addBox(-2.0F, 0.5F, -2.0F, 4.0F, 3.0F, 4.0F), PartPose.offsetAndRotation(0.0F, 1.5F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition upper__arm__left = chest.addOrReplaceChild("upper__arm__left", CubeListBuilder.create().texOffs(40, 16).addBox(-1.0F, -2.0F, -2.0F, 4.0F, 4.0F, 4.0F), PartPose.offsetAndRotation(5.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition elbow__left = upper__arm__left.addOrReplaceChild("elbow__left", CubeListBuilder.create().texOffs(40, 20).addBox(-2.0F, 0.0F, -4.0F, 4.0F, 2.0F, 4.0F), PartPose.offsetAndRotation(0.0F, 2.0F, 2.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition forearm__left = elbow__left.addOrReplaceChild("forearm__left", CubeListBuilder.create().texOffs(40, 21).addBox(-2.0F, 0.0F, -4.0F, 4.0F, 4.0F, 4.0F), PartPose.offsetAndRotation(0.0F, 1.0F, 2.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition hand__left = forearm__left.addOrReplaceChild("hand__left", CubeListBuilder.create().texOffs(40, 32).addBox(-2.0F, 0.5F, -2.0F, 4.0F, 3.0F, 4.0F), PartPose.offsetAndRotation(0.0F, 1.5F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition upper__leg__right = pelvis.addOrReplaceChild("upper__leg__right", CubeListBuilder.create().texOffs(0, 16).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F), PartPose.offsetAndRotation(-2.0F, 2.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition knee__right = upper__leg__right.addOrReplaceChild("knee__right", CubeListBuilder.create().texOffs(0, 22).addBox(-2.0F, 0.0F, 0.0F, 4.0F, 2.0F, 4.0F), PartPose.offsetAndRotation(0.0F, 3.0F, -2.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition lower__leg__right = knee__right.addOrReplaceChild("lower__leg__right", CubeListBuilder.create().texOffs(0, 24).addBox(-2.0F, 0.0F, 0.0F, 4.0F, 4.0F, 4.0F), PartPose.offsetAndRotation(0.0F, 1.0F, -2.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition upper__leg__left = pelvis.addOrReplaceChild("upper__leg__left", CubeListBuilder.create().texOffs(0, 16).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F), PartPose.offsetAndRotation(2.0F, 2.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition knee__left = upper__leg__left.addOrReplaceChild("knee__left", CubeListBuilder.create().texOffs(0, 22).addBox(-2.0F, 0.0F, 0.0F, 4.0F, 2.0F, 4.0F), PartPose.offsetAndRotation(0.0F, 3.0F, -2.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition lower__leg__left = knee__left.addOrReplaceChild("lower__leg__left", CubeListBuilder.create().texOffs(0, 24).addBox(-2.0F, 0.0F, 0.0F, 4.0F, 4.0F, 4.0F), PartPose.offsetAndRotation(0.0F, 1.0F, -2.0F, 0.0F, 0.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }
    private static LayerDefinition player_sit() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition pelvis = root.addOrReplaceChild("pelvis", CubeListBuilder.create().texOffs(16, 24).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 4.0F, 4.0F), PartPose.offsetAndRotation(0.0F, 8.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition spine = pelvis.addOrReplaceChild("spine", CubeListBuilder.create().texOffs(16, 20).addBox(-4.0F, -4.0F, -3.0F, 8.0F, 4.0F, 4.0F), PartPose.offsetAndRotation(0.0F, -2.0F, 1.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition chest = spine.addOrReplaceChild("chest", CubeListBuilder.create().texOffs(16, 16).addBox(-4.0F, -6.0F, -3.0F, 8.0F, 4.0F, 4.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition neck = chest.addOrReplaceChild("neck", CubeListBuilder.create().texOffs(15, -1).addBox(-2.0F, -0.5F, -1.5F, 4.0F, 1.0F, 3.0F), PartPose.offsetAndRotation(0.0F, -2.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition head = neck.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F), PartPose.offsetAndRotation(0.0F, -0.5F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition headwear = head.addOrReplaceChild("headwear", CubeListBuilder.create().texOffs(32, 0).addBox(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition left_ear = head.addOrReplaceChild("left_ear", CubeListBuilder.create().texOffs(24, 0).addBox(-3.0F, -3.0F, -0.5F, 6.0F, 6.0F, 1.0F), PartPose.offsetAndRotation(6.0F, -6.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition right_ear = head.addOrReplaceChild("right_ear", CubeListBuilder.create().texOffs(24, 0).addBox(-3.0F, -3.0F, -0.5F, 6.0F, 6.0F, 1.0F), PartPose.offsetAndRotation(-6.0F, -6.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition upper__arm__right = chest.addOrReplaceChild("upper__arm__right", CubeListBuilder.create().texOffs(40, 16).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 4.0F, 4.0F), PartPose.offsetAndRotation(-5.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition elbow__right = upper__arm__right.addOrReplaceChild("elbow__right", CubeListBuilder.create().texOffs(40, 20).addBox(-2.0F, 0.0F, -4.0F, 4.0F, 2.0F, 4.0F), PartPose.offsetAndRotation(0.0F, 2.0F, 2.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition forearm__right = elbow__right.addOrReplaceChild("forearm__right", CubeListBuilder.create().texOffs(40, 21).addBox(-2.0F, 0.0F, -4.0F, 4.0F, 4.0F, 4.0F), PartPose.offsetAndRotation(0.0F, 1.0F, 2.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition hand__right = forearm__right.addOrReplaceChild("hand__right", CubeListBuilder.create().texOffs(40, 32).addBox(-2.0F, 0.5F, -2.0F, 4.0F, 3.0F, 4.0F), PartPose.offsetAndRotation(0.0F, 1.5F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition upper__arm__left = chest.addOrReplaceChild("upper__arm__left", CubeListBuilder.create().texOffs(40, 16).addBox(-1.0F, -2.0F, -2.0F, 4.0F, 4.0F, 4.0F), PartPose.offsetAndRotation(5.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition elbow__left = upper__arm__left.addOrReplaceChild("elbow__left", CubeListBuilder.create().texOffs(40, 20).addBox(-2.0F, 0.0F, -4.0F, 4.0F, 2.0F, 4.0F), PartPose.offsetAndRotation(0.0F, 2.0F, 2.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition forearm__left = elbow__left.addOrReplaceChild("forearm__left", CubeListBuilder.create().texOffs(40, 21).addBox(-2.0F, 0.0F, -4.0F, 4.0F, 4.0F, 4.0F), PartPose.offsetAndRotation(0.0F, 1.0F, 2.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition hand__left = forearm__left.addOrReplaceChild("hand__left", CubeListBuilder.create().texOffs(40, 32).addBox(-2.0F, 0.5F, -2.0F, 4.0F, 3.0F, 4.0F), PartPose.offsetAndRotation(0.0F, 1.5F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition upper__leg__right = pelvis.addOrReplaceChild("upper__leg__right", CubeListBuilder.create().texOffs(0, 16).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F), PartPose.offsetAndRotation(-2.0F, 0.0F, 0.0F, -1.570796F, 0.087266F, 0.0F));
        PartDefinition knee__right = upper__leg__right.addOrReplaceChild("knee__right", CubeListBuilder.create().texOffs(0, 22).addBox(-2.0F, 0.0F, 0.0F, 4.0F, 2.0F, 4.0F), PartPose.offsetAndRotation(0.0F, 3.0F, -2.0F, 1.221731F, 0.0F, 0.0F));
        PartDefinition lower__leg__right = knee__right.addOrReplaceChild("lower__leg__right", CubeListBuilder.create().texOffs(0, 24).addBox(-2.0F, 0.0F, 0.0F, 4.0F, 4.0F, 4.0F), PartPose.offsetAndRotation(0.0F, 1.0F, -2.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition upper__leg__left = pelvis.addOrReplaceChild("upper__leg__left", CubeListBuilder.create().texOffs(0, 16).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F), PartPose.offsetAndRotation(2.0F, 0.0F, 0.0F, -1.570796F, -0.087266F, 0.0F));
        PartDefinition knee__left = upper__leg__left.addOrReplaceChild("knee__left", CubeListBuilder.create().texOffs(0, 22).addBox(-2.0F, 0.0F, 0.0F, 4.0F, 2.0F, 4.0F), PartPose.offsetAndRotation(0.0F, 3.0F, -2.0F, 1.221731F, 0.0F, 0.0F));
        PartDefinition lower__leg__left = knee__left.addOrReplaceChild("lower__leg__left", CubeListBuilder.create().texOffs(0, 24).addBox(-2.0F, 0.0F, 0.0F, 4.0F, 4.0F, 4.0F), PartPose.offsetAndRotation(0.0F, 1.0F, -2.0F, 0.0F, 0.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }
}
