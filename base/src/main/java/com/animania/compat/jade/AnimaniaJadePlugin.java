package com.animania.compat.jade;

import com.animania.Animania;
import com.animania.api.IAnimaniaProbeBlock;
import com.animania.compat.AnimaniaProbeComponents;
import com.animania.common.block.AnimaniaContainerBlock;
import com.animania.common.block.AnimaniaStorageBlockEntity;
import com.animania.common.entity.AnimaniaAnimalEntity;
import com.animania.common.entity.AnimaniaVehicleEntity;
import com.animania.common.block.AnimaniaSaltLickBlockEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Block;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.WailaPlugin;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** Jade integration is loaded only when Jade is present. */
@WailaPlugin(value = Animania.MOD_ID)
@OnlyIn(Dist.CLIENT)
public final class AnimaniaJadePlugin implements IWailaPlugin {
    private static final ResourceLocation UID = new ResourceLocation(Animania.MOD_ID, "jade");

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerEntityDataProvider(new snownee.jade.api.IServerDataProvider<EntityAccessor>() {
            @Override
            public ResourceLocation getUid() { return UID; }

            @Override
            public void appendServerData(net.minecraft.nbt.CompoundTag tag, EntityAccessor accessor) {
                Entity entity = accessor.getEntity();
                if (entity instanceof AnimaniaAnimalEntity animal) {
                    tag.putInt("hunger", animal.getHunger());
                    tag.putInt("thirst", animal.getThirst());
                    tag.putBoolean("pregnant", animal.isPregnant());
                    tag.putBoolean("tamed", animal.isTamed());
                    tag.putBoolean("sitting", animal.isSitting());
                }
            }
        }, AnimaniaAnimalEntity.class);
        registration.registerEntityDataProvider(new snownee.jade.api.IServerDataProvider<EntityAccessor>() {
            @Override
            public ResourceLocation getUid() { return UID; }
            @Override
            public void appendServerData(net.minecraft.nbt.CompoundTag tag, EntityAccessor accessor) {
                if (accessor.getEntity() instanceof AnimaniaVehicleEntity vehicle) tag.putInt("cargo", vehicle.getContainerSize());
            }
        }, AnimaniaVehicleEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerEntityComponent(AnimalProvider.INSTANCE, AnimaniaAnimalEntity.class);
        registration.registerEntityComponent(VehicleProvider.INSTANCE, AnimaniaVehicleEntity.class);
        registration.registerBlockComponent(BlockProvider.INSTANCE, AnimaniaContainerBlock.class);
        registration.registerBlockComponent(BlockProvider.INSTANCE, com.animania.common.block.AnimaniaSaltLickBlock.class);
        // Addons expose status through IAnimaniaProbeBlock, so Base does not
        // acquire a compile-time or runtime dependency on addon classes.
        registration.registerBlockComponent(BlockProvider.INSTANCE, Block.class);
    }

    private static final class AnimalProvider implements IEntityComponentProvider {
        private static final AnimalProvider INSTANCE = new AnimalProvider();
        @Override
        public ResourceLocation getUid() { return UID; }
        @Override
        public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
            if (accessor.getEntity() instanceof AnimaniaAnimalEntity animal) {
                AnimaniaProbeComponents.animal(animal).forEach(tooltip::add);
            }
        }
    }

    private static final class VehicleProvider implements IEntityComponentProvider {
        private static final VehicleProvider INSTANCE = new VehicleProvider();
        @Override public ResourceLocation getUid() { return UID; }
        @Override public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
            if (accessor.getEntity() instanceof AnimaniaVehicleEntity vehicle) {
                tooltip.add(Component.translatable("jade.animania.vehicle_cargo", vehicle.getContainerSize()));
            }
        }
    }

    private static final class BlockProvider implements IBlockComponentProvider {
        private static final BlockProvider INSTANCE = new BlockProvider();
        @Override
        public ResourceLocation getUid() { return UID; }
        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            if (accessor.getBlockEntity() instanceof AnimaniaStorageBlockEntity storage) {
                tooltip.add(Component.translatable("jade.animania.storage", storage.getContainerSize()));
            }
            if (accessor.getBlockEntity() instanceof AnimaniaSaltLickBlockEntity salt) {
                tooltip.add(Component.translatable("jade.animania.salt_uses", salt.usesLeft()));
            }
            if (accessor.getBlockEntity() instanceof IAnimaniaProbeBlock probe) {
                probe.getAnimaniaProbeInfo().forEach(tooltip::add);
            }
        }
    }
}
