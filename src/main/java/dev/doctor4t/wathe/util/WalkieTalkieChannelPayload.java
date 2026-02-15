package dev.doctor4t.wathe.util;

import dev.doctor4t.wathe.Wathe;
import dev.doctor4t.wathe.index.WatheDataComponentTypes;
import dev.doctor4t.wathe.item.WalkieTalkieItem;
import dev.doctor4t.wathe.item.component.WalkieTalkieComponent;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import org.jetbrains.annotations.NotNull;

public record WalkieTalkieChannelPayload(int channel, boolean mainHand) implements CustomPayload {
    public static final int MAX_CHANNEL = 99;

    public static final Id<WalkieTalkieChannelPayload> ID = new Id<>(Wathe.id("walkie_talkie_channel"));
    public static final PacketCodec<PacketByteBuf, WalkieTalkieChannelPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.INTEGER, WalkieTalkieChannelPayload::channel,
            PacketCodecs.BOOL, WalkieTalkieChannelPayload::mainHand,
            WalkieTalkieChannelPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    public static class Receiver implements ServerPlayNetworking.PlayPayloadHandler<WalkieTalkieChannelPayload> {
        @Override
        public void receive(@NotNull WalkieTalkieChannelPayload payload, ServerPlayNetworking.@NotNull Context context) {
            ServerPlayerEntity player = context.player();
            Hand hand = payload.mainHand() ? Hand.MAIN_HAND : Hand.OFF_HAND;
            ItemStack stack = player.getStackInHand(hand);

            if (!(stack.getItem() instanceof WalkieTalkieItem)) return;

            int channel = Math.max(0, Math.min(MAX_CHANNEL, payload.channel()));
            stack.set(WatheDataComponentTypes.WALKIE_TALKIE, new WalkieTalkieComponent(channel));
        }
    }
}
