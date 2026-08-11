package com.animania.client;

import com.animania.client.model.AnimaniaHamsterBallModel;
import net.minecraft.client.model.geom.ModelPart;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

final class AnimaniaHamsterBallModelTest {
    @Test
    void cageLayerHasRenderableBallPart() {
        ModelPart root = AnimaniaHamsterBallModel.createBodyLayer().bakeRoot();
        assertTrue(root.hasChild("ball"));
        ModelPart ball = root.getChild("ball");
        assertEquals(17, ball.getAllParts().count(), "ball plus all sixteen legacy cage shapes");
        for (int index = 1; index <= 16; index++) {
            assertTrue(ball.hasChild("shape" + index), "missing legacy cage shape " + index);
        }
    }
}
