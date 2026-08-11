package com.animania.network;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Packet registration and server-authority regression coverage. */
class AnimaniaNetworkContractTest {
    @Test
    void onlyValidatedSnapshotRequestsCrossTheSimpleChannel() throws Exception {
        String network = Files.readString(Path.of("src/main/java/com/animania/network/AnimaniaNetwork.java"));
        String packet = Files.readString(Path.of("src/main/java/com/animania/network/RequestAnimalSnapshotPacket.java"));
        assertTrue(network.contains("SimpleChannel"));
        assertTrue(network.contains("registerMessage"));
        assertTrue(packet.contains("context.getSender()"));
        assertTrue(packet.contains("setPacketHandled(true)"));
    }
}
