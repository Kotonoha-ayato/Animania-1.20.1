package com.animania.client.render;

import com.animania.network.CarriedAnimalSyncPacket;
import net.minecraft.nbt.CompoundTag;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Client mirror of the server-authoritative carry capability. */
public final class AnimaniaCarryClientState {
    private static final Map<UUID, CarriedState> STATES = new ConcurrentHashMap<>();

    private AnimaniaCarryClientState() { }

    public static void accept(CarriedAnimalSyncPacket packet) {
        if (!packet.carrying() || packet.type() == null || packet.type().isBlank()) {
            STATES.remove(packet.playerId());
            return;
        }
        STATES.put(packet.playerId(), new CarriedState(packet.type(), packet.animal()));
    }

    public static CarriedState get(UUID playerId) {
        return STATES.get(playerId);
    }

    public record CarriedState(String type, CompoundTag animal) {
        public CarriedState {
            animal = animal == null ? new CompoundTag() : animal.copy();
        }
    }
}
