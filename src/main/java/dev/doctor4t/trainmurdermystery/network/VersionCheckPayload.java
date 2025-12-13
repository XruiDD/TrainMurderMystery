package dev.doctor4t.trainmurdermystery.network;

import dev.doctor4t.trainmurdermystery.TMM;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

public record VersionCheckPayload(String version) implements CustomPayload {
    public static final CustomPayload.Id<VersionCheckPayload> ID =
            new CustomPayload.Id<>(TMM.id("version_check"));
    public static final PacketCodec<PacketByteBuf, VersionCheckPayload> CODEC =
            PacketCodec.tuple(PacketCodecs.STRING, VersionCheckPayload::version, VersionCheckPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
