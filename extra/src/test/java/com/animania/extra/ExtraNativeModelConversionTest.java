package com.animania.extra;

import com.animania.client.model.LegacyAnimalModel;
import com.animania.client.model.LegacyAnimationProfile;
import com.animania.extra.client.model.ExtraNativeAnimations;
import com.animania.extra.client.model.ExtraNativeModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Covers both Extra legacy native models and the remaining two converted clips. */
final class ExtraNativeModelConversionTest {
    private static final Map<String, String> CLIP_MODELS = Map.of(
            "anim_hamster_wheel", "model_hamster_wheel",
            "hamster_run", "hamster");

    @Test
    void everyNativeModelAndAnimationBoneResolves() {
        assertEquals(2, ExtraNativeModelLayers.LAYERS.size());
        assertEquals(2, ExtraNativeAnimations.ALL.size());
        ExtraNativeModelLayers.LAYERS.keySet().forEach(id -> {
            ModelPart root = ExtraNativeModelLayers.create(id).bakeRoot();
            assertTrue(root.getAllParts().anyMatch(part -> !part.isEmpty()), id + " baked with no cubes");
        });
        ExtraNativeAnimations.ALL.forEach((clip, animation) -> {
            ModelPart root = ExtraNativeModelLayers.create(CLIP_MODELS.get(clip)).bakeRoot();
            LegacyAnimalModel model = new LegacyAnimalModel(root, LegacyAnimationProfile.EMPTY);
            animation.boneAnimations().keySet().forEach(bone ->
                    assertTrue(model.getAnyDescendantWithName(bone).isPresent(), clip + " references missing bone " + bone));
        });
    }
}
