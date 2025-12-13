package dev.doctor4t.trainmurdermystery.network;

import dev.doctor4t.trainmurdermystery.TMM;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationNetworking;
import net.minecraft.network.packet.Packet;
import net.minecraft.server.network.ServerPlayerConfigurationTask;

import java.util.function.Consumer;

public record VersionCheckConfigurationTask() implements ServerPlayerConfigurationTask {
    public static final Key KEY = new Key(TMM.id("version_check").toString());

    @Override
    public Key getKey() {
        return KEY;
    }

    @Override
    public void sendPacket(Consumer<Packet<?>> sender) {
        sender.accept(ServerConfigurationNetworking.createS2CPacket(
                new VersionCheckPayload(TMM.MOD_VERSION)
        ));
    }
}
