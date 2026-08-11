package com.animania.extra;

import com.animania.extra.client.model.ExtraLegacyModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
        assertEquals(-1.5F, root.getChild("hamster_head").x, 0.0001F,
                "the normal-pose head must share the body's legacy X centre");
        assertEquals(0.0F, root.getChild("hamster_leg_back_right").xRot, 0.0001F,
                "the generated layer incorrectly rotated the normal rear leg by ninety degrees");
        assertEquals(0.0F, root.getChild("hamster_leg_back_left").xRot, 0.0001F,
                "the generated layer incorrectly rotated the normal rear leg by ninety degrees");
    }
}
