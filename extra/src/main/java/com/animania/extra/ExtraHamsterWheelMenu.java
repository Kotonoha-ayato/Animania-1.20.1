package com.animania.extra;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** One-slot wheel inventory. The legacy wheel only accepts hamster food. */
public final class ExtraHamsterWheelMenu extends AbstractContainerMenu {
    private static final int WHEEL_SLOT = 0;
    private static final int PLAYER_START = 1;
    private static final int PLAYER_END = 37;
    private final Container wheel;

    public ExtraHamsterWheelMenu(int id, Inventory inventory, FriendlyByteBuf data) {
        this(id, inventory, resolve(inventory, data.readBlockPos()));
    }

    public ExtraHamsterWheelMenu(int id, Inventory inventory, Container wheel) {
        super(ExtraContent.HAMSTER_WHEEL_MENU.get(), id);
        this.wheel = wheel;
        checkContainerSize(wheel, 1);
        wheel.startOpen(inventory.player);
        addSlot(new Slot(wheel, 0, 80, 20) {
            @Override public boolean mayPlace(ItemStack stack) { return wheel.canPlaceItem(0, stack); }
            @Override public int getMaxStackSize() { return wheel.getMaxStackSize(); }
        });
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 51 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 8 + column * 18, 109));
        }
    }

    private static Container resolve(Inventory inventory, BlockPos pos) {
        if (inventory.player.level().getBlockEntity(pos) instanceof ExtraHamsterWheelBlockEntity wheel) return wheel;
        return new SimpleContainer(1);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index == WHEEL_SLOT) {
            if (!moveItemStackTo(stack, PLAYER_START, PLAYER_END, true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, WHEEL_SLOT, WHEEL_SLOT + 1, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, stack);
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return wheel.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        wheel.stopOpen(player);
    }
}
