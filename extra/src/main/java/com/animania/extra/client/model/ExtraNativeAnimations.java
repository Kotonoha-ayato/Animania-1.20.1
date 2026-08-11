package com.animania.extra.client.model;

// Generated native AnimationDefinitions from archived legacy native keyframes.
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.Keyframe;
import static net.minecraft.client.animation.AnimationChannel.Interpolations.LINEAR;
import static net.minecraft.client.animation.AnimationChannel.Targets.POSITION;
import static net.minecraft.client.animation.AnimationChannel.Targets.ROTATION;
import static net.minecraft.client.animation.KeyframeAnimations.degreeVec;
import static net.minecraft.client.animation.KeyframeAnimations.posVec;

public final class ExtraNativeAnimations {
    public static final Map<String, AnimationDefinition> ALL = new LinkedHashMap<>();
    static {
        ALL.put("anim_hamster_wheel", anim_hamster_wheel());
        ALL.put("hamster_run", hamster_run());
    }
    private ExtraNativeAnimations() {}
    private static AnimationDefinition anim_hamster_wheel() {
        AnimationDefinition.Builder builder = AnimationDefinition.Builder.withLength(4.0F).looping();
        builder.addAnimation("wheel1", new AnimationChannel(POSITION, new Keyframe(0.0F, posVec(6.5F, 0.000003F, 0.0F), LINEAR)));
        builder.addAnimation("wheel1", new AnimationChannel(ROTATION, new Keyframe(0.0F, degreeVec(0.0F, 0.0F, -0.00001F), LINEAR), new Keyframe(1.0F, degreeVec(0.0F, 0.0F, -89.99999F), LINEAR), new Keyframe(2.0F, degreeVec(0.0F, 0.0F, 180.0F), LINEAR), new Keyframe(3.0F, degreeVec(0.0F, 0.0F, 89.99997F), LINEAR)));
        return builder.build();
    }
    private static AnimationDefinition hamster_run() {
        AnimationDefinition.Builder builder = AnimationDefinition.Builder.withLength(1.0F).looping();
        builder.addAnimation("hamster_body", new AnimationChannel(POSITION, new Keyframe(0.0F, posVec(0.0F, 0.0F, 0.0F), LINEAR), new Keyframe(0.5F, posVec(0.0F, 3.0F, 2.0F), LINEAR), new Keyframe(0.7F, posVec(0.0F, 1.0F, 3.0F), LINEAR)));
        builder.addAnimation("hamster_body", new AnimationChannel(ROTATION, new Keyframe(0.0F, degreeVec(0.0F, 0.0F, 0.0F), LINEAR), new Keyframe(0.5F, degreeVec(-15.0F, 0.0F, 0.0F), LINEAR)));
        builder.addAnimation("hamster_head", new AnimationChannel(ROTATION, new Keyframe(0.0F, degreeVec(0.0F, 0.0F, 0.0F), LINEAR), new Keyframe(0.5F, degreeVec(20.0F, 0.0F, 0.0F), LINEAR)));
        builder.addAnimation("hamsterleg1", new AnimationChannel(POSITION, new Keyframe(0.0F, posVec(0.0F, -0.0F, -1.000001F), LINEAR)));
        builder.addAnimation("hamsterleg1", new AnimationChannel(ROTATION, new Keyframe(0.3F, degreeVec(20.0F, 0.0F, 0.0F), LINEAR), new Keyframe(0.8F, degreeVec(-20.0F, 0.000002F, -0.00001F), LINEAR)));
        builder.addAnimation("hamsterleg3", new AnimationChannel(POSITION, new Keyframe(0.0F, posVec(0.0F, -0.0F, -1.0F), LINEAR)));
        builder.addAnimation("hamsterleg3", new AnimationChannel(ROTATION, new Keyframe(0.3F, degreeVec(15.0F, 0.0F, 0.0F), LINEAR), new Keyframe(0.8F, degreeVec(-15.0F, 0.000001F, -0.00001F), LINEAR)));
        builder.addAnimation("hamster_ear_right", new AnimationChannel(POSITION, new Keyframe(0.0F, posVec(-1.5F, -0.5F, 2.749999F), LINEAR)));
        builder.addAnimation("hamster_ear_right", new AnimationChannel(ROTATION, new Keyframe(0.0F, degreeVec(0.0F, 0.0F, 0.0F), LINEAR), new Keyframe(0.5F, degreeVec(8.499992F, 0.0F, 0.0F), LINEAR)));
        return builder.build();
    }
}
