package com.animania.client.model;

/** A sparse, breed-specific pose converted from the 1.12 model source. */
public final class LegacyPoseDefinition {
    public static final LegacyPoseDefinition EMPTY = new LegacyPoseDefinition();

    private final LegacyPartPose[] parts;

    public LegacyPoseDefinition(LegacyPartPose... parts) {
        this.parts = parts.clone();
    }

    public LegacyPartPose[] parts() {
        return parts.clone();
    }
}
