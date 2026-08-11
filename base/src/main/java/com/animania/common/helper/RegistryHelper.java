package com.animania.common.helper;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

/** DeferredRegister helpers replacing the 1.12 global EntityRegistry calls. */
public final class RegistryHelper {
    private RegistryHelper() { }

    public static <T, R extends T> RegistryObject<T> register(DeferredRegister<T> registry, String id,
                                                               Supplier<R> factory) {
        return registry.register(id, factory);
    }

    public static <T extends net.minecraft.world.entity.Entity> EntityType.Builder<T> animalBuilder(
            EntityType.EntityFactory<T> factory, float width, float height) {
        return EntityType.Builder.of(factory, MobCategory.CREATURE).sized(width, height)
                .clientTrackingRange(8).updateInterval(3);
    }
}
