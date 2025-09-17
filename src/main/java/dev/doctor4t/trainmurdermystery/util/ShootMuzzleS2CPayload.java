package dev.doctor4t.trainmurdermystery.util;

import dev.doctor4t.trainmurdermystery.TMM;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

public record ShootMuzzleS2CPayload(String shooterId) implements CustomPayload {
    public static final Id<ShootMuzzleS2CPayload> ID = new Id<>(TMM.id("shoot_muzzle_s2c"));

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    public static final PacketCodec<PacketByteBuf, ShootMuzzleS2CPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING,
            ShootMuzzleS2CPayload::shooterId,
            ShootMuzzleS2CPayload::new
    );
}
