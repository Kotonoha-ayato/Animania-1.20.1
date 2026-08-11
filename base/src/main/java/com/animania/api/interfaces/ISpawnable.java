package com.animania.api.interfaces;

import net.minecraft.world.item.Item;

/** Published spawn-egg metadata contract retained for third-party addons. */
public interface ISpawnable {
    Item getSpawnEgg();

    int getPrimaryEggColor();

    int getSecondaryEggColor();

    default boolean usesEggColor() {
        return true;
    }
}
