package com.animania.farm;

import com.animania.common.item.AnimaniaFoodItem;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

/** Drinkable Animania milk bottle with the 1.12 container and cure semantics. */
public final class FarmMilkBottleItem extends AnimaniaFoodItem {
    public FarmMilkBottleItem() {
        super(new Item.Properties().stacksTo(4)
                .food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(4).saturationMod(1.0F).alwaysEat().build())
                .craftRemainder(net.minecraft.world.item.Items.GLASS_BOTTLE));
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.DRINK; }

    @Override
    public int getUseDuration(ItemStack stack) { return 32; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack result = super.finishUsingItem(stack, level, entity);
        if (!level.isClientSide && entity instanceof Player player) player.removeAllEffects();
        return result;
    }
}
