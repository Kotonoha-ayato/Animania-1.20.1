package com.animania.common.block;

import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.fluids.capability.IFluidHandler;

/** Small server-side inventory used by troughs, nests, bowls and cheese moulds. */
public abstract class AnimaniaStorageBlockEntity extends BlockEntity implements Container, MenuProvider {
    private final NonNullList<ItemStack> items = NonNullList.withSize(9, ItemStack.EMPTY);
    private boolean syncingCapability;
    private final ItemStackHandler itemCapability = new ItemStackHandler(9) {
        @Override
        protected void onContentsChanged(int slot) {
            if (!syncingCapability) syncFromCapability();
        }
    };
    protected final FluidTank fluidCapability = new FluidTank(8000) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }

        @Override
        public boolean isFluidValid(FluidStack stack) {
            return AnimaniaStorageBlockEntity.this.isFluidValid(stack);
        }
    };
    private final LazyOptional<ItemStackHandler> itemCapabilityOptional = LazyOptional.of(() -> itemCapability);
    private final LazyOptional<FluidTank> fluidCapabilityOptional = LazyOptional.of(() -> fluidCapability);

    protected AnimaniaStorageBlockEntity(net.minecraft.world.level.block.entity.BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /** Hook for server-only facility processing (nest laying, cheese moulds). */
    public void serverTick() {
    }

    /**
     * Subclasses can constrain automation to their supported fluid family.
     * The default intentionally accepts every Forge fluid for generic troughs
     * and addon facilities; specialised blocks override it server-side.
     */
    protected boolean isFluidValid(FluidStack stack) {
        return stack != null && !stack.isEmpty();
    }

    @Override
    public int getContainerSize() {
        return items.size();
    }

    @Override
    public boolean isEmpty() {
        return items.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack result = net.minecraft.world.ContainerHelper.removeItem(items, slot, amount);
        if (!result.isEmpty()) {
            setCapabilityStack(slot, items.get(slot));
            setChanged();
        }
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack result = net.minecraft.world.ContainerHelper.takeItem(items, slot);
        setCapabilityStack(slot, ItemStack.EMPTY);
        return result;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        stack.setCount(Math.min(stack.getCount(), getMaxStackSize()));
        setCapabilityStack(slot, stack);
        setChanged();
    }

    private void setCapabilityStack(int slot, ItemStack stack) {
        syncingCapability = true;
        try {
            itemCapability.setStackInSlot(slot, stack.copy());
        } finally {
            syncingCapability = false;
        }
    }

    private void syncFromCapability() {
        for (int slot = 0; slot < items.size(); slot++) items.set(slot, itemCapability.getStackInSlot(slot).copy());
        setChanged();
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, Direction side) {
        if (capability == ForgeCapabilities.ITEM_HANDLER) return itemCapabilityOptional.cast();
        if (capability == ForgeCapabilities.FLUID_HANDLER) return fluidCapabilityOptional.cast();
        return super.getCapability(capability, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemCapabilityOptional.invalidate();
        fluidCapabilityOptional.invalidate();
    }

    @Override
    public boolean stillValid(Player player) {
        return level != null && level.getBlockEntity(worldPosition) == this
                && player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) <= 64.0;
    }

    @Override
    public void clearContent() {
        for (int slot = 0; slot < items.size(); slot++) {
            items.set(slot, ItemStack.EMPTY);
            setCapabilityStack(slot, ItemStack.EMPTY);
        }
        setChanged();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return ChestMenu.threeRows(id, inventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        net.minecraft.world.ContainerHelper.saveAllItems(tag, items);
        tag.put("AnimaniaFluid", fluidCapability.writeToNBT(new CompoundTag()));
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        net.minecraft.world.ContainerHelper.loadAllItems(tag, items);
        if (tag.contains("AnimaniaFluid")) fluidCapability.readFromNBT(tag.getCompound("AnimaniaFluid"));
        for (int slot = 0; slot < items.size(); slot++) setCapabilityStack(slot, items.get(slot));
    }
}
