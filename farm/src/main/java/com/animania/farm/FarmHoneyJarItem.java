package com.animania.farm;

import com.animania.common.config.AnimaniaConfig;
import com.animania.common.item.AnimaniaFoodItem;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidHandlerItemStack;

import javax.annotation.Nullable;

/** Honey jar food and bottle capability endpoint used by both hive variants. */
public final class FarmHoneyJarItem extends AnimaniaFoodItem {
    public FarmHoneyJarItem() {
        super(new Item.Properties().stacksTo(4)
                .food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(10).saturationMod(1.5F).alwaysEat().build())
                .craftRemainder(net.minecraft.world.item.Items.GLASS_BOTTLE));
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.DRINK; }

    @Override
    public int getUseDuration(ItemStack stack) { return 32; }

    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        return new HoneyFluidHandler(stack);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!player.canEat(true)) return InteractionResultHolder.fail(player.getItemInHand(hand));
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack result = super.finishUsingItem(stack, level, entity);
        if (!level.isClientSide && entity instanceof Player player && bonusEffects()) {
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1));
        }
        return result;
    }

    private static boolean bonusEffects() {
        try { return AnimaniaConfig.FOODS_GIVE_BONUS_EFFECTS.get(); }
        catch (IllegalStateException ignored) { return true; }
    }

    /** Full legacy honey container: exactly 1000 mB, honey-only, swapping to glass when drained. */
    private static final class HoneyFluidHandler extends FluidHandlerItemStack.SwapEmpty {
        private HoneyFluidHandler(ItemStack stack) {
            super(stack, new ItemStack(net.minecraft.world.item.Items.GLASS_BOTTLE), 1000);
            if (getFluid().isEmpty()) {
                var honey = FarmFluids.ALL.get("animania_honey");
                if (honey != null && honey.source.isPresent()) {
                    fill(new FluidStack(honey.source.get(), 1000), IFluidHandler.FluidAction.EXECUTE);
                }
            }
        }

        @Override
        public boolean canFillFluidType(FluidStack fluid) {
            var honey = FarmFluids.ALL.get("animania_honey");
            return honey != null && honey.source.isPresent() && fluid.getFluid() == honey.source.get();
        }
    }
}
