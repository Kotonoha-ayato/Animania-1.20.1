package com.animania.catsdogs;

import com.animania.catsdogs.client.model.CatsDogsNativeAnimations;
import com.animania.catsdogs.client.model.CatsDogsNativeModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Verifies all eight Cats & Dogs legacy native facility models. */
final class CatsDogsNativeModelConversionTest {
    @Test
    void everyNativeFacilityModelBakesAndNoUnownedClipsRemain() {
        assertEquals(8, CatsDogsNativeModelLayers.LAYERS.size());
        CatsDogsNativeModelLayers.LAYERS.keySet().forEach(id -> {
            ModelPart root = CatsDogsNativeModelLayers.create(id).bakeRoot();
            assertTrue(root.getAllParts().anyMatch(part -> !part.isEmpty()), id + " baked with no cubes");
        });
        assertTrue(CatsDogsNativeAnimations.ALL.isEmpty(), "Cats & Dogs unexpectedly owns legacy animation clips");
    }
}
