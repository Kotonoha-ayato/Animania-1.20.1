package com.animania.network;

import com.animania.client.render.AnimaniaCarryClientState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/** Server-authoritative player carry state used by the client render layer. */
public record CarriedAnimalSyncPacket(UUID playerId, boolean carrying, String type, CompoundTag animal) {
    public static void encode(CarriedAnimalSyncPacket packet, FriendlyByteBuf buf) {
        buf.writeUUID(packet.playerId);
        buf.writeBoolean(packet.carrying);
        buf.writeUtf(packet.type == null ? "" : packet.type, 256);
        buf.writeNbt(packet.animal == null ? new CompoundTag() : packet.animal);
    }

    public static CarriedAnimalSyncPacket decode(FriendlyByteBuf buf) {
        return new CarriedAnimalSyncPacket(buf.readUUID(), buf.readBoolean(), buf.readUtf(256), buf.readNbt());
    }

    public static void handle(CarriedAnimalSyncPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> AnimaniaCarryClientState.accept(packet)));
        context.setPacketHandled(true);
    }
}
