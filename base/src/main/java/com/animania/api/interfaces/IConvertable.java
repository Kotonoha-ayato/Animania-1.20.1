package com.animania.api.interfaces;

import net.minecraft.world.entity.Entity;

/** Contract for an Animania entity that can replace itself with its vanilla counterpart. */
public interface IConvertable {
    Entity convertToVanilla();
}
