package com.animania.client.model;

/**
 * Sparse absolute pose values converted from a legacy ModelRenderer model.
 * {@link Float#NaN} means that the corresponding axis remains animated.
 */
public record LegacyPartPose(
        String path,
        float x,
        float y,
        float z,
        float xRot,
        float yRot,
        float zRot) {
}
