package com.animania.compat.top;

import com.animania.Animania;
import com.animania.api.IAnimaniaProbeBlock;
import com.animania.compat.AnimaniaProbeComponents;
import com.animania.common.entity.AnimaniaAnimalEntity;
import com.animania.common.entity.AnimaniaVehicleEntity;
import com.animania.common.block.AnimaniaStorageBlockEntity;
import com.animania.common.block.AnimaniaSaltLickBlockEntity;
import mcjty.theoneprobe.api.IProbeInfo;
import mcjty.theoneprobe.api.IProbeInfoEntityProvider;
import mcjty.theoneprobe.api.IProbeInfoProvider;
import mcjty.theoneprobe.api.ITheOneProbe;
import mcjty.theoneprobe.api.ProbeMode;
import mcjty.theoneprobe.api.IProbeHitData;
import mcjty.theoneprobe.api.IProbeHitEntityData;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.ModList;

/** The One Probe bridge uses TOP's official IMC API and remains optional. */
public final class AnimaniaTopProbeCompat {
    private static final ResourceLocation UID = new ResourceLocation(Animania.MOD_ID, "top");

    private AnimaniaTopProbeCompat() { }

    public static void bootstrap() {
        if (!ModList.get().isLoaded("theoneprobe")) return;
        InterModComms.sendTo("theoneprobe", "getTheOneProbe", () -> (java.util.function.Consumer<ITheOneProbe>) probe -> {
            probe.registerProvider(new BlockProvider());
            probe.registerEntityProvider(new EntityProvider());
        });
    }

    private static final class BlockProvider implements IProbeInfoProvider {
        @Override
        public ResourceLocation getID() { return UID; }

        @Override
        public void addProbeInfo(ProbeMode mode, IProbeInfo info, Player player, Level level,
                                 BlockState state, IProbeHitData hitData) {
            if (state.getBlock().getDescriptionId().startsWith("block.animania")) {
                info.text(Component.translatable("top.animania.block"));
                if (level.getBlockEntity(hitData.getPos()) instanceof AnimaniaStorageBlockEntity storage) {
                    info.text(Component.translatable("top.animania.storage", storage.getContainerSize()));
                }
                if (level.getBlockEntity(hitData.getPos()) instanceof AnimaniaSaltLickBlockEntity salt) {
                    info.text(Component.translatable("top.animania.salt_uses", salt.usesLeft()));
                }
                if (level.getBlockEntity(hitData.getPos()) instanceof IAnimaniaProbeBlock probe) {
                    probe.getAnimaniaProbeInfo().forEach(info::text);
                }
            }
        }
    }

    private static final class EntityProvider implements IProbeInfoEntityProvider {
        @Override
        public String getID() { return UID.toString(); }

        @Override
        public void addProbeEntityInfo(ProbeMode mode, IProbeInfo info, Player player, Level level,
                                       Entity entity, IProbeHitEntityData hitData) {
            if (entity instanceof AnimaniaAnimalEntity animal) {
                AnimaniaProbeComponents.animal(animal).forEach(info::text);
            } else if (entity instanceof AnimaniaVehicleEntity vehicle) {
                info.text(Component.translatable("top.animania.vehicle_cargo", vehicle.getContainerSize()));
            }
        }
    }
}
