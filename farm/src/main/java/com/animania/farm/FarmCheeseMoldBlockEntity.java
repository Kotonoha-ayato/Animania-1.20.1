package com.animania.farm;

import com.animania.common.block.AnimaniaStorageBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

/**
 * Server-authoritative cheese processing. Both milk bottles and the modern
 * Forge fluid buckets are accepted, and the output retains the legacy milk
 * family so automation does not collapse all cheese variants to one item.
 */
public final class FarmCheeseMoldBlockEntity extends AnimaniaStorageBlockEntity {
    private int processTicks;

    public FarmCheeseMoldBlockEntity(BlockPos pos, BlockState state) {
        super(FarmContent.CHEESE_MOLD_BE.get(), pos, state);
    }

    @Override
    public void serverTick() {
        ItemStack input = getItem(0);
        if (input.isEmpty() && fluidCapability.getFluidAmount() >= 1000) {
            String fluidOutput = outputForFluid(fluidCapability.getFluid());
            if (fluidOutput != null) {
                if (++processTicks >= maturityTicks()) {
                    processTicks = 0;
                    fluidCapability.drain(1000, IFluidHandler.FluidAction.EXECUTE);
                    Item output = ForgeRegistries.ITEMS.getValue(new ResourceLocation(AnimaniaFarm.MOD_ID, fluidOutput));
                    if (output != null) {
                        int amount = fluidOutput.equals("salt") ? Math.max(1, FarmConfig.SALT_CREATION_AMOUNT.get()) : 1;
                        setItem(0, new ItemStack(output, amount));
                    }
                }
                return;
            }
        }
        if (input.isEmpty()) {
            processTicks = 0;
            return;
        }
        String outputId = outputFor(input.getItem());
        if (outputId == null) {
            processTicks = 0;
            return;
        }
        Item output = ForgeRegistries.ITEMS.getValue(new ResourceLocation(AnimaniaFarm.MOD_ID, outputId));
        if (output == null) {
            processTicks = 0;
            return;
        }
        if (++processTicks >= maturityTicks()) {
            processTicks = 0;
            setItem(0, new ItemStack(output));
        }
    }

    private static int maturityTicks() {
        try {
            return Math.max(20, FarmConfig.CHEESE_MATURITY_TIME.get());
        } catch (RuntimeException ignored) {
            return 24000;
        }
    }

    private static String outputFor(Item item) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
        if (id == null || !AnimaniaFarm.MOD_ID.equals(id.getNamespace())) return null;
        String path = id.getPath();
        if (path.equals("milk_bottle")) return "friesian_cheese_wedge";
        if (path.contains("holstein")) return "holstein_cheese_wheel";
        if (path.contains("friesian") || path.equals("cow_bucket_milk")) return "friesian_cheese_wheel";
        if (path.contains("jersey")) return "jersey_cheese_wheel";
        if (path.contains("goat")) return "goat_cheese_wheel";
        if (path.contains("sheep")) return "sheep_cheese_wheel";
        return null;
    }

    private static String outputForFluid(FluidStack fluid) {
        if (fluid == null || fluid.isEmpty()) return null;
        ResourceLocation id = ForgeRegistries.FLUIDS.getKey(fluid.getFluid());
        if (id == null) return null;
        if (id.getNamespace().equals("minecraft") && id.getPath().equals("water") && !disabledSaltCreation()) return "salt";
        if (!AnimaniaFarm.MOD_ID.equals(id.getNamespace())) return null;
        return switch (id.getPath()) {
            case "milk_holstein" -> "holstein_cheese_wheel";
            case "milk_friesian" -> "friesian_cheese_wheel";
            case "milk_jersey" -> "jersey_cheese_wheel";
            case "milk_goat" -> "goat_cheese_wheel";
            case "milk_sheep" -> "sheep_cheese_wheel";
            default -> null;
        };
    }

    private static boolean disabledSaltCreation() {
        try {
            return FarmConfig.DISABLE_SALT_CREATION.get();
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    @Override
    protected boolean isFluidValid(FluidStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        ResourceLocation id = ForgeRegistries.FLUIDS.getKey(stack.getFluid());
        if (id == null) return false;
        if (AnimaniaFarm.MOD_ID.equals(id.getNamespace()) && id.getPath().startsWith("milk_")) return true;
        return id.getNamespace().equals("minecraft") && id.getPath().equals("water") && !disabledSaltCreation();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("ProcessTicks", processTicks);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        processTicks = Math.max(0, tag.getInt("ProcessTicks"));
    }
}
