package com.animania.extra;

import com.animania.extra.client.model.ExtraLegacyModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Guards against the empty hamster layer that previously rendered nothing. */
final class ExtraModelLayerTest {
    @Test
    void hamsterLayerContainsNativeGeometryAndAnimationParts() {
        ModelPart root = ExtraLegacyModelLayers.create("hamster").bakeRoot();
        assertTrue(root.hasChild("hamster_body"));
        assertTrue(root.hasChild("hamster_head"));
        assertTrue(root.hasChild("hamster_leg_back_right"));
        assertTrue(root.hasChild("hamster_leg_front_left"));
        assertTrue(root.hasChild("hamster_cheek_right0"));
        assertTrue(root.hasChild("hamster_cheek_right4"));
        assertTrue(root.hasChild("hamster_cheek_left0"));
        assertTrue(root.hasChild("hamster_cheek_left4"));
        assertTrue(root.getAllParts().anyMatch(part -> !part.isEmpty()));
    }
}
