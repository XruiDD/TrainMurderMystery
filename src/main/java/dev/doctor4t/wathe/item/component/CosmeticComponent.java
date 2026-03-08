package dev.doctor4t.wathe.item.component;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @param displayName Minecraft Raw JSON Text, parsed via {@link net.minecraft.text.Text.Serialization#fromJson}
 * @param resources   JSON string containing named textures and optional custom data.
 *                    Structure: {"textures": {"key": "url", ...}, "data": {...}}
 *                    Empty string if no extra resources.
 */
public record CosmeticComponent(String cosmeticId, String displayName, String rarity, String textureUrl, String resources) {
    private static final Gson GSON = new Gson();

    /**
     * Retrieve a named texture URL from resources.textures, falling back to textureUrl.
     * Only called at state-transition time (e.g. when throwing a grenade), not per-frame.
     */
    public String getTexture(String key) {
        if (!resources.isEmpty()) {
            try {
                JsonObject res = GSON.fromJson(resources, JsonObject.class);
                JsonObject textures = res.has("textures") ? res.getAsJsonObject("textures") : null;
                if (textures != null && textures.has(key)) {
                    String url = textures.get(key).getAsString();
                    if (!url.isEmpty()) return url;
                }
            } catch (Exception ignored) {}
        }
        return textureUrl;
    }

    /**
     * Retrieve the optional "data" object from resources, for item-specific custom logic.
     */
    public @Nullable JsonObject getExtraData() {
        if (resources.isEmpty()) return null;
        try {
            JsonObject res = GSON.fromJson(resources, JsonObject.class);
            return res.has("data") ? res.getAsJsonObject("data") : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Collect all texture URLs from resources.textures (excluding the primary textureUrl).
     * Used to pre-load variant textures (e.g. thrown grenade) while the item is in inventory.
     */
    public List<String> getResourceTextureUrls() {
        if (resources.isEmpty()) return List.of();
        try {
            JsonObject res = GSON.fromJson(resources, JsonObject.class);
            JsonObject textures = res.has("textures") ? res.getAsJsonObject("textures") : null;
            if (textures == null) return List.of();
            List<String> urls = new ArrayList<>();
            for (Map.Entry<String, JsonElement> entry : textures.entrySet()) {
                String url = entry.getValue().getAsString();
                if (!url.isEmpty()) urls.add(url);
            }
            return urls;
        } catch (Exception e) {
            return List.of();
        }
    }

    /**
     * Create a copy with a different textureUrl (used when transitioning visual states,
     * e.g. setting the thrown grenade's active texture).
     */
    public CosmeticComponent withTextureUrl(String newTextureUrl) {
        return new CosmeticComponent(cosmeticId, displayName, rarity, newTextureUrl, resources);
    }

    public static final Codec<CosmeticComponent> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("cosmeticId").forGetter(CosmeticComponent::cosmeticId),
            Codec.STRING.fieldOf("displayName").forGetter(CosmeticComponent::displayName),
            Codec.STRING.fieldOf("rarity").forGetter(CosmeticComponent::rarity),
            Codec.STRING.fieldOf("textureUrl").forGetter(CosmeticComponent::textureUrl),
            Codec.STRING.optionalFieldOf("resources", "").forGetter(CosmeticComponent::resources)
    ).apply(instance, CosmeticComponent::new));

    public static final PacketCodec<PacketByteBuf, CosmeticComponent> PACKET_CODEC = PacketCodec.tuple(
            PacketCodecs.STRING, CosmeticComponent::cosmeticId,
            PacketCodecs.STRING, CosmeticComponent::displayName,
            PacketCodecs.STRING, CosmeticComponent::rarity,
            PacketCodecs.STRING, CosmeticComponent::textureUrl,
            PacketCodecs.STRING, CosmeticComponent::resources,
            CosmeticComponent::new
    );
}
