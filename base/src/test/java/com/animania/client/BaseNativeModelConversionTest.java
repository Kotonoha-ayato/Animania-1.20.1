package com.animania.client;

import com.animania.client.model.BaseNativeAnimations;
import com.animania.client.model.BaseNativeModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Verifies the two Base legacy native models were converted to real ModelPart geometry. */
final class BaseNativeModelConversionTest {
    @Test
    void allBaseNativeModelsBakeAndNoUnownedClipsRemain() {
        assertEquals(2, BaseNativeModelLayers.LAYERS.size());
        BaseNativeModelLayers.LAYERS.keySet().forEach(id -> {
            ModelPart root = BaseNativeModelLayers.create(id).bakeRoot();
            assertTrue(root.getAllParts().anyMatch(part -> !part.isEmpty()), id + " baked with no cubes");
        });
        assertTrue(BaseNativeAnimations.ALL.isEmpty(), "Base unexpectedly owns legacy animation clips");
    }
}
