package dev.doctor4t.wathe.item.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;

/**
 * Item cosmetic marker. The runtime resource pack supplies the actual model/texture
 * keyed by {@link #cosmeticId}; this component only carries identity + tooltip metadata.
 */
public record CosmeticComponent(String cosmeticId, String displayName, String rarity) {
    public static final Codec<CosmeticComponent> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("cosmeticId").forGetter(CosmeticComponent::cosmeticId),
            Codec.STRING.fieldOf("displayName").forGetter(CosmeticComponent::displayName),
            Codec.STRING.fieldOf("rarity").forGetter(CosmeticComponent::rarity)
    ).apply(instance, CosmeticComponent::new));

    public static final PacketCodec<PacketByteBuf, CosmeticComponent> PACKET_CODEC = PacketCodec.tuple(
            PacketCodecs.STRING, CosmeticComponent::cosmeticId,
            PacketCodecs.STRING, CosmeticComponent::displayName,
            PacketCodecs.STRING, CosmeticComponent::rarity,
            CosmeticComponent::new
    );
}
