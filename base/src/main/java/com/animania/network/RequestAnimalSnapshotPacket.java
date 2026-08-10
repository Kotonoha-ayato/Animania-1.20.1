package com.animania.network;

import com.animania.common.entity.AnimaniaAnimalEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client request used by probe integrations; no client input is trusted for gameplay. */
public record RequestAnimalSnapshotPacket(int entityId) {
    public static void encode(RequestAnimalSnapshotPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.entityId);
    }

    public static RequestAnimalSnapshotPacket decode(FriendlyByteBuf buf) {
        return new RequestAnimalSnapshotPacket(buf.readVarInt());
    }

    public static void handle(RequestAnimalSnapshotPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            if (context.getSender() != null && packet.entityId >= 0) {
                if (context.getSender().level().getEntity(packet.entityId) instanceof AnimaniaAnimalEntity animal) {
                    animal.ensureValidState();
                }
            }
        });
        context.setPacketHandled(true);
    }
}

