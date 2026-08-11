package com.animania.client.model;

/** Per-entity translation applied by the original 1.12 renderer before scaling. */
public record LegacyRenderTransform(float x, float y, float z) {
    public static final LegacyRenderTransform EMPTY = new LegacyRenderTransform(0.0F, 0.0F, 0.0F);
}
