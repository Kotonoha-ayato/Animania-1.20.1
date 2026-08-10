package com.animania.client.model;

import com.animania.common.entity.AnimaniaAnimalEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;

/** Runtime wrapper for the breed-specific native layers converted from 1.12. */
public final class LegacyAnimalModel extends HierarchicalModel<AnimaniaAnimalEntity> {
    private final ModelPart root;

    public LegacyAnimalModel(ModelPart root) {
        this.root = root;
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(AnimaniaAnimalEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        // The converted layers retain every original pivot and default pose.
        // Dynamic 1.12 pose clips are converted separately to AnimationDefinitions.
    }
}
