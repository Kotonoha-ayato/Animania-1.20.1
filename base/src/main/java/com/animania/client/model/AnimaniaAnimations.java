package com.animania.client.model;

import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.Keyframe;

import static net.minecraft.client.animation.AnimationChannel.Interpolations.LINEAR;
import static net.minecraft.client.animation.AnimationChannel.Targets.ROTATION;
import static net.minecraft.client.animation.KeyframeAnimations.degreeVec;

/**
 * Native 1.20.1 animation definitions replacing the 1.12 CraftStudio clips.
 *
 * The definitions deliberately use the same stable bone names as
 * {@link AnimaniaAnimalModel}.  Addons can reference these clips without a
 * client-only animation library, while the shared model remains small enough
 * for all legacy breeds.
 */
public final class AnimaniaAnimations {
    public static final AnimationDefinition WALK = walk(1.0F, 1.0F);
    public static final AnimationDefinition RUN = walk(0.65F, 1.55F);
    public static final AnimationDefinition SLEEP = AnimationDefinition.Builder.withLength(1.0F)
            .addAnimation("body", new AnimationChannel(ROTATION,
                    frame(0.0F, 0.0F, 7.0F, 0.0F)))
            .addAnimation("head", new AnimationChannel(ROTATION,
                    frame(0.0F, 12.0F, 0.0F, 0.0F)))
            .build();
    public static final AnimationDefinition EAT = headMotion(0.75F, 18.0F);
    public static final AnimationDefinition DRINK = headMotion(0.8F, 28.0F);
    public static final AnimationDefinition PLAY = bodyMotion(0.9F, 10.0F);
    public static final AnimationDefinition BREED = bodyMotion(0.7F, 16.0F);
    public static final AnimationDefinition GRAZE = headMotion(1.2F, 32.0F);

    private AnimaniaAnimations() {
    }

    private static AnimationDefinition walk(float length, float amplitude) {
        return AnimationDefinition.Builder.withLength(length).looping()
                .addAnimation("leg_front_left", new AnimationChannel(ROTATION,
                        frame(0.0F, 25.0F * amplitude, 0.0F, 0.0F),
                        frame(length / 2.0F, -25.0F * amplitude, 0.0F, 0.0F),
                        frame(length, 25.0F * amplitude, 0.0F, 0.0F)))
                .addAnimation("leg_back_right", new AnimationChannel(ROTATION,
                        frame(0.0F, 25.0F * amplitude, 0.0F, 0.0F),
                        frame(length / 2.0F, -25.0F * amplitude, 0.0F, 0.0F),
                        frame(length, 25.0F * amplitude, 0.0F, 0.0F)))
                .addAnimation("leg_front_right", new AnimationChannel(ROTATION,
                        frame(0.0F, -25.0F * amplitude, 0.0F, 0.0F),
                        frame(length / 2.0F, 25.0F * amplitude, 0.0F, 0.0F),
                        frame(length, -25.0F * amplitude, 0.0F, 0.0F)))
                .addAnimation("leg_back_left", new AnimationChannel(ROTATION,
                        frame(0.0F, -25.0F * amplitude, 0.0F, 0.0F),
                        frame(length / 2.0F, 25.0F * amplitude, 0.0F, 0.0F),
                        frame(length, -25.0F * amplitude, 0.0F, 0.0F)))
                .build();
    }

    private static AnimationDefinition headMotion(float length, float pitch) {
        return AnimationDefinition.Builder.withLength(length).looping()
                .addAnimation("head", new AnimationChannel(ROTATION,
                        frame(0.0F, pitch, 0.0F, 0.0F),
                        frame(length / 2.0F, -pitch, 0.0F, 0.0F),
                        frame(length, pitch, 0.0F, 0.0F)))
                .build();
    }

    private static AnimationDefinition bodyMotion(float length, float roll) {
        return AnimationDefinition.Builder.withLength(length).looping()
                .addAnimation("body", new AnimationChannel(ROTATION,
                        frame(0.0F, 0.0F, roll, 0.0F),
                        frame(length / 2.0F, 0.0F, -roll, 0.0F),
                        frame(length, 0.0F, roll, 0.0F)))
                .build();
    }

    private static Keyframe frame(float timestamp, float x, float y, float z) {
        return new Keyframe(timestamp, degreeVec(x, y, z), LINEAR);
    }
}
