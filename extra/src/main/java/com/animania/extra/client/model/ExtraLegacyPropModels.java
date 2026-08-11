package com.animania.extra.client.model;

// Generated from the exact CraftStudio cuboid topology and UV layout.
import com.animania.client.model.LegacyCraftStudioCube;
import com.animania.client.model.LegacyCraftStudioModel;
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
        ModelPart part_2_base2 = LegacyCraftStudioModel.part(PartPose.offsetAndRotation(-11.0F, 6.0F, 0.0F, 0.0F, 0.0F, -2.094395F),
                new LegacyCraftStudioCube(new float[][]{{-0.5F, 12.5F, 0.5F}, {0.5F, 12.5F, 0.5F}, {0.5F, -0.5F, 0.5F}, {-0.5F, -0.5F, 0.5F}, {-0.5F, 12.5F, -0.5F}, {0.5F, 12.5F, -0.5F}, {0.5F, -0.5F, -0.5F}, {-0.5F, -0.5F, -0.5F}}, new int[][]{{15, 15, 14, 2}, {13, 15, 12, 2}, {14, 1, 15, 2}, {13, 1, 14, 2}, {17, 15, 15, 2}, {14, 15, 13, 2}}, 64, 32), Map.of());
        ModelPart part_3_axel1 = LegacyCraftStudioModel.part(PartPose.offsetAndRotation(-11.0F, 6.0F, -1.0F, 0.0F, 0.0F, -2.094395F),
                new LegacyCraftStudioCube(new float[][]{{-0.5F, 0.5F, 0.5F}, {0.5F, 0.5F, 0.5F}, {0.5F, -0.5F, 0.5F}, {-0.5F, -0.5F, 0.5F}, {-0.5F, 0.5F, -0.5F}, {0.5F, 0.5F, -0.5F}, {0.5F, -0.5F, -0.5F}, {-0.5F, -0.5F, -0.5F}}, new int[][]{{15, 3, 14, 2}, {13, 3, 12, 2}, {14, 1, 15, 2}, {13, 1, 14, 2}, {17, 3, 15, 2}, {14, 3, 13, 2}}, 64, 32), Map.of());
        ModelPart part_5_base4 = LegacyCraftStudioModel.part(PartPose.offsetAndRotation(-11.0F, 6.0F, 0.0F, 0.0F, 0.0F, -2.094395F),
                new LegacyCraftStudioCube(new float[][]{{-0.5F, 12.5F, 0.5F}, {0.5F, 12.5F, 0.5F}, {0.5F, -0.5F, 0.5F}, {-0.5F, -0.5F, 0.5F}, {-0.5F, 12.5F, -0.5F}, {0.5F, 12.5F, -0.5F}, {0.5F, -0.5F, -0.5F}, {-0.5F, -0.5F, -0.5F}}, new int[][]{{15, 15, 14, 2}, {13, 15, 12, 2}, {14, 1, 15, 2}, {13, 1, 14, 2}, {17, 15, 15, 2}, {14, 15, 13, 2}}, 64, 32), Map.of());
        ModelPart part_6_axel12 = LegacyCraftStudioModel.part(PartPose.offsetAndRotation(-11.0F, 6.000002F, 1.0F, 0.0F, 0.0F, -2.094395F),
                new LegacyCraftStudioCube(new float[][]{{-0.5F, 0.5F, 0.5F}, {0.5F, 0.5F, 0.5F}, {0.5F, -0.5F, 0.5F}, {-0.5F, -0.5F, 0.5F}, {-0.5F, 0.5F, -0.5F}, {0.5F, 0.5F, -0.5F}, {0.5F, -0.5F, -0.5F}, {-0.5F, -0.5F, -0.5F}}, new int[][]{{15, 3, 14, 2}, {13, 3, 12, 2}, {14, 1, 15, 2}, {13, 1, 14, 2}, {17, 3, 15, 2}, {14, 3, 13, 2}}, 64, 32), Map.of());
        ModelPart part_6_base3 = LegacyCraftStudioModel.part(PartPose.offsetAndRotation(-0.0F, 0.000001F, -12.0F, 0.0F, 0.0F, -0.0F),
                new LegacyCraftStudioCube(new float[][]{{-1.0F, 13.0F, 0.5F}, {0.0F, 13.0F, 0.5F}, {0.0F, 0.0F, 0.5F}, {-1.0F, 0.0F, 0.5F}, {-1.0F, 13.0F, -0.5F}, {0.0F, 13.0F, -0.5F}, {0.0F, 0.0F, -0.5F}, {-1.0F, 0.0F, -0.5F}}, new int[][]{{15, 15, 14, 2}, {13, 15, 12, 2}, {14, 1, 15, 2}, {13, 1, 14, 2}, {17, 15, 15, 2}, {14, 15, 13, 2}}, 64, 32), Map.ofEntries(Map.entry("base4", part_5_base4), Map.entry("axel12", part_6_axel12)));
        ModelPart part_7_base5 = LegacyCraftStudioModel.part(PartPose.offsetAndRotation(-0.500001F, 12.500001F, -6.0F, 0.0F, 0.0F, -1.570797F),
                new LegacyCraftStudioCube(new float[][]{{-0.5F, 0.5F, 6.0F}, {0.5F, 0.5F, 6.0F}, {0.5F, -0.5F, 6.0F}, {-0.5F, -0.5F, 6.0F}, {-0.5F, 0.5F, -6.0F}, {0.5F, 0.5F, -6.0F}, {0.5F, -0.5F, -6.0F}, {-0.5F, -0.5F, -6.0F}}, new int[][]{{29, 26, 17, 25}, {16, 26, 4, 25}, {17, 13, 18, 25}, {16, 13, 17, 25}, {42, 26, 29, 25}, {17, 26, 16, 25}}, 64, 32), Map.of());
        ModelPart part_15_wheel8 = LegacyCraftStudioModel.part(PartPose.offsetAndRotation(6.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.785398F),
                new LegacyCraftStudioCube(new float[][]{{0.0F, 1.0F, 5.0F}, {6.0F, 1.0F, 5.0F}, {6.0F, 0.0F, 5.0F}, {0.0F, 0.0F, 5.0F}, {0.0F, 1.0F, -5.0F}, {6.0F, 1.0F, -5.0F}, {6.0F, 0.0F, -5.0F}, {0.0F, 0.0F, -5.0F}}, new int[][]{{45, 13, 35, 12}, {29, 13, 19, 12}, {35, 2, 41, 12}, {29, 2, 35, 12}, {61, 13, 45, 12}, {35, 13, 29, 12}}, 64, 32), Map.of());
        ModelPart part_15_wheel7 = LegacyCraftStudioModel.part(PartPose.offsetAndRotation(6.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.785398F),
                new LegacyCraftStudioCube(new float[][]{{0.0F, 1.0F, 5.0F}, {6.0F, 1.0F, 5.0F}, {6.0F, 0.0F, 5.0F}, {0.0F, 0.0F, 5.0F}, {0.0F, 1.0F, -5.0F}, {6.0F, 1.0F, -5.0F}, {6.0F, 0.0F, -5.0F}, {0.0F, 0.0F, -5.0F}}, new int[][]{{45, 13, 35, 12}, {29, 13, 19, 12}, {35, 2, 41, 12}, {29, 2, 35, 12}, {61, 13, 45, 12}, {35, 13, 29, 12}}, 64, 32), Map.ofEntries(Map.entry("wheel8", part_15_wheel8)));
        ModelPart part_15_wheel6 = LegacyCraftStudioModel.part(PartPose.offsetAndRotation(6.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.785398F),
                new LegacyCraftStudioCube(new float[][]{{0.0F, 1.0F, 5.0F}, {6.0F, 1.0F, 5.0F}, {6.0F, 0.0F, 5.0F}, {0.0F, 0.0F, 5.0F}, {0.0F, 1.0F, -5.0F}, {6.0F, 1.0F, -5.0F}, {6.0F, 0.0F, -5.0F}, {0.0F, 0.0F, -5.0F}}, new int[][]{{45, 13, 35, 12}, {29, 13, 19, 12}, {35, 2, 41, 12}, {29, 2, 35, 12}, {61, 13, 45, 12}, {35, 13, 29, 12}}, 64, 32), Map.ofEntries(Map.entry("wheel7", part_15_wheel7)));
        ModelPart part_15_wheel5 = LegacyCraftStudioModel.part(PartPose.offsetAndRotation(6.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.785398F),
                new LegacyCraftStudioCube(new float[][]{{0.0F, 1.0F, 5.0F}, {6.0F, 1.0F, 5.0F}, {6.0F, 0.0F, 5.0F}, {0.0F, 0.0F, 5.0F}, {0.0F, 1.0F, -5.0F}, {6.0F, 1.0F, -5.0F}, {6.0F, 0.0F, -5.0F}, {0.0F, 0.0F, -5.0F}}, new int[][]{{45, 13, 35, 12}, {29, 13, 19, 12}, {35, 2, 41, 12}, {29, 2, 35, 12}, {61, 13, 45, 12}, {35, 13, 29, 12}}, 64, 32), Map.ofEntries(Map.entry("wheel6", part_15_wheel6)));
        ModelPart part_15_wheel4 = LegacyCraftStudioModel.part(PartPose.offsetAndRotation(6.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.785398F),
                new LegacyCraftStudioCube(new float[][]{{0.0F, 1.0F, 5.0F}, {6.0F, 1.0F, 5.0F}, {6.0F, 0.0F, 5.0F}, {0.0F, 0.0F, 5.0F}, {0.0F, 1.0F, -5.0F}, {6.0F, 1.0F, -5.0F}, {6.0F, 0.0F, -5.0F}, {0.0F, 0.0F, -5.0F}}, new int[][]{{45, 13, 35, 12}, {29, 13, 19, 12}, {35, 2, 41, 12}, {29, 2, 35, 12}, {61, 13, 45, 12}, {35, 13, 29, 12}}, 64, 32), Map.ofEntries(Map.entry("wheel5", part_15_wheel5)));
        ModelPart part_15_wheel3 = LegacyCraftStudioModel.part(PartPose.offsetAndRotation(6.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.785398F),
                new LegacyCraftStudioCube(new float[][]{{0.0F, 1.0F, 5.0F}, {6.0F, 1.0F, 5.0F}, {6.0F, 0.0F, 5.0F}, {0.0F, 0.0F, 5.0F}, {0.0F, 1.0F, -5.0F}, {6.0F, 1.0F, -5.0F}, {6.0F, 0.0F, -5.0F}, {0.0F, 0.0F, -5.0F}}, new int[][]{{45, 13, 35, 12}, {29, 13, 19, 12}, {35, 2, 41, 12}, {29, 2, 35, 12}, {61, 13, 45, 12}, {35, 13, 29, 12}}, 64, 32), Map.ofEntries(Map.entry("wheel4", part_15_wheel4)));
        ModelPart part_15_wheel2 = LegacyCraftStudioModel.part(PartPose.offsetAndRotation(3.0F, -0.5F, 0.0F, 0.0F, 0.0F, 0.785398F),
                new LegacyCraftStudioCube(new float[][]{{0.0F, 1.0F, 5.0F}, {6.0F, 1.0F, 5.0F}, {6.0F, 0.0F, 5.0F}, {0.0F, 0.0F, 5.0F}, {0.0F, 1.0F, -5.0F}, {6.0F, 1.0F, -5.0F}, {6.0F, 0.0F, -5.0F}, {0.0F, 0.0F, -5.0F}}, new int[][]{{45, 13, 35, 12}, {29, 13, 19, 12}, {35, 2, 41, 12}, {29, 2, 35, 12}, {61, 13, 45, 12}, {35, 13, 29, 12}}, 64, 32), Map.ofEntries(Map.entry("wheel3", part_15_wheel3)));
        ModelPart part_17_stick2 = LegacyCraftStudioModel.part(PartPose.offsetAndRotation(-0.000001F, 0.0F, -9.5F, 0.0F, 0.0F, 0.0F),
                new LegacyCraftStudioCube(new float[][]{{-0.5F, 6.5F, 0.0F}, {0.5F, 6.5F, 0.0F}, {0.5F, -6.5F, 0.0F}, {-0.5F, -6.5F, 0.0F}, {-0.5F, 6.5F, 0.0F}, {0.5F, 6.5F, 0.0F}, {0.5F, -6.5F, 0.0F}, {-0.5F, -6.5F, 0.0F}}, new int[][]{{14, 15, 14, 2}, {13, 15, 13, 2}, {14, 2, 15, 2}, {13, 2, 14, 2}, {15, 15, 14, 2}, {14, 15, 13, 2}}, 64, 32), Map.of());
        ModelPart part_17_stick = LegacyCraftStudioModel.part(PartPose.offsetAndRotation(-0.000001F, 6.5F, 4.75F, 0.0F, 0.0F, 0.0F),
                new LegacyCraftStudioCube(new float[][]{{-0.5F, 6.5F, 0.0F}, {0.5F, 6.5F, 0.0F}, {0.5F, -6.5F, 0.0F}, {-0.5F, -6.5F, 0.0F}, {-0.5F, 6.5F, 0.0F}, {0.5F, 6.5F, 0.0F}, {0.5F, -6.5F, 0.0F}, {-0.5F, -6.5F, 0.0F}}, new int[][]{{14, 15, 14, 2}, {13, 15, 13, 2}, {14, 2, 15, 2}, {13, 2, 14, 2}, {15, 15, 14, 2}, {14, 15, 13, 2}}, 64, 32), Map.ofEntries(Map.entry("stick2", part_17_stick2)));
        ModelPart part_17_wheel1 = LegacyCraftStudioModel.part(PartPose.offsetAndRotation(-17.5F, 6.000003F, -6.0F, 0.0F, 0.0F, -1.570797F),
                new LegacyCraftStudioCube(new float[][]{{-3.0F, 0.5F, 5.0F}, {3.0F, 0.5F, 5.0F}, {3.0F, -0.5F, 5.0F}, {-3.0F, -0.5F, 5.0F}, {-3.0F, 0.5F, -5.0F}, {3.0F, 0.5F, -5.0F}, {3.0F, -0.5F, -5.0F}, {-3.0F, -0.5F, -5.0F}}, new int[][]{{45, 13, 35, 12}, {29, 13, 19, 12}, {35, 2, 41, 12}, {29, 2, 35, 12}, {61, 13, 45, 12}, {35, 13, 29, 12}}, 64, 32), Map.ofEntries(Map.entry("wheel2", part_15_wheel2), Map.entry("stick", part_17_stick)));
        ModelPart part_18_axel1b = LegacyCraftStudioModel.part(PartPose.offsetAndRotation(-11.0F, 6.0F, 1.0F, 0.0F, 0.0F, -2.094395F),
                new LegacyCraftStudioCube(new float[][]{{-0.5F, 0.5F, 1.0F}, {0.5F, 0.5F, 1.0F}, {0.5F, -0.5F, 1.0F}, {-0.5F, -0.5F, 1.0F}, {-0.5F, 0.5F, -1.0F}, {0.5F, 0.5F, -1.0F}, {0.5F, -0.5F, -1.0F}, {-0.5F, -0.5F, -1.0F}}, new int[][]{{5, 3, 3, 2}, {2, 3, 0, 2}, {3, 0, 4, 2}, {2, 0, 3, 2}, {8, 3, 5, 2}, {3, 3, 2, 2}}, 64, 32), Map.of());
        ModelPart part_18_base1 = LegacyCraftStudioModel.part(PartPose.offsetAndRotation(6.0F, 24.0F, 6.0F, 0.0F, 0.0F, 1.570796F),
                new LegacyCraftStudioCube(new float[][]{{-1.0F, 13.0F, 0.5F}, {0.0F, 13.0F, 0.5F}, {0.0F, 0.0F, 0.5F}, {-1.0F, 0.0F, 0.5F}, {-1.0F, 13.0F, -0.5F}, {0.0F, 13.0F, -0.5F}, {0.0F, 0.0F, -0.5F}, {-1.0F, 0.0F, -0.5F}}, new int[][]{{15, 15, 14, 2}, {13, 15, 12, 2}, {14, 1, 15, 2}, {13, 1, 14, 2}, {17, 15, 15, 2}, {14, 15, 13, 2}}, 64, 32), Map.ofEntries(Map.entry("base2", part_2_base2), Map.entry("axel1", part_3_axel1), Map.entry("base3", part_6_base3), Map.entry("base5", part_7_base5), Map.entry("wheel1", part_17_wheel1), Map.entry("axel1b", part_18_axel1b)));
        return LegacyCraftStudioModel.root(Map.ofEntries(Map.entry("base1", part_18_base1)));
    }
}
