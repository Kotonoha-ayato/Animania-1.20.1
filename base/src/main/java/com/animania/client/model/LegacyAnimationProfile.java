package com.animania.client.model;

/** Named ModelPart paths used to animate converted breed geometry. */
public record LegacyAnimationProfile(
        String[] heads,
        String[] leftLegs,
        String[] rightLegs,
        String[] tails,
        String[] wings,
        String[] bodies,
        String[] privateParts,
        String[] coloredParts) {
    public static final LegacyAnimationProfile EMPTY = new LegacyAnimationProfile(
            new String[0], new String[0], new String[0], new String[0], new String[0], new String[0], new String[0], new String[0]);
}
