package com.animania.farm;

import com.animania.common.block.AnimaniaStorageBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.registries.ForgeRegistries;

/** Server-side honey production and Forge fluid capability for both hives. */
public final class FarmHiveBlockEntity extends AnimaniaStorageBlockEntity {
    private final FluidTank honeyTank;
    private final LazyOptional<FluidTank> honeyOptional;
    private int nextHoney;

    public FarmHiveBlockEntity(BlockPos pos, BlockState state) {
        this(FarmContent.HIVE_BE.get(), pos, state);
    }

    public static FarmHiveBlockEntity createHive(BlockPos pos, BlockState state) {
        return new FarmHiveBlockEntity(FarmContent.HIVE_BE.get(), pos, state);
    }

    public static FarmHiveBlockEntity createWildHive(BlockPos pos, BlockState state) {
        return new FarmHiveBlockEntity(FarmContent.WILD_HIVE_BE.get(), pos, state);
    }

    public FarmHiveBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        honeyTank = new FluidTank(FarmConfig.HIVE_CAPACITY.get()) {
            @Override
            protected void onContentsChanged() {
                setChanged();
            }
        };
        honeyOptional = LazyOptional.of(() -> honeyTank);
        nextHoney = isWild() ? FarmConfig.HIVE_WILD_HONEY_RATE.get() : FarmConfig.HIVE_PLAYER_HONEY_RATE.get();
    }

    @Override
    public void serverTick() {
        if (level == null) return;
        if (--nextHoney > 0) return;
        nextHoney = isWild() ? FarmConfig.HIVE_WILD_HONEY_RATE.get() : FarmConfig.HIVE_PLAYER_HONEY_RATE.get();
        var registration = FarmFluids.ALL.get("animania_honey");
        if (registration != null && registration.source.isPresent()) {
            honeyTank.fill(new FluidStack(registration.source.get(), 25), IFluidHandler.FluidAction.EXECUTE);
        }
        if (isWild()) {
            level.getEntitiesOfClass(net.minecraft.world.entity.player.Player.class,
                    new net.minecraft.world.phys.AABB(worldPosition).inflate(1.5D)).forEach(player -> {
                        if (level.random.nextInt(40) == 0) player.hurt(level.damageSources().generic(), 1.0F);
                    });
        }
        setChanged();
    }

    public boolean isWild() {
        return getBlockState().is(FarmContent.WILD_HIVE.get());
    }

    public FluidTank honeyTank() {
        return honeyTank;
    }

    public int honeyAmount() {
        return honeyTank.getFluidAmount();
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, net.minecraft.core.Direction side) {
        if (capability == ForgeCapabilities.FLUID_HANDLER) return honeyOptional.cast();
        return super.getCapability(capability, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        honeyOptional.invalidate();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Honey", honeyTank.writeToNBT(new CompoundTag()));
        tag.putInt("NextHoney", nextHoney);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Honey")) honeyTank.readFromNBT(tag.getCompound("Honey"));
        nextHoney = Math.max(1, tag.contains("NextHoney") ? tag.getInt("NextHoney") : 1);
    }
}
