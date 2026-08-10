package com.animania.extra;

import com.animania.common.block.AnimaniaStorageBlockEntity;
import com.animania.common.entity.AnimaniaAnimalEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * FE-capable hamster wheel.  A nearby Animania hamster runs server-side,
 * consumes hamster food from the inventory at the configured interval, and
 * generates FE through the standard Forge capability.
 */
public final class ExtraHamsterWheelBlockEntity extends AnimaniaStorageBlockEntity {
    private final EnergyStorage energy = new EnergyStorage(ExtraConfig.HAMSTER_WHEEL_CAPACITY.get(),
            ExtraConfig.HAMSTER_WHEEL_GENERATION.get(), ExtraConfig.HAMSTER_WHEEL_GENERATION.get());
    private final LazyOptional<EnergyStorage> energyOptional = LazyOptional.of(() -> energy);
    private int useTicks;
    private boolean running;

    public ExtraHamsterWheelBlockEntity(BlockPos pos, BlockState state) {
        super(ExtraContent.HAMSTER_WHEEL_BE.get(), pos, state);
    }

    @Override
    public void serverTick() {
        AnimaniaAnimalEntity hamster = findHamster();
        running = hamster != null && hamster.getHunger() > 0;
        if (!running) {
            useTicks = 0;
            return;
        }
        energy.receiveEnergy(ExtraConfig.HAMSTER_WHEEL_GENERATION.get(), false);
        if (++useTicks >= ExtraConfig.HAMSTER_WHEEL_USE_TIME.get()) {
            useTicks = 0;
            ItemStack food = getItem(0);
            if (!food.isEmpty() && isHamsterFood(food)) {
                setItem(0, new ItemStack(food.getItem(), food.getCount() - 1));
            } else {
                hamster.setHunger(Math.max(0, hamster.getHunger() - 20));
            }
        }
        setChanged();
    }

    private AnimaniaAnimalEntity findHamster() {
        if (level == null) return null;
        return level.getEntitiesOfClass(AnimaniaAnimalEntity.class,
                        new AABB(worldPosition).inflate(1.5D)).stream()
                .filter(entity -> {
                    ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
                    return id != null && AnimaniaExtra.MOD_ID.equals(id.getNamespace()) && "hamster".equals(id.getPath());
                })
                .findFirst().orElse(null);
    }

    private static boolean isHamsterFood(ItemStack stack) {
        net.minecraft.world.item.Item food = ForgeRegistries.ITEMS.getValue(new ResourceLocation(AnimaniaExtra.MOD_ID, "hamster_food"));
        return food != null && stack.is(food);
    }

    public boolean isRunning() {
        return running;
    }

    public int energyStored() {
        return energy.getEnergyStored();
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, net.minecraft.core.Direction side) {
        if (capability == ForgeCapabilities.ENERGY) return energyOptional.cast();
        return super.getCapability(capability, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        energyOptional.invalidate();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("Energy", energy.getEnergyStored());
        tag.putInt("UseTicks", useTicks);
        tag.putBoolean("Running", running);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        energy.receiveEnergy(Math.max(0, tag.getInt("Energy")), false);
        useTicks = Math.max(0, tag.getInt("UseTicks"));
        running = tag.getBoolean("Running");
    }
}
