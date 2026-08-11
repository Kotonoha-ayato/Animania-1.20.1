package com.animania.common.item;

import com.animania.common.config.AnimaniaConfig;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;

/** Animania food with live 1.12 nutrition overrides and bonus-effect gating. */
public class AnimaniaFoodItem extends Item {
    public AnimaniaFoodItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!isEdible()) return super.use(level, player, hand);
        if (!player.canEat(AnimaniaConfig.eatFoodAnytime())) return InteractionResultHolder.fail(stack);
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        FoodProperties food = getFoodProperties();
        int countBefore = stack.getCount();
        ItemStack consumptionRemainder = hasCraftingRemainingItem(stack)
                ? getCraftingRemainingItem(stack)
                : ItemStack.EMPTY;
        AnimaniaConfig.FoodValueOverride override = AnimaniaConfig.foodValueOverride(stack).orElse(null);
        int foodBefore = 0;
        float saturationBefore = 0.0F;
        Map<MobEffect, MobEffectInstance> effectsBefore = new HashMap<>();
        if (!level.isClientSide && entity instanceof Player player && food != null) {
            foodBefore = player.getFoodData().getFoodLevel();
            saturationBefore = player.getFoodData().getSaturationLevel();
            if (!AnimaniaConfig.foodsGiveBonusEffects()) {
                player.getActiveEffects().forEach(previous ->
                        effectsBefore.put(previous.getEffect(), new MobEffectInstance(previous)));
            }
        }

        ItemStack result = super.finishUsingItem(stack, level, entity);
        if (!level.isClientSide && entity instanceof Player player && food != null) {
            if (override != null) {
                int targetFood = Math.min(20, foodBefore + override.nutrition());
                float targetSaturation = Math.min(targetFood, saturationBefore
                        + override.nutrition() * override.saturationModifier() * 2.0F);
                player.getFoodData().setFoodLevel(targetFood);
                player.getFoodData().setSaturation(targetSaturation);
            }
            if (!AnimaniaConfig.foodsGiveBonusEffects()) {
                player.removeAllEffects();
                effectsBefore.values().forEach(player::addEffect);
            }
        }
        // Item.Properties#craftRemainder is otherwise only honored by crafting
        // grids.  The legacy foods also returned their bottle/bowl when they
        // were consumed, so provide the modern vanilla-style remainder here.
        // Creative players do not consume the food and therefore receive no
        // duplicate container.
        if (!level.isClientSide && result.getCount() < countBefore && !consumptionRemainder.isEmpty()) {
            if (result.isEmpty()) return consumptionRemainder;
            if (entity instanceof Player player && !player.getInventory().add(consumptionRemainder)) {
                player.drop(consumptionRemainder, false);
            }
        }
        return result;
    }
}
