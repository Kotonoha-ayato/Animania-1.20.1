package com.animania.client;

import com.animania.client.model.AnimaniaAnimations;
import com.animania.client.model.AnimaniaAnimalModel;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.model.geom.ModelPart;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AnimaniaAnimationsTest {
    @Test
    void allConvertedClipsUseNativeDefinitionsAndStableBones() {
        Map<String, AnimationDefinition> clips = Map.of(
                "walk", AnimaniaAnimations.WALK,
                "run", AnimaniaAnimations.RUN,
                "sleep", AnimaniaAnimations.SLEEP,
                "eat", AnimaniaAnimations.EAT,
                "drink", AnimaniaAnimations.DRINK,
                "play", AnimaniaAnimations.PLAY,
                "breed", AnimaniaAnimations.BREED,
                "graze", AnimaniaAnimations.GRAZE);
        assertEquals(8, clips.size());
        assertTrue(AnimaniaAnimations.WALK.looping());
        assertTrue(clips.values().stream().allMatch(animation -> animation.lengthInSeconds() > 0.0F));
        assertTrue(AnimaniaAnimations.WALK.boneAnimations().containsKey("leg_front_left"));
        assertTrue(AnimaniaAnimations.SLEEP.boneAnimations().containsKey("body"));
    }

    @Test
    void nativeLayerContainsLegacySilhouetteBranches() {
        ModelPart root = AnimaniaAnimalModel.createBodyLayer().bakeRoot();
        for (String name : new String[]{
                "body", "head", "leg_front_left", "leg_front_right",
                "leg_back_left", "leg_back_right", "tail", "ear_left", "ear_right",
                "muzzle", "horn_left", "horn_right", "wing_left", "wing_right"}) {
            assertDoesNotThrow(() -> root.getChild(name), name);
        }
    }
}
