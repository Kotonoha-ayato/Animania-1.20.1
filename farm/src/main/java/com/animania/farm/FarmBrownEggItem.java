package com.animania.farm;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Throwable brown egg with a configurable, synchronized hatch projectile. */
public final class FarmBrownEggItem extends Item {
    public FarmBrownEggItem() {
        super(new Item.Properties().stacksTo(16));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        // The legacy addon kept egg throwing opt-in.  Return PASS without
        // consuming the stack when the server config disables the projectile.
        try {
            if (!FarmConfig.ALLOW_EGG_THROWING.get()) return InteractionResultHolder.pass(stack);
        } catch (RuntimeException ignored) {
            // Datagen/GameTest construction can occur before config binding;
            // use the historical default (disabled) in that window.
            return InteractionResultHolder.pass(stack);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.EGG_THROW, SoundSource.NEUTRAL, 0.5F,
                0.4F / (level.random.nextFloat() * 0.4F + 0.8F));
        if (!level.isClientSide) {
            FarmBrownEggProjectile egg = new FarmBrownEggProjectile(level, player);
            egg.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.5F, 1.0F);
            level.addFreshEntity(egg);
        }
        if (!player.getAbilities().instabuild) stack.shrink(1);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
