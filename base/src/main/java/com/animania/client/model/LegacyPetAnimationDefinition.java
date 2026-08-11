package com.animania.client.model;

/** Source-derived Cats & Dogs animation parameters that differ per legacy model. */
public record LegacyPetAnimationDefinition(
        LegacyPoseDefinition sleepingPose,
        String lookPart,
        float pitchScale,
        float pitchOffset,
        float yawScale,
        float strideScale,
        boolean lookWhileSitting) {
    public static final LegacyPetAnimationDefinition EMPTY = new LegacyPetAnimationDefinition(
            LegacyPoseDefinition.EMPTY, "", 0.0F, 0.0F, 0.0F, 1.2F, false);

    public boolean active() {
        return !lookPart.isEmpty();
    }
}
