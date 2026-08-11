package com.animania.extra.client.model;

// Generated from the exact CraftStudio cuboid topology and UV layout.
import com.animania.client.model.LegacyMeshCube;
import com.animania.client.model.LegacyMeshModel;
import java.util.Map;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;

public final class ExtraLegacyPropModels {
    private ExtraLegacyPropModels() { }

    public static ModelPart create(String id) {
        return switch (id) {
            case "model_hamster_wheel" -> model_hamster_wheel();
            default -> throw new IllegalArgumentException("Unknown exact CraftStudio model " + id);
        };
    }

    private static ModelPart model_hamster_wheel() {
        ModelPart part_2_base2 = LegacyMeshModel.part(PartPose.offsetAndRotation(-11.0F, 6.0F, 0.0F, 0.0F, 0.0F, -2.094395F),
                new LegacyMeshCube(new float[][]{{-0.5F, 12.5F, 0.5F}, {0.5F, 12.5F, 0.5F}, {0.5F, -0.5F, 0.5F}, {-0.5F, -0.5F, 0.5F}, {-0.5F, 12.5F, -0.5F}, {0.5F, 12.5F, -0.5F}, {0.5F, -0.5F, -0.5F}, {-0.5F, -0.5F, -0.5F}}, new int[][]{{11, -13, 12, 0}, {11, -13, 12, 0}, {12, 1, 13, 0}, {11, 1, 12, 0}, {11, -13, 11, 0}, {12, -13, 11, 0}}, 64, 32, false), Map.of());
        ModelPart part_3_axel1 = LegacyMeshModel.part(PartPose.offsetAndRotation(-11.0F, 6.0F, -1.0F, 0.0F, 0.0F, -2.094395F),
                new LegacyMeshCube(new float[][]{{-0.5F, 0.5F, 0.5F}, {0.5F, 0.5F, 0.5F}, {0.5F, -0.5F, 0.5F}, {-0.5F, -0.5F, 0.5F}, {-0.5F, 0.5F, -0.5F}, {0.5F, 0.5F, -0.5F}, {0.5F, -0.5F, -0.5F}, {-0.5F, -0.5F, -0.5F}}, new int[][]{{11, -1, 12, 0}, {11, -1, 12, 0}, {12, 1, 13, 0}, {11, 1, 12, 0}, {11, -1, 11, 0}, {12, -1, 11, 0}}, 64, 32, false), Map.of());
        ModelPart part_5_base4 = LegacyMeshModel.part(PartPose.offsetAndRotation(-11.0F, 6.0F, 0.0F, 0.0F, 0.0F, -2.094395F),
                new LegacyMeshCube(new float[][]{{-0.5F, 12.5F, 0.5F}, {0.5F, 12.5F, 0.5F}, {0.5F, -0.5F, 0.5F}, {-0.5F, -0.5F, 0.5F}, {-0.5F, 12.5F, -0.5F}, {0.5F, 12.5F, -0.5F}, {0.5F, -0.5F, -0.5F}, {-0.5F, -0.5F, -0.5F}}, new int[][]{{11, -13, 12, 0}, {11, -13, 12, 0}, {12, 1, 13, 0}, {11, 1, 12, 0}, {11, -13, 11, 0}, {12, -13, 11, 0}}, 64, 32, false), Map.of());
        ModelPart part_6_axel12 = LegacyMeshModel.part(PartPose.offsetAndRotation(-11.0F, 6.000002F, 1.0F, 0.0F, 0.0F, -2.094395F),
                new LegacyMeshCube(new float[][]{{-0.5F, 0.5F, 0.5F}, {0.5F, 0.5F, 0.5F}, {0.5F, -0.5F, 0.5F}, {-0.5F, -0.5F, 0.5F}, {-0.5F, 0.5F, -0.5F}, {0.5F, 0.5F, -0.5F}, {0.5F, -0.5F, -0.5F}, {-0.5F, -0.5F, -0.5F}}, new int[][]{{11, -1, 12, 0}, {11, -1, 12, 0}, {12, 1, 13, 0}, {11, 1, 12, 0}, {11, -1, 11, 0}, {12, -1, 11, 0}}, 64, 32, false), Map.of());
        ModelPart part_6_base3 = LegacyMeshModel.part(PartPose.offsetAndRotation(-0.0F, 0.000001F, -12.0F, 0.0F, 0.0F, -0.0F),
                new LegacyMeshCube(new float[][]{{-1.0F, 13.0F, 0.5F}, {0.0F, 13.0F, 0.5F}, {0.0F, 0.0F, 0.5F}, {-1.0F, 0.0F, 0.5F}, {-1.0F, 13.0F, -0.5F}, {0.0F, 13.0F, -0.5F}, {0.0F, 0.0F, -0.5F}, {-1.0F, 0.0F, -0.5F}}, new int[][]{{11, -13, 12, 0}, {11, -13, 12, 0}, {12, 1, 13, 0}, {11, 1, 12, 0}, {11, -13, 11, 0}, {12, -13, 11, 0}}, 64, 32, false), Map.ofEntries(Map.entry("base4", part_5_base4), Map.entry("axel12", part_6_axel12)));
        ModelPart part_7_base5 = LegacyMeshModel.part(PartPose.offsetAndRotation(-0.500001F, 12.500001F, -6.0F, 0.0F, 0.0F, -1.570797F),
                new LegacyMeshCube(new float[][]{{-0.5F, 0.5F, 6.0F}, {0.5F, 0.5F, 6.0F}, {0.5F, -0.5F, 6.0F}, {-0.5F, -0.5F, 6.0F}, {-0.5F, 0.5F, -6.0F}, {0.5F, 0.5F, -6.0F}, {0.5F, -0.5F, -6.0F}, {-0.5F, -0.5F, -6.0F}}, new int[][]{{-19, 0, -7, 1}, {-8, 0, 4, 1}, {-7, 13, -6, 1}, {-8, 13, -7, 1}, {-30, 0, -19, 1}, {-7, 0, -8, 1}}, 64, 32, false), Map.of());
        ModelPart part_15_wheel8 = LegacyMeshModel.part(PartPose.offsetAndRotation(6.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.785398F),
                new LegacyMeshCube(new float[][]{{0.0F, 1.0F, 5.0F}, {6.0F, 1.0F, 5.0F}, {6.0F, 0.0F, 5.0F}, {0.0F, 0.0F, 5.0F}, {0.0F, 1.0F, -5.0F}, {6.0F, 1.0F, -5.0F}, {6.0F, 0.0F, -5.0F}, {0.0F, 0.0F, -5.0F}}, new int[][]{{5, -9, 15, -8}, {9, -9, 19, -8}, {15, 2, 21, -8}, {9, 2, 15, -8}, {1, -9, 5, -8}, {15, -9, 9, -8}}, 64, 32, false), Map.of());
        ModelPart part_15_wheel7 = LegacyMeshModel.part(PartPose.offsetAndRotation(6.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.785398F),
                new LegacyMeshCube(new float[][]{{0.0F, 1.0F, 5.0F}, {6.0F, 1.0F, 5.0F}, {6.0F, 0.0F, 5.0F}, {0.0F, 0.0F, 5.0F}, {0.0F, 1.0F, -5.0F}, {6.0F, 1.0F, -5.0F}, {6.0F, 0.0F, -5.0F}, {0.0F, 0.0F, -5.0F}}, new int[][]{{5, -9, 15, -8}, {9, -9, 19, -8}, {15, 2, 21, -8}, {9, 2, 15, -8}, {1, -9, 5, -8}, {15, -9, 9, -8}}, 64, 32, false), Map.ofEntries(Map.entry("wheel8", part_15_wheel8)));
        ModelPart part_15_wheel6 = LegacyMeshModel.part(PartPose.offsetAndRotation(6.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.785398F),
                new LegacyMeshCube(new float[][]{{0.0F, 1.0F, 5.0F}, {6.0F, 1.0F, 5.0F}, {6.0F, 0.0F, 5.0F}, {0.0F, 0.0F, 5.0F}, {0.0F, 1.0F, -5.0F}, {6.0F, 1.0F, -5.0F}, {6.0F, 0.0F, -5.0F}, {0.0F, 0.0F, -5.0F}}, new int[][]{{5, -9, 15, -8}, {9, -9, 19, -8}, {15, 2, 21, -8}, {9, 2, 15, -8}, {1, -9, 5, -8}, {15, -9, 9, -8}}, 64, 32, false), Map.ofEntries(Map.entry("wheel7", part_15_wheel7)));
        ModelPart part_15_wheel5 = LegacyMeshModel.part(PartPose.offsetAndRotation(6.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.785398F),
                new LegacyMeshCube(new float[][]{{0.0F, 1.0F, 5.0F}, {6.0F, 1.0F, 5.0F}, {6.0F, 0.0F, 5.0F}, {0.0F, 0.0F, 5.0F}, {0.0F, 1.0F, -5.0F}, {6.0F, 1.0F, -5.0F}, {6.0F, 0.0F, -5.0F}, {0.0F, 0.0F, -5.0F}}, new int[][]{{5, -9, 15, -8}, {9, -9, 19, -8}, {15, 2, 21, -8}, {9, 2, 15, -8}, {1, -9, 5, -8}, {15, -9, 9, -8}}, 64, 32, false), Map.ofEntries(Map.entry("wheel6", part_15_wheel6)));
        ModelPart part_15_wheel4 = LegacyMeshModel.part(PartPose.offsetAndRotation(6.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.785398F),
                new LegacyMeshCube(new float[][]{{0.0F, 1.0F, 5.0F}, {6.0F, 1.0F, 5.0F}, {6.0F, 0.0F, 5.0F}, {0.0F, 0.0F, 5.0F}, {0.0F, 1.0F, -5.0F}, {6.0F, 1.0F, -5.0F}, {6.0F, 0.0F, -5.0F}, {0.0F, 0.0F, -5.0F}}, new int[][]{{5, -9, 15, -8}, {9, -9, 19, -8}, {15, 2, 21, -8}, {9, 2, 15, -8}, {1, -9, 5, -8}, {15, -9, 9, -8}}, 64, 32, false), Map.ofEntries(Map.entry("wheel5", part_15_wheel5)));
        ModelPart part_15_wheel3 = LegacyMeshModel.part(PartPose.offsetAndRotation(6.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.785398F),
                new LegacyMeshCube(new float[][]{{0.0F, 1.0F, 5.0F}, {6.0F, 1.0F, 5.0F}, {6.0F, 0.0F, 5.0F}, {0.0F, 0.0F, 5.0F}, {0.0F, 1.0F, -5.0F}, {6.0F, 1.0F, -5.0F}, {6.0F, 0.0F, -5.0F}, {0.0F, 0.0F, -5.0F}}, new int[][]{{5, -9, 15, -8}, {9, -9, 19, -8}, {15, 2, 21, -8}, {9, 2, 15, -8}, {1, -9, 5, -8}, {15, -9, 9, -8}}, 64, 32, false), Map.ofEntries(Map.entry("wheel4", part_15_wheel4)));
        ModelPart part_15_wheel2 = LegacyMeshModel.part(PartPose.offsetAndRotation(3.0F, -0.5F, 0.0F, 0.0F, 0.0F, 0.785398F),
                new LegacyMeshCube(new float[][]{{0.0F, 1.0F, 5.0F}, {6.0F, 1.0F, 5.0F}, {6.0F, 0.0F, 5.0F}, {0.0F, 0.0F, 5.0F}, {0.0F, 1.0F, -5.0F}, {6.0F, 1.0F, -5.0F}, {6.0F, 0.0F, -5.0F}, {0.0F, 0.0F, -5.0F}}, new int[][]{{5, -9, 15, -8}, {9, -9, 19, -8}, {15, 2, 21, -8}, {9, 2, 15, -8}, {1, -9, 5, -8}, {15, -9, 9, -8}}, 64, 32, false), Map.ofEntries(Map.entry("wheel3", part_15_wheel3)));
        ModelPart part_17_stick2 = LegacyMeshModel.part(PartPose.offsetAndRotation(-0.000001F, 0.0F, -9.5F, 0.0F, 0.0F, 0.0F),
                new LegacyMeshCube(new float[][]{{-0.5F, 6.5F, 0.0F}, {0.5F, 6.5F, 0.0F}, {0.5F, -6.5F, 0.0F}, {-0.5F, -6.5F, 0.0F}, {-0.5F, 6.5F, 0.0F}, {0.5F, 6.5F, 0.0F}, {0.5F, -6.5F, 0.0F}, {-0.5F, -6.5F, 0.0F}}, new int[][]{{14, -11, 14, 2}, {13, -11, 13, 2}, {14, 2, 15, 2}, {13, 2, 14, 2}, {15, -11, 14, 2}, {14, -11, 13, 2}}, 64, 32, false), Map.of());
        ModelPart part_17_stick = LegacyMeshModel.part(PartPose.offsetAndRotation(-0.000001F, 6.5F, 4.75F, 0.0F, 0.0F, 0.0F),
                new LegacyMeshCube(new float[][]{{-0.5F, 6.5F, 0.0F}, {0.5F, 6.5F, 0.0F}, {0.5F, -6.5F, 0.0F}, {-0.5F, -6.5F, 0.0F}, {-0.5F, 6.5F, 0.0F}, {0.5F, 6.5F, 0.0F}, {0.5F, -6.5F, 0.0F}, {-0.5F, -6.5F, 0.0F}}, new int[][]{{14, -11, 14, 2}, {13, -11, 13, 2}, {14, 2, 15, 2}, {13, 2, 14, 2}, {15, -11, 14, 2}, {14, -11, 13, 2}}, 64, 32, false), Map.ofEntries(Map.entry("stick2", part_17_stick2)));
        ModelPart part_17_wheel1 = LegacyMeshModel.part(PartPose.offsetAndRotation(-17.5F, 6.000003F, -6.0F, 0.0F, 0.0F, -1.570797F),
                new LegacyMeshCube(new float[][]{{-3.0F, 0.5F, 5.0F}, {3.0F, 0.5F, 5.0F}, {3.0F, -0.5F, 5.0F}, {-3.0F, -0.5F, 5.0F}, {-3.0F, 0.5F, -5.0F}, {3.0F, 0.5F, -5.0F}, {3.0F, -0.5F, -5.0F}, {-3.0F, -0.5F, -5.0F}}, new int[][]{{5, -9, 15, -8}, {9, -9, 19, -8}, {15, 2, 21, -8}, {9, 2, 15, -8}, {1, -9, 5, -8}, {15, -9, 9, -8}}, 64, 32, false), Map.ofEntries(Map.entry("wheel2", part_15_wheel2), Map.entry("stick", part_17_stick)));
        ModelPart part_18_axel1b = LegacyMeshModel.part(PartPose.offsetAndRotation(-11.0F, 6.0F, 1.0F, 0.0F, 0.0F, -2.094395F),
                new LegacyMeshCube(new float[][]{{-0.5F, 0.5F, 1.0F}, {0.5F, 0.5F, 1.0F}, {0.5F, -0.5F, 1.0F}, {-0.5F, -0.5F, 1.0F}, {-0.5F, 0.5F, -1.0F}, {0.5F, 0.5F, -1.0F}, {0.5F, -0.5F, -1.0F}, {-0.5F, -0.5F, -1.0F}}, new int[][]{{-3, -3, -1, -2}, {-2, -3, 0, -2}, {-1, 0, 0, -2}, {-2, 0, -1, -2}, {-4, -3, -3, -2}, {-1, -3, -2, -2}}, 64, 32, false), Map.of());
        ModelPart part_18_base1 = LegacyMeshModel.part(PartPose.offsetAndRotation(6.0F, 24.0F, 6.0F, 0.0F, 0.0F, 1.570796F),
                new LegacyMeshCube(new float[][]{{-1.0F, 13.0F, 0.5F}, {0.0F, 13.0F, 0.5F}, {0.0F, 0.0F, 0.5F}, {-1.0F, 0.0F, 0.5F}, {-1.0F, 13.0F, -0.5F}, {0.0F, 13.0F, -0.5F}, {0.0F, 0.0F, -0.5F}, {-1.0F, 0.0F, -0.5F}}, new int[][]{{11, -13, 12, 0}, {11, -13, 12, 0}, {12, 1, 13, 0}, {11, 1, 12, 0}, {11, -13, 11, 0}, {12, -13, 11, 0}}, 64, 32, false), Map.ofEntries(Map.entry("base2", part_2_base2), Map.entry("axel1", part_3_axel1), Map.entry("base3", part_6_base3), Map.entry("base5", part_7_base5), Map.entry("wheel1", part_17_wheel1), Map.entry("axel1b", part_18_axel1b)));
        return LegacyMeshModel.root(Map.ofEntries(Map.entry("base1", part_18_base1)));
    }
}
