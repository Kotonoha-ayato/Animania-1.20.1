package com.animania.client;

import com.animania.client.model.AnimaniaHamsterBallModel;
import net.minecraft.client.model.geom.ModelPart;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class AnimaniaHamsterBallModelTest {
    @Test
    void cageLayerHasRenderableBallPart() {
        ModelPart root = AnimaniaHamsterBallModel.createBodyLayer().bakeRoot();
        assertTrue(root.hasChild("ball"));
        assertTrue(root.getChild("ball").getAllParts().count() == 1);
    }
}
