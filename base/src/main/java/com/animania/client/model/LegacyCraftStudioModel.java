package com.animania.client.model;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;

import java.util.List;
import java.util.Map;

/** Factory helpers for generated, direct ModelPart trees. */
public final class LegacyCraftStudioModel {
    private LegacyCraftStudioModel() { }

    public static ModelPart part(PartPose pose, LegacyCraftStudioCube cube, Map<String, ModelPart> children) {
        ModelPart part = new ModelPart(cube == null ? List.of() : List.of(cube), children);
        part.setInitialPose(pose);
        part.loadPose(pose);
        return part;
    }

    public static ModelPart root(Map<String, ModelPart> children) {
        return part(PartPose.ZERO, null, children);
    }
}
