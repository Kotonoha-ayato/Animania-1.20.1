package com.animania.catsdogs;

import com.mojang.serialization.Codec;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class CatsDogsWorldgen {
    public static final DeferredRegister<Codec<? extends BiomeModifier>> BIOME_MODIFIER_SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, AnimaniaCatsDogs.MOD_ID);
    public static final RegistryObject<Codec<CatsDogsSpawnBiomeModifier>> CONFIGURED_SPAWNS =
            BIOME_MODIFIER_SERIALIZERS.register("configured_spawns", () -> Codec.unit(CatsDogsSpawnBiomeModifier::new));

    private CatsDogsWorldgen() { }
}
