package dev.doctor4t.wathe.item.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;

public record WalkieTalkieComponent(int channel) {
    public static final Codec<WalkieTalkieComponent> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("channel").forGetter(WalkieTalkieComponent::channel)
    ).apply(instance, WalkieTalkieComponent::new));

    public static final PacketCodec<PacketByteBuf, WalkieTalkieComponent> PACKET_CODEC =
            PacketCodec.tuple(PacketCodecs.INTEGER, WalkieTalkieComponent::channel, WalkieTalkieComponent::new);

    public static final WalkieTalkieComponent DEFAULT = new WalkieTalkieComponent(0);
}
