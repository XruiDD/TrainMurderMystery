package dev.doctor4t.wathe.util;

import dev.doctor4t.wathe.Wathe;
import dev.doctor4t.wathe.cca.MapVotingComponent;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;

public record MapVotePayload(int mapIndex) implements CustomPayload {
    public static final Id<MapVotePayload> ID = new Id<>(Wathe.id("map_vote"));
    public static final PacketCodec<RegistryByteBuf, MapVotePayload> CODEC = PacketCodec.tuple(
        PacketCodecs.VAR_INT, MapVotePayload::mapIndex,
        MapVotePayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    public static class Receiver implements ServerPlayNetworking.PlayPayloadHandler<MapVotePayload> {
        @Override
        public void receive(MapVotePayload payload, ServerPlayNetworking.Context context) {
            ServerPlayerEntity player = context.player();
            MapVotingComponent voting = MapVotingComponent.KEY.get(
                player.getServer().getScoreboard());
            voting.castVote(player.getUuid(), payload.mapIndex());
        }
    }
}
