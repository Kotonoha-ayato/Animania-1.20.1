package com.animania.farm.client.model;

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

public final class FarmNativeAnimations {
    public static final Map<String, AnimationDefinition> ALL = new LinkedHashMap<>();
    static {
        ALL.put("anim_bees", anim_bees());
        ALL.put("anim_bees_wild", anim_bees_wild());
        ALL.put("anim_cart", anim_cart());
        ALL.put("anim_cart_chest", anim_cart_chest());
        ALL.put("anim_tiller", anim_tiller());
        ALL.put("anim_wagon", anim_wagon());
    }
    private FarmNativeAnimations() {}
    private static AnimationDefinition anim_bees() {
        AnimationDefinition.Builder builder = AnimationDefinition.Builder.withLength(3.2F).looping();
        builder.addAnimation("bee_node1", new AnimationChannel(ROTATION, new Keyframe(0.0F, degreeVec(2.0F, 0.0F, 0.0F), LINEAR), new Keyframe(0.8F, degreeVec(-4.980925F, 89.56313F, 5.019002F), LINEAR), new Keyframe(1.6F, degreeVec(-12.0F, 180.0F, 0.0F), LINEAR), new Keyframe(2.4F, degreeVec(-7.483239F, -119.525F, -4.366007F), LINEAR), new Keyframe(3.2F, degreeVec(2.0F, 0.0F, 0.0F), LINEAR)));
        builder.addAnimation("bee_node2", new AnimationChannel(ROTATION, new Keyframe(0.0F, degreeVec(-25.0F, -0.000005F, 0.000001F), LINEAR), new Keyframe(0.8F, degreeVec(-21.91194F, 92.01673F, -5.390444F), LINEAR), new Keyframe(1.6F, degreeVec(-19.07332F, 170.3371F, -0.917549F), LINEAR), new Keyframe(2.4F, degreeVec(-21.04792F, -101.9373F, 5.276817F), LINEAR), new Keyframe(3.2F, degreeVec(-25.0F, -0.000005F, 0.000001F), LINEAR)));
        return builder.build();
    }
    private static AnimationDefinition anim_bees_wild() {
        AnimationDefinition.Builder builder = AnimationDefinition.Builder.withLength(3.2F).looping();
        builder.addAnimation("bee_node1", new AnimationChannel(ROTATION, new Keyframe(0.0F, degreeVec(2.0F, 0.0F, 0.0F), LINEAR), new Keyframe(0.8F, degreeVec(-6.980925F, 89.56313F, 5.019002F), LINEAR), new Keyframe(1.6F, degreeVec(-16.0F, 180.0F, 0.0F), LINEAR), new Keyframe(2.4F, degreeVec(-7.483239F, -119.525F, -4.366007F), LINEAR), new Keyframe(3.2F, degreeVec(2.0F, 0.0F, 0.0F), LINEAR)));
        builder.addAnimation("bee_node2", new AnimationChannel(ROTATION, new Keyframe(0.0F, degreeVec(-27.0F, -0.000005F, 0.000001F), LINEAR), new Keyframe(0.8F, degreeVec(-22.91194F, 92.01673F, -5.390444F), LINEAR), new Keyframe(1.6F, degreeVec(-16.07332F, 170.3371F, -0.917549F), LINEAR), new Keyframe(2.4F, degreeVec(-22.04792F, -101.9373F, 5.276817F), LINEAR), new Keyframe(3.2F, degreeVec(-27.0F, -0.000005F, 0.000001F), LINEAR)));
        return builder.build();
    }
    private static AnimationDefinition anim_cart() {
        AnimationDefinition.Builder builder = AnimationDefinition.Builder.withLength(3.2F).looping();
        builder.addAnimation("wheel1", new AnimationChannel(POSITION, new Keyframe(1.6F, posVec(0.0F, 0.0F, 0.0F), LINEAR), new Keyframe(2.0F, posVec(0.0F, 0.5F, 0.0F), LINEAR), new Keyframe(2.4F, posVec(0.0F, 0.0F, 0.0F), LINEAR)));
        builder.addAnimation("wheel1", new AnimationChannel(ROTATION, new Keyframe(0.0F, degreeVec(0.0F, -0.000014F, 0.0F), LINEAR), new Keyframe(0.4F, degreeVec(0.0F, -0.000014F, 22.5F), LINEAR), new Keyframe(0.8F, degreeVec(-360.0F, -0.000024F, 45.0F), LINEAR), new Keyframe(1.2F, degreeVec(360.0F, -0.000024F, 67.5F), LINEAR), new Keyframe(1.6F, degreeVec(0.0F, -0.000024F, 89.99999F), LINEAR), new Keyframe(2.0F, degreeVec(-360.0F, -0.000024F, 112.5F), LINEAR), new Keyframe(2.4F, degreeVec(360.0F, -0.000024F, 135.0F), LINEAR), new Keyframe(2.8F, degreeVec(360.0F, -0.000024F, 157.5F), LINEAR), new Keyframe(3.2F, degreeVec(0.0F, -0.000024F, -180.0F), LINEAR)));
        builder.addAnimation("wheel2", new AnimationChannel(POSITION, new Keyframe(0.8F, posVec(0.0F, 0.0F, 0.0F), LINEAR), new Keyframe(1.2F, posVec(0.0F, 0.5F, 0.0F), LINEAR), new Keyframe(1.6F, posVec(0.0F, 0.0F, 0.0F), LINEAR)));
        builder.addAnimation("wheel2", new AnimationChannel(ROTATION, new Keyframe(0.0F, degreeVec(0.0F, 0.0F, 0.0F), LINEAR), new Keyframe(0.4F, degreeVec(0.0F, 0.0F, -22.5F), LINEAR), new Keyframe(0.8F, degreeVec(0.0F, 0.0F, -45.0F), LINEAR), new Keyframe(1.2F, degreeVec(0.0F, 0.0F, -67.5F), LINEAR), new Keyframe(1.6F, degreeVec(0.0F, 0.0F, -89.99999F), LINEAR), new Keyframe(2.0F, degreeVec(0.0F, 0.0F, -112.5F), LINEAR), new Keyframe(2.4F, degreeVec(0.0F, 0.0F, -135.0F), LINEAR), new Keyframe(2.8F, degreeVec(0.0F, 0.0F, -157.5F), LINEAR), new Keyframe(3.2F, degreeVec(0.0F, 0.0F, 180.0F), LINEAR)));
        builder.addAnimation("bottom", new AnimationChannel(POSITION, new Keyframe(0.0F, posVec(0.0F, 0.0F, 0.0F), LINEAR), new Keyframe(0.4F, posVec(0.0F, -0.75F, 0.0F), LINEAR), new Keyframe(0.8F, posVec(0.0F, 0.0F, 0.0F), LINEAR), new Keyframe(1.6F, posVec(0.0F, 0.0F, 0.0F), LINEAR), new Keyframe(2.4F, posVec(0.0F, -0.5F, 0.0F), LINEAR), new Keyframe(3.2F, posVec(0.0F, 0.0F, 0.0F), LINEAR)));
        builder.addAnimation("bottom", new AnimationChannel(ROTATION, new Keyframe(0.4F, degreeVec(0.0F, 0.0F, 0.38F), LINEAR), new Keyframe(0.8F, degreeVec(0.0F, 0.0F, 0.5F), LINEAR), new Keyframe(2.4F, degreeVec(0.0F, 0.0F, -0.5F), LINEAR)));
        return builder.build();
    }
    private static AnimationDefinition anim_cart_chest() {
        AnimationDefinition.Builder builder = AnimationDefinition.Builder.withLength(3.2F).looping();
        builder.addAnimation("wheel1", new AnimationChannel(POSITION, new Keyframe(1.6F, posVec(0.0F, 0.0F, 0.0F), LINEAR), new Keyframe(2.0F, posVec(0.0F, 0.5F, 0.0F), LINEAR), new Keyframe(2.4F, posVec(0.0F, 0.0F, 0.0F), LINEAR)));
        builder.addAnimation("wheel1", new AnimationChannel(ROTATION, new Keyframe(0.0F, degreeVec(0.0F, -0.000014F, 0.0F), LINEAR), new Keyframe(0.4F, degreeVec(0.0F, -0.000014F, 22.5F), LINEAR), new Keyframe(0.8F, degreeVec(-360.0F, -0.000024F, 45.0F), LINEAR), new Keyframe(1.2F, degreeVec(360.0F, -0.000024F, 67.5F), LINEAR), new Keyframe(1.6F, degreeVec(0.0F, -0.000024F, 89.99999F), LINEAR), new Keyframe(2.0F, degreeVec(-360.0F, -0.000024F, 112.5F), LINEAR), new Keyframe(2.4F, degreeVec(360.0F, -0.000024F, 135.0F), LINEAR), new Keyframe(2.8F, degreeVec(360.0F, -0.000024F, 157.5F), LINEAR), new Keyframe(3.2F, degreeVec(0.0F, -0.000024F, -180.0F), LINEAR)));
        builder.addAnimation("wheel2", new AnimationChannel(POSITION, new Keyframe(0.8F, posVec(0.0F, 0.0F, 0.0F), LINEAR), new Keyframe(1.2F, posVec(0.0F, 0.5F, 0.0F), LINEAR), new Keyframe(1.6F, posVec(0.0F, 0.0F, 0.0F), LINEAR)));
        builder.addAnimation("wheel2", new AnimationChannel(ROTATION, new Keyframe(0.0F, degreeVec(0.0F, 0.0F, 0.0F), LINEAR), new Keyframe(0.4F, degreeVec(0.0F, 0.0F, -22.5F), LINEAR), new Keyframe(0.8F, degreeVec(0.0F, 0.0F, -45.0F), LINEAR), new Keyframe(1.2F, degreeVec(0.0F, 0.0F, -67.5F), LINEAR), new Keyframe(1.6F, degreeVec(0.0F, 0.0F, -89.99999F), LINEAR), new Keyframe(2.0F, degreeVec(0.0F, 0.0F, -112.5F), LINEAR), new Keyframe(2.4F, degreeVec(0.0F, 0.0F, -135.0F), LINEAR), new Keyframe(2.8F, degreeVec(0.0F, 0.0F, -157.5F), LINEAR), new Keyframe(3.2F, degreeVec(0.0F, 0.0F, 180.0F), LINEAR)));
        builder.addAnimation("bottom", new AnimationChannel(POSITION, new Keyframe(0.0F, posVec(0.0F, 0.0F, 0.0F), LINEAR), new Keyframe(0.4F, posVec(0.0F, -0.75F, 0.0F), LINEAR), new Keyframe(0.8F, posVec(0.0F, 0.0F, 0.0F), LINEAR), new Keyframe(1.6F, posVec(0.0F, 0.0F, 0.0F), LINEAR), new Keyframe(2.4F, posVec(0.0F, -0.5F, 0.0F), LINEAR), new Keyframe(3.2F, posVec(0.0F, 0.0F, 0.0F), LINEAR)));
        builder.addAnimation("bottom", new AnimationChannel(ROTATION, new Keyframe(0.4F, degreeVec(0.0F, 0.0F, 0.38F), LINEAR), new Keyframe(0.8F, degreeVec(0.0F, 0.0F, 0.5F), LINEAR), new Keyframe(2.4F, degreeVec(0.0F, 0.0F, -0.5F), LINEAR)));
        builder.addAnimation("chest1", new AnimationChannel(POSITION, new Keyframe(0.0F, posVec(0.0F, 0.0F, 0.0F), LINEAR), new Keyframe(0.4F, posVec(0.0F, -0.75F, 0.0F), LINEAR), new Keyframe(0.8F, posVec(0.0F, 0.0F, 0.0F), LINEAR), new Keyframe(1.6F, posVec(0.0F, 0.0F, 0.0F), LINEAR), new Keyframe(2.4F, posVec(0.0F, -0.5F, 0.0F), LINEAR), new Keyframe(3.2F, posVec(0.0F, 0.0F, 0.0F), LINEAR)));
        return builder.build();
    }
    private static AnimationDefinition anim_tiller() {
        AnimationDefinition.Builder builder = AnimationDefinition.Builder.withLength(3.2F).looping();
        builder.addAnimation("wheel1_axle", new AnimationChannel(ROTATION, new Keyframe(0.0F, degreeVec(-0.000011F, 0.0F, 0.0F), LINEAR), new Keyframe(0.4F, degreeVec(22.49999F, 0.0F, 0.0F), LINEAR), new Keyframe(0.8F, degreeVec(45.0F, 0.0F, 0.0F), LINEAR), new Keyframe(1.2F, degreeVec(67.49999F, 0.0F, 0.0F), LINEAR), new Keyframe(1.6F, degreeVec(90.0F, 0.0F, 0.0F), LINEAR), new Keyframe(2.0F, degreeVec(67.50001F, -180.0F, -180.0F), LINEAR), new Keyframe(2.4F, degreeVec(45.00002F, -180.0F, -180.0F), LINEAR), new Keyframe(2.8F, degreeVec(22.50001F, -180.0F, -180.0F), LINEAR), new Keyframe(3.2F, degreeVec(-0.000011F, 180.0F, 180.0F), LINEAR)));
        builder.addAnimation("wheel2_axle", new AnimationChannel(ROTATION, new Keyframe(0.0F, degreeVec(-0.000011F, 0.0F, 0.0F), LINEAR), new Keyframe(0.4F, degreeVec(22.49999F, 0.0F, 0.0F), LINEAR), new Keyframe(0.8F, degreeVec(45.0F, 0.0F, 0.0F), LINEAR), new Keyframe(1.2F, degreeVec(67.49999F, 0.0F, 0.0F), LINEAR), new Keyframe(1.6F, degreeVec(90.0F, 0.0F, 0.0F), LINEAR), new Keyframe(2.0F, degreeVec(67.50001F, -180.0F, -180.0F), LINEAR), new Keyframe(2.4F, degreeVec(45.00002F, -180.0F, -180.0F), LINEAR), new Keyframe(2.8F, degreeVec(22.50001F, -180.0F, -180.0F), LINEAR), new Keyframe(3.2F, degreeVec(0.000011F, 180.0F, 180.0F), LINEAR)));
        builder.addAnimation("tiller_base", new AnimationChannel(POSITION, new Keyframe(0.0F, posVec(0.0F, 0.0F, 0.0F), LINEAR), new Keyframe(2.4F, posVec(0.0F, 0.700001F, 0.0F), LINEAR)));
        builder.addAnimation("fastener", new AnimationChannel(POSITION, new Keyframe(0.0F, posVec(0.0F, 0.0F, 0.0F), LINEAR), new Keyframe(2.4F, posVec(0.0F, 0.7F, 0.0F), LINEAR)));
        return builder.build();
    }
    private static AnimationDefinition anim_wagon() {
        AnimationDefinition.Builder builder = AnimationDefinition.Builder.withLength(6.4F).looping();
        builder.addAnimation("front_weel_axe", new AnimationChannel(POSITION, new Keyframe(0.0F, posVec(0.0F, 0.0F, -0.36F), LINEAR), new Keyframe(0.4F, posVec(0.0F, 0.0F, -0.86F), LINEAR), new Keyframe(1.6F, posVec(0.0F, 0.0F, 0.14F), LINEAR), new Keyframe(2.4F, posVec(0.0F, 0.0F, -0.86F), LINEAR)));
        builder.addAnimation("front_weel_axe", new AnimationChannel(ROTATION, new Keyframe(0.0F, degreeVec(0.0F, 0.0F, 0.0F), LINEAR), new Keyframe(0.4F, degreeVec(22.5F, 0.0F, 0.0F), LINEAR), new Keyframe(0.8F, degreeVec(45.0F, 0.0F, 0.0F), LINEAR), new Keyframe(1.2F, degreeVec(67.49999F, 0.0F, 0.0F), LINEAR), new Keyframe(1.6F, degreeVec(90.0F, 0.0F, 0.0F), LINEAR), new Keyframe(2.0F, degreeVec(67.49999F, -180.0F, -180.0F), LINEAR), new Keyframe(2.4F, degreeVec(45.0F, -180.0F, -180.0F), LINEAR), new Keyframe(2.8F, degreeVec(22.5F, 180.0F, 180.0F), LINEAR), new Keyframe(3.2F, degreeVec(0.0F, -180.0F, -180.0F), LINEAR), new Keyframe(3.6F, degreeVec(-22.5F, -180.0F, 180.0F), LINEAR), new Keyframe(4.0F, degreeVec(-45.0F, -180.0F, -180.0F), LINEAR), new Keyframe(4.4F, degreeVec(-67.49999F, -180.0F, -180.0F), LINEAR), new Keyframe(4.8F, degreeVec(-90.0F, 0.0F, 0.0F), LINEAR), new Keyframe(5.2F, degreeVec(-67.49999F, 0.0F, 0.0F), LINEAR), new Keyframe(5.6F, degreeVec(-45.0F, 0.0F, 0.0F), LINEAR), new Keyframe(6.0F, degreeVec(-22.5F, 0.0F, 0.0F), LINEAR), new Keyframe(6.4F, degreeVec(0.0F, 0.0F, 0.0F), LINEAR)));
        builder.addAnimation("weels_axe_back", new AnimationChannel(POSITION, new Keyframe(1.6F, posVec(0.0F, -0.000004F, -0.660002F), LINEAR), new Keyframe(2.4F, posVec(0.0F, -0.000004F, 0.839998F), LINEAR)));
        builder.addAnimation("weels_axe_back", new AnimationChannel(ROTATION, new Keyframe(0.0F, degreeVec(-0.00001F, 0.0F, 0.0F), LINEAR), new Keyframe(0.4F, degreeVec(22.49998F, 0.0F, 0.0F), LINEAR), new Keyframe(0.8F, degreeVec(44.99998F, 0.0F, 0.0F), LINEAR), new Keyframe(1.2F, degreeVec(67.49996F, 0.0F, 0.0F), LINEAR), new Keyframe(1.6F, degreeVec(90.0F, 0.0F, 0.0F), LINEAR), new Keyframe(2.0F, degreeVec(67.49998F, 180.0F, 180.0F), LINEAR), new Keyframe(2.4F, degreeVec(45.0F, 180.0F, 180.0F), LINEAR), new Keyframe(2.8F, degreeVec(22.5F, 180.0F, 180.0F), LINEAR), new Keyframe(3.2F, degreeVec(0.00001F, 180.0F, -180.0F), LINEAR), new Keyframe(3.6F, degreeVec(-22.49998F, -180.0F, 180.0F), LINEAR), new Keyframe(4.0F, degreeVec(-44.99998F, 180.0F, -180.0F), LINEAR), new Keyframe(4.4F, degreeVec(-67.49996F, 180.0F, -180.0F), LINEAR), new Keyframe(4.8F, degreeVec(-90.0F, 0.0F, 0.0F), LINEAR), new Keyframe(5.2F, degreeVec(-67.49998F, 0.0F, 0.0F), LINEAR), new Keyframe(5.6F, degreeVec(-45.0F, 0.0F, 0.0F), LINEAR), new Keyframe(6.0F, degreeVec(-22.5F, 0.0F, 0.0F), LINEAR), new Keyframe(6.4F, degreeVec(-0.00001F, 0.0F, 0.0F), LINEAR)));
        builder.addAnimation("ground", new AnimationChannel(POSITION, new Keyframe(0.0F, posVec(0.0F, 0.0F, 0.0F), LINEAR), new Keyframe(0.5F, posVec(0.0F, -0.5F, 0.0F), LINEAR), new Keyframe(1.5F, posVec(0.0F, 0.0F, 0.0F), LINEAR), new Keyframe(2.5F, posVec(0.0F, -0.5F, 0.0F), LINEAR)));
        builder.addAnimation("ground", new AnimationChannel(ROTATION, new Keyframe(0.0F, degreeVec(0.0F, 0.0F, 0.0F), LINEAR), new Keyframe(0.5F, degreeVec(1.0F, 0.0F, 0.0F), LINEAR), new Keyframe(1.5F, degreeVec(-1.0F, 0.0F, 0.0F), LINEAR), new Keyframe(2.5F, degreeVec(0.0F, 0.0F, 0.0F), LINEAR)));
        builder.addAnimation("maint_back_support", new AnimationChannel(POSITION, new Keyframe(0.0F, posVec(0.0F, 0.0F, 0.0F), LINEAR), new Keyframe(1.5F, posVec(0.0F, -1.0F, 0.0F), LINEAR)));
        builder.addAnimation("maint_front_support", new AnimationChannel(POSITION, new Keyframe(0.0F, posVec(0.0F, 0.0F, 0.0F), LINEAR), new Keyframe(0.5F, posVec(0.0F, -1.0F, 0.0F), LINEAR), new Keyframe(1.5F, posVec(0.0F, 0.0F, 0.0F), LINEAR)));
        return builder.build();
    }
}
