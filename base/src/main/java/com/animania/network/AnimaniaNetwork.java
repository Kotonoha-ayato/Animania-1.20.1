package com.animania.network;

import com.animania.Animania;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.concurrent.atomic.AtomicBoolean;

/** Minimal validated channel reserved for future client hints; authoritative state uses entity data. */
public final class AnimaniaNetwork {
    private static final String PROTOCOL = "1";
    private static final AtomicBoolean REGISTERED = new AtomicBoolean();
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(Animania.MOD_ID, "main"), () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    private AnimaniaNetwork() {
    }

    public static void register() {
        if (REGISTERED.compareAndSet(false, true)) {
            CHANNEL.registerMessage(0, RequestAnimalSnapshotPacket.class, RequestAnimalSnapshotPacket::encode,
                    RequestAnimalSnapshotPacket::decode, RequestAnimalSnapshotPacket::handle);
        }
    }
}

