package com.animania.farm;

import com.animania.common.entity.AnimaniaVehicleEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Server-authoritative boost tool for the pullable Animania vehicles. */
public final class FarmRidingCropItem extends Item {
    public FarmRidingCropItem() {
        super(new Item.Properties().stacksTo(1).durability(100));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if ((player.getVehicle() instanceof AnimaniaVehicleEntity vehicle && vehicle.boost())
                || (player.getVehicle() instanceof com.animania.common.entity.AnimaniaAnimalEntity animal && animal.boost())) {
            if (!level.isClientSide && !player.getAbilities().instabuild) stack.hurtAndBreak(1, player, broken -> player.broadcastBreakEvent(hand));
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        return InteractionResultHolder.pass(stack);
    }
}
