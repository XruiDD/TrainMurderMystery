package dev.doctor4t.wathe.network;

import dev.doctor4t.wathe.Wathe;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

public record VersionCheckPayload(String version) implements CustomPayload {
    public static final CustomPayload.Id<VersionCheckPayload> ID =
            new CustomPayload.Id<>(Wathe.id("version_check"));
    public static final PacketCodec<PacketByteBuf, VersionCheckPayload> CODEC =
            PacketCodec.tuple(PacketCodecs.STRING, VersionCheckPayload::version, VersionCheckPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
