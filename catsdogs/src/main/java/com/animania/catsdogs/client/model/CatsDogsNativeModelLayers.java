package com.animania.catsdogs.client.model;

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

public final class CatsDogsNativeModelLayers {
    public static final Map<String, ModelLayerLocation> LAYERS = new LinkedHashMap<>();
    static {
        LAYERS.put("model_cat_bed_1", new ModelLayerLocation(new ResourceLocation("animania_catsdogs", "native/model_cat_bed_1"), "main"));
        LAYERS.put("model_cat_bed_2", new ModelLayerLocation(new ResourceLocation("animania_catsdogs", "native/model_cat_bed_2"), "main"));
        LAYERS.put("model_cat_tower", new ModelLayerLocation(new ResourceLocation("animania_catsdogs", "native/model_cat_tower"), "main"));
        LAYERS.put("model_dog_house", new ModelLayerLocation(new ResourceLocation("animania_catsdogs", "native/model_dog_house"), "main"));
        LAYERS.put("model_dog_pillow", new ModelLayerLocation(new ResourceLocation("animania_catsdogs", "native/model_dog_pillow"), "main"));
        LAYERS.put("model_litter_box", new ModelLayerLocation(new ResourceLocation("animania_catsdogs", "native/model_litter_box"), "main"));
        LAYERS.put("model_pet_bowl", new ModelLayerLocation(new ResourceLocation("animania_catsdogs", "native/model_pet_bowl"), "main"));
        LAYERS.put("model_ragdoll", new ModelLayerLocation(new ResourceLocation("animania_catsdogs", "native/model_ragdoll"), "main"));
    }
    private CatsDogsNativeModelLayers() {}
    public static LayerDefinition create(String id) {
        return switch (id) {
            case "model_cat_bed_1" -> model_cat_bed_1();
            case "model_cat_bed_2" -> model_cat_bed_2();
            case "model_cat_tower" -> model_cat_tower();
            case "model_dog_house" -> model_dog_house();
            case "model_dog_pillow" -> model_dog_pillow();
            case "model_litter_box" -> model_litter_box();
            case "model_pet_bowl" -> model_pet_bowl();
            case "model_ragdoll" -> model_ragdoll();
            default -> throw new IllegalArgumentException("Unknown legacy native model " + id);
        };
    }
    private static LayerDefinition model_cat_bed_1() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition block1 = root.addOrReplaceChild("block1", CubeListBuilder.create().texOffs(1, 1).addBox(-5.0F, -0.5F, -7.5F, 10.0F, 1.0F, 15.0F), PartPose.offsetAndRotation(-0.25F, 0.5F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition block1_2 = root.addOrReplaceChild("block1_2", CubeListBuilder.create().texOffs(0, 0).addBox(-0.5F, -2.0F, -8.0F, 1.0F, 4.0F, 16.0F), PartPose.offsetAndRotation(-5.5F, 2.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition block1_3 = root.addOrReplaceChild("block1_3", CubeListBuilder.create().texOffs(0, 0).addBox(-0.5F, -2.0F, -8.0F, 1.0F, 4.0F, 16.0F), PartPose.offsetAndRotation(5.0F, 2.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition block1_4 = root.addOrReplaceChild("block1_4", CubeListBuilder.create().texOffs(15, 15).addBox(-5.0F, -2.0F, -0.5F, 10.0F, 4.0F, 1.0F), PartPose.offsetAndRotation(-0.25F, 2.0F, -7.5F, 0.0F, 0.0F, 0.0F));
        PartDefinition block1_5 = root.addOrReplaceChild("block1_5", CubeListBuilder.create().texOffs(-14, -14).addBox(-4.5F, -0.5F, -7.0F, 9.0F, 1.0F, 14.0F), PartPose.offsetAndRotation(-0.25F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 32);
    }
    private static LayerDefinition model_cat_bed_2() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition root_node = root.addOrReplaceChild("root_node", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, 1.5F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition side = root_node.addOrReplaceChild("side", CubeListBuilder.create().texOffs(7, 0).addBox(-3.5F, -1.0F, -1.0F, 7.0F, 2.0F, 2.0F), PartPose.offsetAndRotation(-7.0F, 1.5F, 0.0F, 0.0F, 1.570796F, 0.0F));
        PartDefinition side_2 = side.addOrReplaceChild("side_2", CubeListBuilder.create().texOffs(5, 0).addBox(-3.5F, -1.0F, -1.0F, 7.0F, 2.0F, 2.0F), PartPose.offsetAndRotation(7.0F, 0.0F, 7.0F, 3.141593F, -1.570796F, 3.141593F));
        PartDefinition side_3 = side.addOrReplaceChild("side_3", CubeListBuilder.create().texOffs(5, 0).addBox(-3.5F, -1.0F, 6.0F, 7.0F, 2.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 7.0F, 0.0F, -0.785398F, 0.0F));
        PartDefinition side_4 = side.addOrReplaceChild("side_4", CubeListBuilder.create().texOffs(5, 0).addBox(-3.5F, -1.0F, 6.0F, 7.0F, 2.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 7.0F, 3.141593F, -0.785398F, 3.141593F));
        PartDefinition side_5 = side.addOrReplaceChild("side_5", CubeListBuilder.create().texOffs(5, 0).addBox(-3.5F, -1.0F, -8.0F, 7.0F, 2.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 7.0F, 0.0F, -0.785398F, 0.0F));
        PartDefinition side_6 = side.addOrReplaceChild("side_6", CubeListBuilder.create().texOffs(5, 0).addBox(-3.5F, -1.0F, -1.0F, 7.0F, 2.0F, 2.0F), PartPose.offsetAndRotation(-7.0F, 0.0F, 6.999999F, 3.141593F, -1.570796F, 3.141593F));
        PartDefinition side_7 = side.addOrReplaceChild("side_7", CubeListBuilder.create().texOffs(5, 0).addBox(-3.5F, -1.0F, -1.0F, 7.0F, 2.0F, 2.0F), PartPose.offsetAndRotation(-0.000002F, 0.0F, 14.0F, -3.141593F, 0.0F, 3.141593F));
        PartDefinition side_8 = side.addOrReplaceChild("side_8", CubeListBuilder.create().texOffs(5, 0).addBox(-3.5F, -1.0F, -8.0F, 7.0F, 2.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 7.0F, 3.141593F, -0.785398F, 3.141593F));
        PartDefinition bottom = root_node.addOrReplaceChild("bottom", CubeListBuilder.create().texOffs(0, 14).addBox(-3.5F, -1.0F, -8.0F, 7.0F, 2.0F, 16.0F), PartPose.offsetAndRotation(0.0F, -0.5F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition bottom_2 = bottom.addOrReplaceChild("bottom_2", CubeListBuilder.create().texOffs(0, 14).addBox(-3.5F, -1.0F, -8.0F, 7.0F, 2.0F, 16.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 1.570796F, 0.0F));
        PartDefinition bottom_3 = bottom_2.addOrReplaceChild("bottom_3", CubeListBuilder.create().texOffs(0, 14).addBox(-3.5F, -1.0F, -8.0F, 7.0F, 2.0F, 16.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -0.785398F, 0.0F));
        PartDefinition bottom_4 = bottom_3.addOrReplaceChild("bottom_4", CubeListBuilder.create().texOffs(0, 14).addBox(-3.5F, -1.0F, -8.0F, 7.0F, 2.0F, 16.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 3.141593F, -1.570796F, 3.141593F));
        return LayerDefinition.create(mesh, 64, 32);
    }
    private static LayerDefinition model_cat_tower() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition root_node = root.addOrReplaceChild("root_node", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, 8.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition base = root_node.addOrReplaceChild("base", CubeListBuilder.create().texOffs(28, 49).addBox(-8.0F, -1.0F, -8.0F, 16.0F, 2.0F, 16.0F), PartPose.offsetAndRotation(0.0F, -7.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition box_one_1 = base.addOrReplaceChild("box_one_1", CubeListBuilder.create().texOffs(12, 23).addBox(-0.5F, -3.5F, -5.0F, 1.0F, 7.0F, 10.0F), PartPose.offsetAndRotation(-6.5F, 12.0F, -2.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition box_one_2 = box_one_1.addOrReplaceChild("box_one_2", CubeListBuilder.create().texOffs(24, 25).addBox(-4.5F, -0.5F, -3.5F, 9.0F, 1.0F, 7.0F), PartPose.offsetAndRotation(5.0F, 0.0F, -4.5F, 1.570796F, 0.0F, 0.0F));
        PartDefinition box_one_3 = box_one_1.addOrReplaceChild("box_one_3", CubeListBuilder.create().texOffs(13, 49).addBox(-0.5F, -0.5F, -3.5F, 1.0F, 1.0F, 7.0F), PartPose.offsetAndRotation(9.0F, 0.0F, 4.5F, 1.570796F, 0.0F, 0.0F));
        PartDefinition box_one_4 = box_one_1.addOrReplaceChild("box_one_4", CubeListBuilder.create().texOffs(13, 8).addBox(-5.0F, -0.5F, -5.0F, 10.0F, 1.0F, 10.0F), PartPose.offsetAndRotation(4.5F, 4.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition box_one_5 = box_one_1.addOrReplaceChild("box_one_5", CubeListBuilder.create().texOffs(34, 33).addBox(-2.5F, -0.5F, -5.0F, 5.0F, 1.0F, 10.0F), PartPose.offsetAndRotation(12.0F, -3.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition box_one_6 = box_one_1.addOrReplaceChild("box_one_6", CubeListBuilder.create().texOffs(78, 51).addBox(-7.5F, -0.5F, -5.0F, 15.0F, 1.0F, 10.0F), PartPose.offsetAndRotation(4.5F, -3.0F, 2.5F, 0.0F, 1.570796F, 0.0F));
        PartDefinition box_one__door = box_one_1.addOrReplaceChild("box_one__door", CubeListBuilder.create().texOffs(15, 40).addBox(-0.5F, -1.0F, -3.5F, 1.0F, 2.0F, 7.0F), PartPose.offsetAndRotation(9.0F, 0.0F, 3.0F, 1.570796F, 0.0F, 0.0F));
        PartDefinition box_one__door2 = box_one__door.addOrReplaceChild("box_one__door2", CubeListBuilder.create().texOffs(24, 42).addBox(-0.5F, -1.0F, -3.5F, 1.0F, 2.0F, 7.0F), PartPose.offsetAndRotation(0.0F, -6.0F, -0.000001F, 0.0F, 0.0F, 0.0F));
        PartDefinition box_one__door3 = box_one__door.addOrReplaceChild("box_one__door3", CubeListBuilder.create().texOffs(33, 19).addBox(-0.5F, -2.0F, -1.0F, 1.0F, 4.0F, 2.0F), PartPose.offsetAndRotation(0.0F, -3.0F, -2.5F, 0.0F, 0.0F, 0.0F));
        PartDefinition box_one__door_2 = box_one_1.addOrReplaceChild("box_one__door_2", CubeListBuilder.create().texOffs(27, 33).addBox(-0.5F, -1.0F, -3.5F, 1.0F, 2.0F, 7.0F), PartPose.offsetAndRotation(1.0F, 0.0F, 4.5F, 3.141593F, -1.570796F, 3.141593F));
        PartDefinition box_one__door2_2 = box_one__door_2.addOrReplaceChild("box_one__door2_2", CubeListBuilder.create().texOffs(24, 51).addBox(-0.5F, -1.0F, -3.5F, 1.0F, 2.0F, 7.0F), PartPose.offsetAndRotation(0.0F, -6.5F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition box_one__door4 = box_one__door_2.addOrReplaceChild("box_one__door4", CubeListBuilder.create().texOffs(36, 33).addBox(-0.5F, -2.5F, -1.0F, 1.0F, 5.0F, 2.0F), PartPose.offsetAndRotation(0.0F, -3.25F, -2.5F, -0.0F, 0.0F, -0.0F));
        PartDefinition leg1 = base.addOrReplaceChild("leg1", CubeListBuilder.create().texOffs(25, 94).addBox(-1.0F, -15.0F, -1.0F, 2.0F, 30.0F, 2.0F), PartPose.offsetAndRotation(-6.0F, 4.0F, -5.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition leg2 = base.addOrReplaceChild("leg2", CubeListBuilder.create().texOffs(0, 58).addBox(-1.0F, -33.0F, -1.0F, 2.0F, 66.0F, 2.0F), PartPose.offsetAndRotation(6.0F, 10.0F, -5.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition leg3 = base.addOrReplaceChild("leg3", CubeListBuilder.create().texOffs(9, 93).addBox(-1.0F, -15.5F, -1.0F, 2.0F, 31.0F, 2.0F), PartPose.offsetAndRotation(-2.0F, 4.0F, 5.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition leg4 = base.addOrReplaceChild("leg4", CubeListBuilder.create().texOffs(33, 109).addBox(-1.0F, -7.5F, -1.0F, 2.0F, 15.0F, 2.0F), PartPose.offsetAndRotation(1.0F, 18.0F, 1.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition leg5 = base.addOrReplaceChild("leg5", CubeListBuilder.create().texOffs(17, 91).addBox(-1.0F, -16.5F, -1.0F, 2.0F, 33.0F, 2.0F), PartPose.offsetAndRotation(-1.0F, 22.0F, -5.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition string = base.addOrReplaceChild("string", CubeListBuilder.create().texOffs(110, 66).addBox(-1.0F, -21.0F, -1.0F, 2.0F, 42.0F, 2.0F), PartPose.offsetAndRotation(-6.0F, 25.0F, -5.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition toy = string.addOrReplaceChild("toy", CubeListBuilder.create().texOffs(12, 9).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F), PartPose.offsetAndRotation(0.0F, -3.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition toy2 = toy.addOrReplaceChild("toy2", CubeListBuilder.create().texOffs(12, 9).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition toy3 = toy.addOrReplaceChild("toy3", CubeListBuilder.create().texOffs(12, 9).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition toy4 = toy.addOrReplaceChild("toy4", CubeListBuilder.create().texOffs(12, 9).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition platform1 = base.addOrReplaceChild("platform1", CubeListBuilder.create().texOffs(48, 5).addBox(-5.0F, -0.5F, -5.0F, 10.0F, 1.0F, 10.0F), PartPose.offsetAndRotation(2.5F, 20.5F, -2.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition platform1_edge1 = platform1.addOrReplaceChild("platform1_edge1", CubeListBuilder.create().texOffs(78, 4).addBox(-0.5F, -0.5F, -5.0F, 1.0F, 1.0F, 10.0F), PartPose.offsetAndRotation(-4.5F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition platform1_edge2 = platform1.addOrReplaceChild("platform1_edge2", CubeListBuilder.create().texOffs(78, 4).addBox(-0.5F, -0.5F, -5.0F, 1.0F, 1.0F, 10.0F), PartPose.offsetAndRotation(4.5F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition platform_edge3 = platform1.addOrReplaceChild("platform_edge3", CubeListBuilder.create().texOffs(90, 12).addBox(-5.0F, -0.5F, -0.5F, 10.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 1.0F, -4.5F, 0.0F, 0.0F, 0.0F));
        PartDefinition platform_edge4 = platform1.addOrReplaceChild("platform_edge4", CubeListBuilder.create().texOffs(90, 10).addBox(-5.0F, -0.5F, -0.5F, 10.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 1.0F, 4.5F, 0.0F, 0.0F, 0.0F));
        PartDefinition platform2 = base.addOrReplaceChild("platform2", CubeListBuilder.create().texOffs(78, 18).addBox(-4.0F, -0.5F, -4.0F, 8.0F, 1.0F, 8.0F), PartPose.offsetAndRotation(-3.5F, 27.0F, -3.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition platform2_edge1 = platform2.addOrReplaceChild("platform2_edge1", CubeListBuilder.create().texOffs(84, 30).addBox(-4.0F, -0.5F, -0.5F, 8.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 1.0F, -3.5F, 0.0F, 0.0F, 0.0F));
        PartDefinition platform2_edge2 = platform2.addOrReplaceChild("platform2_edge2", CubeListBuilder.create().texOffs(83, 33).addBox(-0.5F, -0.5F, -4.0F, 1.0F, 1.0F, 8.0F), PartPose.offsetAndRotation(-3.5F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition platform2_edge3 = platform2.addOrReplaceChild("platform2_edge3", CubeListBuilder.create().texOffs(84, 30).addBox(-4.0F, -0.5F, -0.5F, 8.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 1.0F, 3.5F, 0.0F, 0.0F, 0.0F));
        PartDefinition platform2_edge4 = platform2.addOrReplaceChild("platform2_edge4", CubeListBuilder.create().texOffs(83, 33).addBox(-0.5F, -0.5F, -4.0F, 1.0F, 1.0F, 8.0F), PartPose.offsetAndRotation(3.5F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition ramp = base.addOrReplaceChild("ramp", CubeListBuilder.create().texOffs(31, 68).addBox(-2.5F, -2.5F, -10.0F, 5.0F, 1.0F, 20.0F), PartPose.offsetAndRotation(5.5F, 6.0F, 6.5F, 0.993109F, 0.0F, 0.0F));
        return LayerDefinition.create(mesh, 128, 128);
    }
    private static LayerDefinition model_dog_house() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition block1 = root.addOrReplaceChild("block1", CubeListBuilder.create().texOffs(4, 1).addBox(-0.5F, -7.0F, -8.0F, 1.0F, 14.0F, 16.0F), PartPose.offsetAndRotation(-7.5F, 7.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition block1_2 = block1.addOrReplaceChild("block1_2", CubeListBuilder.create().texOffs(3, 0).addBox(-0.5F, -7.0F, -8.0F, 1.0F, 14.0F, 16.0F), PartPose.offsetAndRotation(15.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition block1_3 = block1_2.addOrReplaceChild("block1_3", CubeListBuilder.create().texOffs(15, 15).addBox(-0.5F, -5.0F, -0.5F, 1.0F, 10.0F, 1.0F), PartPose.offsetAndRotation(-12.5F, -2.0F, 7.5F, 0.0F, 0.0F, 0.0F));
        PartDefinition block1_4 = block1_3.addOrReplaceChild("block1_4", CubeListBuilder.create().texOffs(15, 15).addBox(-2.0F, -2.5F, -0.5F, 2.0F, 5.0F, 1.0F), PartPose.offsetAndRotation(2.0F, 6.0F, 0.0F, 0.0F, 0.0F, -0.785398F));
        PartDefinition block1_5 = block1_3.addOrReplaceChild("block1_5", CubeListBuilder.create().texOffs(15, 15).addBox(-2.0F, -2.5F, -0.5F, 2.0F, 5.0F, 1.0F), PartPose.offsetAndRotation(5.0F, 9.25F, 0.0F, 0.0F, 0.0F, 1.570796F));
        PartDefinition block1_6 = block1_5.addOrReplaceChild("block1_6", CubeListBuilder.create().texOffs(15, 15).addBox(-0.5F, -5.0F, -0.5F, 1.0F, 10.0F, 1.0F), PartPose.offsetAndRotation(-8.250001F, -5.0F, 0.0F, 0.0F, 0.0F, -1.570797F));
        PartDefinition block1_7 = block1_6.addOrReplaceChild("block1_7", CubeListBuilder.create().texOffs(15, 15).addBox(0.0F, -2.5F, -0.5F, 2.0F, 5.0F, 1.0F), PartPose.offsetAndRotation(-2.0F, 6.0F, 0.0F, 0.0F, 0.0F, 0.785398F));
        PartDefinition block1_8 = root.addOrReplaceChild("block1_8", CubeListBuilder.create().texOffs(26, 32).addBox(-0.5F, -7.0F, -9.0F, 1.0F, 14.0F, 18.0F), PartPose.offsetAndRotation(4.52F, 17.5F, 0.0F, 0.0F, 0.0F, 0.785398F));
        PartDefinition block1_9 = block1_8.addOrReplaceChild("block1_9", CubeListBuilder.create().texOffs(26, 32).addBox(-0.5F, -7.0F, -9.0F, 1.0F, 14.0F, 18.0F), PartPose.offsetAndRotation(-6.36396F, 6.363962F, 0.0F, 0.0F, 0.0F, -1.570797F));
        PartDefinition block1_10 = block1_8.addOrReplaceChild("block1_10", CubeListBuilder.create().texOffs(15, 15).addBox(-1.0F, -7.0F, -0.5F, 2.0F, 14.0F, 1.0F), PartPose.offsetAndRotation(-14.86338F, 0.014143F, 7.5F, 0.0F, 0.0F, -0.785399F));
        PartDefinition block1_11 = block1_10.addOrReplaceChild("block1_11", CubeListBuilder.create().texOffs(15, 15).addBox(-1.0F, -7.0F, -0.5F, 2.0F, 14.0F, 1.0F), PartPose.offsetAndRotation(12.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition block1_12 = block1_11.addOrReplaceChild("block1_12", CubeListBuilder.create().texOffs(15, 15).addBox(-2.5F, -7.5F, -0.5F, 3.0F, 15.0F, 1.0F), PartPose.offsetAndRotation(-6.0F, 7.0F, 0.0F, 0.0F, 0.0F, 1.570796F));
        PartDefinition block1_13 = block1_12.addOrReplaceChild("block1_13", CubeListBuilder.create().texOffs(15, 15).addBox(-2.5F, -6.0F, -0.5F, 3.0F, 12.0F, 1.0F), PartPose.offsetAndRotation(0.999999F, 0.0F, 0.0F, 0.0F, 0.0F, -0.0F));
        PartDefinition block1_14 = root.addOrReplaceChild("block1_14", CubeListBuilder.create().texOffs(15, 15).addBox(-2.0F, -7.0F, -0.5F, 2.0F, 14.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 15.5F, -7.5F, 0.0F, 0.0F, 1.570796F));
        PartDefinition block1_15 = block1_14.addOrReplaceChild("block1_15", CubeListBuilder.create().texOffs(15, 15).addBox(-2.0F, -6.5F, -0.5F, 2.0F, 13.0F, 1.0F), PartPose.offsetAndRotation(1.499999F, 0.0F, 0.0F, 0.0F, 0.0F, -0.0F));
        PartDefinition block1_16 = block1_15.addOrReplaceChild("block1_16", CubeListBuilder.create().texOffs(15, 15).addBox(-2.0F, -5.5F, -0.5F, 2.0F, 11.0F, 1.0F), PartPose.offsetAndRotation(1.999999F, 0.0F, 0.0F, 0.0F, 0.0F, -0.0F));
        PartDefinition block1_17 = block1_16.addOrReplaceChild("block1_17", CubeListBuilder.create().texOffs(15, 15).addBox(-2.0F, -4.5F, -0.5F, 2.0F, 9.0F, 1.0F), PartPose.offsetAndRotation(2.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.0F));
        PartDefinition block1_18 = block1_17.addOrReplaceChild("block1_18", CubeListBuilder.create().texOffs(15, 15).addBox(-2.0F, -3.5F, -0.5F, 2.0F, 7.0F, 1.0F), PartPose.offsetAndRotation(2.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.0F));
        PartDefinition block1_19 = block1_18.addOrReplaceChild("block1_19", CubeListBuilder.create().texOffs(15, 15).addBox(-2.0F, -2.5F, -0.5F, 2.0F, 5.0F, 1.0F), PartPose.offsetAndRotation(2.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.0F));
        PartDefinition block1_20 = block1_19.addOrReplaceChild("block1_20", CubeListBuilder.create().texOffs(15, 15).addBox(-2.0F, -1.5F, -0.5F, 2.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(2.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.0F));
        PartDefinition block1_21 = block1_20.addOrReplaceChild("block1_21", CubeListBuilder.create().texOffs(15, 15).addBox(-2.0F, -7.5F, -0.5F, 2.0F, 15.0F, 1.0F), PartPose.offsetAndRotation(-5.0F, -0.0F, 0.0F, 0.0F, 0.0F, 0.000001F));
        PartDefinition block1_22 = block1_20.addOrReplaceChild("block1_22", CubeListBuilder.create().texOffs(15, 15).addBox(-2.0F, -0.5F, -0.5F, 2.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(2.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.0F));
        PartDefinition block1_23 = block1_20.addOrReplaceChild("block1_23", CubeListBuilder.create().texOffs(13, 16).addBox(-8.0F, -7.5F, -0.5F, 14.0F, 15.0F, 1.0F), PartPose.offsetAndRotation(-12.0F, -0.0F, 0.0F, 0.0F, 0.0F, 0.000001F));
        PartDefinition block1_24 = root.addOrReplaceChild("block1_24", CubeListBuilder.create().texOffs(15, 15).addBox(-2.0F, -7.0F, -0.5F, 2.0F, 14.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 15.5F, 7.5F, 0.0F, 0.0F, 1.570796F));
        PartDefinition block1_25 = block1_24.addOrReplaceChild("block1_25", CubeListBuilder.create().texOffs(15, 15).addBox(-2.0F, -6.5F, -0.5F, 2.0F, 13.0F, 1.0F), PartPose.offsetAndRotation(1.499999F, 0.0F, 0.0F, 0.0F, 0.0F, -0.0F));
        PartDefinition block1_26 = block1_25.addOrReplaceChild("block1_26", CubeListBuilder.create().texOffs(15, 15).addBox(-2.0F, -5.5F, -0.5F, 2.0F, 11.0F, 1.0F), PartPose.offsetAndRotation(1.999999F, 0.0F, 0.0F, 0.0F, 0.0F, -0.0F));
        PartDefinition block1_27 = block1_26.addOrReplaceChild("block1_27", CubeListBuilder.create().texOffs(15, 15).addBox(-2.0F, -4.5F, -0.5F, 2.0F, 9.0F, 1.0F), PartPose.offsetAndRotation(2.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.0F));
        PartDefinition block1_28 = block1_27.addOrReplaceChild("block1_28", CubeListBuilder.create().texOffs(15, 15).addBox(-2.0F, -3.5F, -0.5F, 2.0F, 7.0F, 1.0F), PartPose.offsetAndRotation(2.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.0F));
        PartDefinition block1_29 = block1_28.addOrReplaceChild("block1_29", CubeListBuilder.create().texOffs(15, 15).addBox(-2.0F, -2.5F, -0.5F, 2.0F, 5.0F, 1.0F), PartPose.offsetAndRotation(2.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.0F));
        PartDefinition block1_30 = block1_29.addOrReplaceChild("block1_30", CubeListBuilder.create().texOffs(15, 15).addBox(-2.0F, -1.5F, -0.5F, 2.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(2.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.0F));
        PartDefinition block1_31 = block1_30.addOrReplaceChild("block1_31", CubeListBuilder.create().texOffs(15, 15).addBox(-2.0F, -0.5F, -0.5F, 2.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(2.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.0F));
        PartDefinition block = root.addOrReplaceChild("block", CubeListBuilder.create().texOffs(0, 0).addBox(-8.0F, -0.5F, -8.0F, 16.0F, 1.0F, 16.0F), PartPose.offsetAndRotation(0.0F, 0.5F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition block_2 = root.addOrReplaceChild("block_2", CubeListBuilder.create().texOffs(7, 0).addBox(-8.0F, -0.5F, -8.0F, 16.0F, 1.0F, 16.0F), PartPose.offsetAndRotation(0.0F, 13.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }
    private static LayerDefinition model_dog_pillow() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition root_node = root.addOrReplaceChild("root_node", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition pillow__c = root_node.addOrReplaceChild("pillow__c", CubeListBuilder.create().texOffs(0, 0).addBox(-11.0F, -1.0F, -7.0F, 22.0F, 2.0F, 14.0F), PartPose.offsetAndRotation(0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition pillow2 = pillow__c.addOrReplaceChild("pillow2", CubeListBuilder.create().texOffs(0, 17).addBox(-21.0F, -1.0F, -13.0F, 42.0F, 2.0F, 26.0F), PartPose.offsetAndRotation(0.0F, 0.5F, 0.0F, 0.0F, 0.0F, 0.0F));
        return LayerDefinition.create(mesh, 128, 128);
    }
    private static LayerDefinition model_litter_box() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition root_node = root.addOrReplaceChild("root_node", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, 0.5F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition sand = root_node.addOrReplaceChild("sand", CubeListBuilder.create().texOffs(4, 31).addBox(-6.0F, -0.5F, -6.5F, 12.0F, 1.0F, 13.0F), PartPose.offsetAndRotation(0.0F, 1.5F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition pooper__scooper__back = root_node.addOrReplaceChild("pooper__scooper__back", CubeListBuilder.create().texOffs(13, 53).addBox(-3.5F, -2.5F, -0.5F, 7.0F, 5.0F, 1.0F), PartPose.offsetAndRotation(4.0F, 4.0F, -4.9F, 0.132641F, -0.898366F, -0.045373F));
        PartDefinition pooper__scooper_1 = pooper__scooper__back.addOrReplaceChild("pooper__scooper_1", CubeListBuilder.create().texOffs(8, 48).addBox(-2.0F, -0.5F, -3.0F, 4.0F, 1.0F, 6.0F), PartPose.offsetAndRotation(0.0F, -1.000002F, 2.749999F, 0.0F, 0.0F, 0.0F));
        PartDefinition pooper__scooper_2 = pooper__scooper_1.addOrReplaceChild("pooper__scooper_2", CubeListBuilder.create().texOffs(8, 48).addBox(-0.5F, -0.5F, -3.0F, 1.0F, 1.0F, 6.0F), PartPose.offsetAndRotation(-1.75F, 1.0F, 0.0F, 0.302466F, 0.0F, 0.0F));
        PartDefinition pooper__scooper_3 = pooper__scooper_1.addOrReplaceChild("pooper__scooper_3", CubeListBuilder.create().texOffs(12, 52).addBox(-0.5F, -1.0F, -1.0F, 1.0F, 2.0F, 2.0F), PartPose.offsetAndRotation(-1.75F, 0.5F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition pooper__scooper_4 = pooper__scooper_1.addOrReplaceChild("pooper__scooper_4", CubeListBuilder.create().texOffs(12, 52).addBox(-0.5F, -1.5F, -1.0F, 1.0F, 3.0F, 2.0F), PartPose.offsetAndRotation(-1.75F, 0.75F, -2.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition pooper__scooper_5 = pooper__scooper_1.addOrReplaceChild("pooper__scooper_5", CubeListBuilder.create().texOffs(13, 53).addBox(-0.5F, -2.5F, -0.5F, 1.0F, 5.0F, 1.0F), PartPose.offsetAndRotation(-1.75F, 1.0F, -2.75F, 0.0F, 0.0F, 0.0F));
        PartDefinition pooper__scooper_1_2 = pooper__scooper__back.addOrReplaceChild("pooper__scooper_1_2", CubeListBuilder.create().texOffs(10, 49).addBox(-2.0F, -0.5F, -2.5F, 4.0F, 1.0F, 5.0F), PartPose.offsetAndRotation(-0.0F, -1.000002F, 2.749999F, 0.0F, -0.0F, 0.0F));
        PartDefinition pooper__scooper_2_2 = pooper__scooper_1_2.addOrReplaceChild("pooper__scooper_2_2", CubeListBuilder.create().texOffs(8, 48).addBox(-0.5F, -0.5F, -3.0F, 1.0F, 1.0F, 6.0F), PartPose.offsetAndRotation(1.75F, 1.0F, 0.0F, 0.302492F, 0.0F, 0.0F));
        PartDefinition pooper__scooper_3_2 = pooper__scooper_1_2.addOrReplaceChild("pooper__scooper_3_2", CubeListBuilder.create().texOffs(12, 52).addBox(-0.5F, -1.0F, -1.0F, 1.0F, 2.0F, 2.0F), PartPose.offsetAndRotation(1.75F, 0.5F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition pooper__scooper_4_2 = pooper__scooper_1_2.addOrReplaceChild("pooper__scooper_4_2", CubeListBuilder.create().texOffs(12, 52).addBox(-0.5F, -1.5F, -1.0F, 1.0F, 3.0F, 2.0F), PartPose.offsetAndRotation(1.75F, 0.75F, -2.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition pooper__scooper_5_2 = pooper__scooper_1_2.addOrReplaceChild("pooper__scooper_5_2", CubeListBuilder.create().texOffs(13, 53).addBox(-0.5F, -2.5F, -0.5F, 1.0F, 5.0F, 1.0F), PartPose.offsetAndRotation(1.75F, 1.0F, -2.75F, 0.0F, 0.0F, 0.0F));
        PartDefinition pooper__scooper__handle = pooper__scooper__back.addOrReplaceChild("pooper__scooper__handle", CubeListBuilder.create().texOffs(6, 46).addBox(-1.0F, -1.0F, -4.0F, 2.0F, 2.0F, 8.0F), PartPose.offsetAndRotation(0.0F, 1.000001F, -2.25F, 0.135637F, 0.000689F, 0.000138F));
        PartDefinition base = root_node.addOrReplaceChild("base", CubeListBuilder.create().texOffs(1, 1).addBox(-6.0F, -0.5F, -7.5F, 12.0F, 1.0F, 15.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition side__left = base.addOrReplaceChild("side__left", CubeListBuilder.create().texOffs(1, 1).addBox(-0.5F, -2.5F, -7.5F, 1.0F, 5.0F, 15.0F), PartPose.offsetAndRotation(-6.0F, 2.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition side__left__top = side__left.addOrReplaceChild("side__left__top", CubeListBuilder.create().texOffs(1, 1).addBox(-0.5F, -0.5F, -7.5F, 1.0F, 1.0F, 15.0F), PartPose.offsetAndRotation(-0.25F, 2.25F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition back__top_left = side__left__top.addOrReplaceChild("back__top_left", CubeListBuilder.create().texOffs(15, 15).addBox(-0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, -7.25F, 0.0F, 0.0F, 0.0F));
        PartDefinition side_right__top = side__left.addOrReplaceChild("side_right__top", CubeListBuilder.create().texOffs(1, 1).addBox(-0.5F, -0.5F, -7.5F, 1.0F, 1.0F, 15.0F), PartPose.offsetAndRotation(12.25F, 2.25F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition back__top_left_2 = side_right__top.addOrReplaceChild("back__top_left_2", CubeListBuilder.create().texOffs(15, 15).addBox(-0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, -7.25F, 0.0F, 0.0F, 0.0F));
        PartDefinition side__right = base.addOrReplaceChild("side__right", CubeListBuilder.create().texOffs(1, 1).addBox(-0.5F, -2.5F, -7.5F, 1.0F, 5.0F, 15.0F), PartPose.offsetAndRotation(6.0F, 2.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition front__middle = base.addOrReplaceChild("front__middle", CubeListBuilder.create().texOffs(15, 15).addBox(-4.5F, -1.0F, -0.5F, 9.0F, 2.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 2.0F, 7.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition front__right = front__middle.addOrReplaceChild("front__right", CubeListBuilder.create().texOffs(15, 15).addBox(-1.0F, -2.5F, -0.5F, 2.0F, 5.0F, 1.0F), PartPose.offsetAndRotation(5.5F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition front__right_2 = front__right.addOrReplaceChild("front__right_2", CubeListBuilder.create().texOffs(15, 15).addBox(-1.0F, -2.5F, -0.5F, 2.0F, 5.0F, 1.0F), PartPose.offsetAndRotation(-1.5F, 0.0F, 0.0F, 0.0F, 0.0F, -0.523599F));
        PartDefinition back = base.addOrReplaceChild("back", CubeListBuilder.create().texOffs(15, 15).addBox(-6.0F, -2.5F, -0.5F, 12.0F, 5.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 2.0F, -7.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition front__middle_2 = base.addOrReplaceChild("front__middle_2", CubeListBuilder.create().texOffs(15, 15).addBox(-4.5F, -1.0F, -0.5F, 9.0F, 2.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 1.0F, 7.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition back__top = base.addOrReplaceChild("back__top", CubeListBuilder.create().texOffs(15, 15).addBox(-6.5F, -0.5F, -0.5F, 13.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 4.25F, -7.25F, 0.0F, 0.0F, 0.0F));
        PartDefinition front__left = base.addOrReplaceChild("front__left", CubeListBuilder.create().texOffs(15, 15).addBox(-1.0F, -2.5F, -0.5F, 2.0F, 5.0F, 1.0F), PartPose.offsetAndRotation(-5.5F, 2.0F, 7.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition front__left_2 = front__left.addOrReplaceChild("front__left_2", CubeListBuilder.create().texOffs(15, 15).addBox(-1.0F, -2.5F, -0.5F, 2.0F, 5.0F, 1.0F), PartPose.offsetAndRotation(1.5F, 0.0F, 0.0F, 0.0F, 0.0F, 0.523599F));
        PartDefinition front__left_2_2 = front__left.addOrReplaceChild("front__left_2_2", CubeListBuilder.create().texOffs(15, 15).addBox(-1.0F, -2.5F, -0.5F, 2.0F, 5.0F, 1.0F), PartPose.offsetAndRotation(1.5F, 0.0F, 0.0F, 0.0F, 0.0F, 0.523599F));
        return LayerDefinition.create(mesh, 64, 64);
    }
    private static LayerDefinition model_pet_bowl() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition root__node = root.addOrReplaceChild("root__node", CubeListBuilder.create(), PartPose.offsetAndRotation(-8.0F, 1.5F, -8.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition bottom_ = root__node.addOrReplaceChild("bottom_", CubeListBuilder.create().texOffs(12, 6).addBox(-4.0F, -0.5F, -2.0F, 8.0F, 1.0F, 4.0F), PartPose.offsetAndRotation(0.0F, -1.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition bottom_2 = bottom_.addOrReplaceChild("bottom_2", CubeListBuilder.create().texOffs(12, 6).addBox(-4.0F, -0.5F, -2.0F, 8.0F, 1.0F, 4.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 1.570796F, 0.0F));
        PartDefinition bottom_3 = bottom_.addOrReplaceChild("bottom_3", CubeListBuilder.create().texOffs(12, 6).addBox(-4.0F, -0.5F, -2.0F, 8.0F, 1.0F, 4.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.785398F, 0.0F));
        PartDefinition bottom_4 = bottom_.addOrReplaceChild("bottom_4", CubeListBuilder.create().texOffs(12, 6).addBox(-4.0F, -0.5F, -2.0F, 8.0F, 1.0F, 4.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -0.785398F, 0.0F));
        PartDefinition side_8 = root__node.addOrReplaceChild("side_8", CubeListBuilder.create().texOffs(12, 12).addBox(-0.5F, -2.0F, -2.0F, 1.0F, 4.0F, 4.0F), PartPose.offsetAndRotation(3.61F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition side_7 = root__node.addOrReplaceChild("side_7", CubeListBuilder.create().texOffs(12, 12).addBox(-0.5F, -2.0F, -2.0F, 1.0F, 4.0F, 4.0F), PartPose.offsetAndRotation(-3.61F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition side_4 = root__node.addOrReplaceChild("side_4", CubeListBuilder.create().texOffs(12, 12).addBox(3.35F, 1.0F, -2.0F, 1.0F, 4.0F, 4.0F), PartPose.offsetAndRotation(0.0F, -2.0F, 0.0F, 0.0F, 0.785398F, 0.0F));
        PartDefinition side_6 = root__node.addOrReplaceChild("side_6", CubeListBuilder.create().texOffs(12, 12).addBox(3.35F, 1.0F, -2.0F, 1.0F, 4.0F, 4.0F), PartPose.offsetAndRotation(0.0F, -2.0F, 0.0F, 0.0F, -0.785398F, 0.0F));
        PartDefinition side_5 = root__node.addOrReplaceChild("side_5", CubeListBuilder.create().texOffs(12, 12).addBox(-4.35F, 1.0F, -2.0F, 1.0F, 4.0F, 4.0F), PartPose.offsetAndRotation(0.0F, -2.0F, 0.0F, 0.0F, -0.785398F, 0.0F));
        PartDefinition side_2 = root__node.addOrReplaceChild("side_2", CubeListBuilder.create().texOffs(12, 12).addBox(-0.5F, -2.0F, -2.0F, 1.0F, 4.0F, 4.0F), PartPose.offsetAndRotation(0.0F, 1.0F, 3.61F, 0.0F, 1.570796F, 0.0F));
        PartDefinition side_1 = root__node.addOrReplaceChild("side_1", CubeListBuilder.create().texOffs(12, 12).addBox(-0.5F, -2.0F, -2.0F, 1.0F, 4.0F, 4.0F), PartPose.offsetAndRotation(0.0F, 1.0F, -3.6F, 0.0F, 1.570796F, 0.0F));
        PartDefinition side_3 = root__node.addOrReplaceChild("side_3", CubeListBuilder.create().texOffs(12, 12).addBox(-4.35F, 1.0F, -2.0F, 1.0F, 4.0F, 4.0F), PartPose.offsetAndRotation(0.0F, -2.0F, 0.0F, 0.0F, 0.785398F, 0.0F));
        return LayerDefinition.create(mesh, 64, 32);
    }
    private static LayerDefinition model_ragdoll() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(38, 42).addBox(-4.5F, -6.0F, -5.5F, 9.0F, 10.0F, 11.0F), PartPose.offsetAndRotation(0.0F, 12.0F, 4.0F, 0.034907F, 0.0F, 0.0F));
        PartDefinition lower_body = body.addOrReplaceChild("lower_body", CubeListBuilder.create().texOffs(80, 41).addBox(-4.0F, -5.0F, -12.5F, 8.0F, 9.0F, 13.0F), PartPose.offsetAndRotation(0.0F, 1.0F, -4.0F, -0.044147F, 0.0F, 0.0F));
        PartDefinition tail = lower_body.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(44, 16).addBox(-1.5F, -2.0F, -9.5F, 3.0F, 4.0F, 9.0F), PartPose.offsetAndRotation(0.0F, 3.0F, -5.0F, -0.973323F, 0.0F, 0.0F));
        PartDefinition tail2 = tail.addOrReplaceChild("tail2", CubeListBuilder.create().texOffs(71, 27).addBox(-1.5F, -2.0F, -5.5F, 3.0F, 4.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 0.0F, -3.0F, 0.404708F, 0.0F, 0.0F));
        PartDefinition tail3 = tail2.addOrReplaceChild("tail3", CubeListBuilder.create().texOffs(62, 6).addBox(-2.0F, -2.5F, -5.0F, 4.0F, 5.0F, 6.0F), PartPose.offsetAndRotation(0.0F, 0.0F, -3.0F, 0.436277F, 0.0F, 0.0F));
        PartDefinition tail4 = tail3.addOrReplaceChild("tail4", CubeListBuilder.create().texOffs(57, 5).addBox(-1.5F, -3.0F, -2.0F, 3.0F, 4.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 1.0F, -3.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition back_leg_r1 = lower_body.addOrReplaceChild("back_leg_r1", CubeListBuilder.create().texOffs(19, 51).addBox(-1.0F, -8.5F, -3.0F, 2.0F, 8.0F, 5.0F), PartPose.offsetAndRotation(-4.0F, 4.5F, -3.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition back_leg_r2 = back_leg_r1.addOrReplaceChild("back_leg_r2", CubeListBuilder.create().texOffs(18, 35).addBox(-0.99F, -9.5F, -2.2F, 2.0F, 12.0F, 3.0F), PartPose.offsetAndRotation(0.0F, -2.5F, -1.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition back_leg_l1 = lower_body.addOrReplaceChild("back_leg_l1", CubeListBuilder.create().texOffs(19, 51).addBox(-1.0F, -8.5F, -3.0F, 2.0F, 8.0F, 5.0F), PartPose.offsetAndRotation(4.0F, 4.5F, -3.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition back_leg_l2 = back_leg_l1.addOrReplaceChild("back_leg_l2", CubeListBuilder.create().texOffs(18, 35).addBox(-1.01F, -9.5F, -2.2F, 2.0F, 12.0F, 3.0F), PartPose.offsetAndRotation(0.0F, -2.5F, -1.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition neck1 = body.addOrReplaceChild("neck1", CubeListBuilder.create().texOffs(11, 16).addBox(-2.5F, -4.0F, -3.0F, 5.0F, 6.0F, 10.0F), PartPose.offsetAndRotation(0.0F, 3.0F, 5.0F, -0.868145F, 0.0F, 0.0F));
        PartDefinition head_base = neck1.addOrReplaceChild("head_base", CubeListBuilder.create().texOffs(102, 4).addBox(-3.0F, -2.0F, 0.5F, 6.0F, 6.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 1.0F, 1.7F, 1.038364F, 0.0F, 0.0F));
        PartDefinition head_front = head_base.addOrReplaceChild("head_front", CubeListBuilder.create().texOffs(0, 12).addBox(-2.0F, 0.0F, 0.4F, 4.0F, 2.0F, 5.0F), PartPose.offsetAndRotation(0.0F, -1.0F, -1.5F, 0.15708F, 0.0F, 0.0F));
        PartDefinition head_slope = head_front.addOrReplaceChild("head_slope", CubeListBuilder.create().texOffs(37, 11).addBox(-1.0F, 0.0F, 0.4F, 2.0F, 2.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 1.0F, -3.0F, 0.351233F, 0.0F, 0.0F));
        PartDefinition nose = head_slope.addOrReplaceChild("nose", CubeListBuilder.create().texOffs(10, 0).addBox(-0.5F, -1.1F, 0.3F, 1.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.7F, 1.5F, -0.176772F, 0.0F, 0.0F));
        PartDefinition jaw = head_base.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(0, 20).addBox(-1.5F, -0.3F, -1.0F, 3.0F, 1.0F, 4.0F), PartPose.offsetAndRotation(0.0F, -2.4F, 0.5F, -0.009828F, 0.0F, 0.0F));
        PartDefinition cheek_l = head_base.addOrReplaceChild("cheek_l", CubeListBuilder.create().texOffs(32, 41).addBox(0.5F, -1.9F, -0.5F, 3.0F, 5.0F, 3.0F), PartPose.offsetAndRotation(1.4F, -1.0F, -2.0F, 0.031686F, -0.176182F, 0.084957F));
        PartDefinition cheek_l2 = cheek_l.addOrReplaceChild("cheek_l2", CubeListBuilder.create().texOffs(32, 36).addBox(0.5F, -0.9F, 0.0F, 3.0F, 3.0F, 2.0F), PartPose.offsetAndRotation(-1.0F, -0.6F, -1.5F, 0.0F, -0.240054F, 0.0F));
        PartDefinition cheek_r = head_base.addOrReplaceChild("cheek_r", CubeListBuilder.create().texOffs(32, 41).addBox(-3.5F, -1.9F, -0.5F, 3.0F, 5.0F, 3.0F), PartPose.offsetAndRotation(-1.4F, -1.0F, -2.0F, 0.031686F, 0.176182F, -0.084957F));
        PartDefinition cheek_r2 = cheek_r.addOrReplaceChild("cheek_r2", CubeListBuilder.create().texOffs(32, 36).addBox(-3.5F, -0.9F, 0.0F, 3.0F, 3.0F, 2.0F), PartPose.offsetAndRotation(1.0F, -0.6F, -1.5F, 0.0F, 0.240054F, 0.0F));
        PartDefinition ear_r = head_base.addOrReplaceChild("ear_r", CubeListBuilder.create().texOffs(3, 3).addBox(-2.0F, 0.1F, -1.5F, 3.0F, 1.0F, 4.0F), PartPose.offsetAndRotation(-2.5F, 2.5F, -0.700001F, -1.513855F, -0.042108F, 0.532973F));
        PartDefinition ear_r2 = ear_r.addOrReplaceChild("ear_r2", CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, 0.1F, 0.0F, 2.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(0.0F, -0.6F, 1.7F, 0.150501F, 0.0F, 0.0F));
        PartDefinition ear_r3 = ear_r2.addOrReplaceChild("ear_r3", CubeListBuilder.create().texOffs(1, 0).addBox(-0.5F, 0.1F, 0.0F, 1.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(0.0F, -0.6F, 0.3F, 0.189827F, 0.0F, 0.0F));
        PartDefinition ear_l = head_base.addOrReplaceChild("ear_l", CubeListBuilder.create().texOffs(3, 3).addBox(-1.0F, 0.1F, -1.5F, 3.0F, 1.0F, 4.0F), PartPose.offsetAndRotation(2.5F, 2.5F, -0.700001F, -1.513855F, 0.042108F, -0.532972F));
        PartDefinition ear_l2 = ear_l.addOrReplaceChild("ear_l2", CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, 0.1F, 0.0F, 2.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(0.0F, -0.6F, 1.7F, 0.150501F, 0.0F, 0.0F));
        PartDefinition ear_l3 = ear_l2.addOrReplaceChild("ear_l3", CubeListBuilder.create().texOffs(1, 0).addBox(-0.5F, 0.1F, 0.0F, 1.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(0.0F, -0.6F, 0.3F, 0.189827F, 0.0F, 0.0F));
        PartDefinition mane = neck1.addOrReplaceChild("mane", CubeListBuilder.create().texOffs(96, 22).addBox(-5.0F, -5.8F, -1.0F, 10.0F, 8.0F, 6.0F), PartPose.offsetAndRotation(0.0F, 2.5F, -3.0F, 0.160293F, 0.0F, 0.0F));
        PartDefinition leg_r1 = body.addOrReplaceChild("leg_r1", CubeListBuilder.create().texOffs(2, 37).addBox(-1.0F, -8.5F, -2.0F, 2.0F, 9.0F, 4.0F), PartPose.offsetAndRotation(-4.2F, 5.0F, 2.0F, -0.034907F, 0.0F, 0.0F));
        PartDefinition leg_r2 = leg_r1.addOrReplaceChild("leg_r2", CubeListBuilder.create().texOffs(1, 52).addBox(-0.88F, -8.5F, -0.9F, 2.0F, 9.0F, 3.0F), PartPose.offsetAndRotation(0.0F, -4.0F, -0.2F, 0.0F, 0.0F, 0.0F));
        PartDefinition leg_l1 = body.addOrReplaceChild("leg_l1", CubeListBuilder.create().texOffs(2, 37).addBox(-1.0F, -8.5F, -2.0F, 2.0F, 9.0F, 4.0F), PartPose.offsetAndRotation(4.2F, 5.0F, 2.0F, -0.034907F, 0.0F, 0.0F));
        PartDefinition leg_l2 = leg_l1.addOrReplaceChild("leg_l2", CubeListBuilder.create().texOffs(1, 52).addBox(-1.12F, -8.5F, -0.9F, 2.0F, 9.0F, 3.0F), PartPose.offsetAndRotation(0.0F, -4.0F, -0.2F, 0.0F, 0.0F, 0.0F));
        return LayerDefinition.create(mesh, 128, 64);
    }
}
