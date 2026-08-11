package com.animania.common.item;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
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
        if (!allowed()) return InteractionResultHolder.fail(stack);
        if (!level.isClientSide && !spawn(level, player, stack,
                player.getX(), player.getY(), player.getZ())) {
            return InteractionResultHolder.fail(stack);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public net.minecraft.world.InteractionResult useOn(UseOnContext context) {
        ItemStack stack = context.getItemInHand();
        Player player = context.getPlayer();
        if (player == null || !allowed()) return net.minecraft.world.InteractionResult.FAIL;
        var target = context.getClickedPos().relative(context.getClickedFace());
        if (!context.getLevel().isClientSide && !spawn(context.getLevel(), player, stack,
                target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D)) {
            return net.minecraft.world.InteractionResult.FAIL;
        }
        return net.minecraft.world.InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }

    private boolean allowed() {
        try {
            return AnimaniaConfig.ENABLE_VEHICLES.get() && enabled.getAsBoolean();
        } catch (RuntimeException ignored) {
            return enabled.getAsBoolean();
        }
    }

    private boolean spawn(Level level, Player player, ItemStack stack,
                          double x, double y, double z) {
        Entity entity = entityType.get().create(level);
        if (entity == null) return false;
        entity.moveTo(x, y, z, player.getYRot(), 0.0F);
        if (stack.hasCustomHoverName()) entity.setCustomName(stack.getHoverName());
        if (!level.addFreshEntity(entity)) return false;
        if (!player.getAbilities().instabuild) stack.shrink(1);
        player.playSound(SoundEvents.WOOD_PLACE, 0.8F, 1.0F);
        return true;
    }
}
