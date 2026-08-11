package com.animania.common.entity;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.LongPredicate;
import java.util.function.Supplier;

/** Addon-owned, dynamically configurable replacements for 1.12 sleep settings. */
public final class AnimaniaSleepProfiles {
    public record Profile(Supplier<String> primaryBed, Supplier<String> secondaryBed,
                          LongPredicate sleepTime) {
        public Profile {
            if (primaryBed == null || secondaryBed == null || sleepTime == null) {
                throw new IllegalArgumentException("sleep profile fields cannot be null");
            }
        }

        public boolean shouldSleep(long dayTime) {
            return sleepTime.test(Math.floorMod(dayTime, 24000L));
        }

        public net.minecraft.world.level.block.Block primaryBlock() {
            return resolve(primaryBed.get());
        }

        public net.minecraft.world.level.block.Block secondaryBlock() {
            return resolve(secondaryBed.get());
        }

        private static net.minecraft.world.level.block.Block resolve(String id) {
            if (id == null || id.isBlank()) return null;
            ResourceLocation location = ResourceLocation.tryParse(id);
            return location == null ? null : ForgeRegistries.BLOCKS.getValue(location);
        }
    }

    public static final LongPredicate NIGHT = time -> time >= 13000L;
    public static final LongPredicate DAY = time -> time < 13000L;
    public static final LongPredicate RABBIT = time -> (time > 20000L && time < 24000L)
            || (time > 10000L && time < 15000L);

    private static final Map<String, Function<String, Profile>> RESOLVERS = new ConcurrentHashMap<>();

    public static void register(String namespace, Function<String, Profile> resolver) {
        if (namespace == null || namespace.isBlank() || resolver == null) {
            throw new IllegalArgumentException("sleep resolver requires a namespace and function");
        }
        RESOLVERS.put(namespace, resolver);
    }

    public static Optional<Profile> resolve(AnimaniaAnimalEntity animal) {
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(animal.getType());
        if (id == null) return Optional.empty();
        Function<String, Profile> resolver = RESOLVERS.get(id.getNamespace());
        return resolver == null ? Optional.empty() : Optional.ofNullable(resolver.apply(id.getPath()));
    }

    private AnimaniaSleepProfiles() { }
}
