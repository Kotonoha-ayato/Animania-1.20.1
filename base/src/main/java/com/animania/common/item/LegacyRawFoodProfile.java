package com.animania.common.item;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;

/** Exact shared 1.12 values for every {@code ItemAnimaniaFoodRaw}. */
public record LegacyRawFoodProfile(int nutrition, float saturation, int nauseaTicks, int nauseaAmplifier,
                                   float effectProbability) {
    public static final LegacyRawFoodProfile RAW = new LegacyRawFoodProfile(1, 1.0F, 200, 3, 1.0F);

    public static LegacyRawFoodProfile forItemId(String id) {
        return id != null && id.startsWith("raw_") ? RAW : null;
    }

    public FoodProperties.Builder apply(FoodProperties.Builder builder) {
        return builder.nutrition(nutrition).saturationMod(saturation)
                .effect(() -> new MobEffectInstance(MobEffects.CONFUSION, nauseaTicks, nauseaAmplifier), effectProbability);
    }
}
