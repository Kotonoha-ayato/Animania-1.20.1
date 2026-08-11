package com.animania.farm;

import com.animania.client.model.LegacyAnimalModel;
import com.animania.client.model.LegacyAnimationProfile;
import com.animania.farm.client.model.FarmNativeAnimations;
import com.animania.farm.client.model.FarmNativeModelLayers;
import com.animania.farm.client.model.FarmLegacyPropModels;
import net.minecraft.client.model.geom.ModelPart;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Covers all six Farm legacy native models and their six converted clips. */
final class FarmNativeModelConversionTest {
    private static final Map<String, String> CLIP_MODELS = Map.of(
            "anim_bees", "model_bee_hive",
            "anim_bees_wild", "model_wild_hive",
            "anim_cart", "model_cart",
            "anim_cart_chest", "model_cart_chest",
            "anim_tiller", "model_tiller",
            "anim_wagon", "model_wagon");

    @Test
    void everyNativeModelAndAnimationBoneResolves() {
        assertEquals(6, FarmNativeModelLayers.LAYERS.size());
        assertEquals(6, FarmNativeAnimations.ALL.size());
        FarmNativeModelLayers.LAYERS.keySet().forEach(id -> assertGeometry(FarmNativeModelLayers.create(id).bakeRoot(), id));
        FarmNativeAnimations.ALL.forEach((clip, animation) -> {
            ModelPart root = FarmNativeModelLayers.create(CLIP_MODELS.get(clip)).bakeRoot();
            LegacyAnimalModel model = new LegacyAnimalModel(root, LegacyAnimationProfile.EMPTY);
            animation.boneAnimations().keySet().forEach(bone ->
                    assertTrue(model.getAnyDescendantWithName(bone).isPresent(), clip + " references missing bone " + bone));
        });
    }

    @Test
    void bothHiveModelsUseExactCraftStudioTopology() {
        for (String id : new String[]{"model_bee_hive", "model_wild_hive"}) {
            ModelPart exact = FarmLegacyPropModels.create(id);
            assertTrue(exact.getAllParts().anyMatch(part -> !part.isEmpty()), id + " exact mesh has no geometry");
            assertTrue(exact.getAllParts().count() > 10, id + " lost its legacy part hierarchy");
        }
    }

    private static void assertGeometry(ModelPart root, String id) {
        assertTrue(root.getAllParts().anyMatch(part -> !part.isEmpty()), id + " baked with no cubes");
    }
}
