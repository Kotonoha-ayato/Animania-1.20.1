package com.animania.common.item;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import com.animania.common.config.AnimaniaConfig;
import java.util.function.BooleanSupplier;

import java.util.function.Supplier;

/** Places one of the native farm vehicle entities from its legacy item. */
public final class AnimaniaVehicleItem extends Item {
    private final Supplier<EntityType<?>> entityType;
    private final BooleanSupplier enabled;

    public AnimaniaVehicleItem(Supplier<EntityType<?>> entityType, Properties properties) {
        this(entityType, properties, () -> true);
    }

    /** Optional addon/config gate evaluated only on the authoritative side. */
    public AnimaniaVehicleItem(Supplier<EntityType<?>> entityType, Properties properties, BooleanSupplier enabled) {
        super(properties);
        this.entityType = entityType;
        this.enabled = enabled == null ? () -> true : enabled;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        try {
            if (!AnimaniaConfig.ENABLE_VEHICLES.get() || !enabled.getAsBoolean()) return InteractionResultHolder.fail(stack);
        } catch (RuntimeException ignored) {
            if (!enabled.getAsBoolean()) return InteractionResultHolder.fail(stack);
        }
        if (!level.isClientSide) {
            Entity entity = entityType.get().create(level);
            if (entity == null) return InteractionResultHolder.fail(stack);
            entity.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), 0.0F);
            level.addFreshEntity(entity);
            if (!player.getAbilities().instabuild) stack.shrink(1);
            player.playSound(SoundEvents.WOOD_PLACE, 0.8F, 1.0F);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
