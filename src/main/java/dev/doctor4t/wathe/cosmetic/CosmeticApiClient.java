package dev.doctor4t.wathe.cosmetic;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.doctor4t.wathe.Wathe;
import dev.doctor4t.wathe.cca.PlayerCosmeticsComponent;
import dev.doctor4t.wathe.item.component.CosmeticComponent;
import net.minecraft.util.Identifier;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class CosmeticApiClient {
    private static final Gson GSON = new Gson();
    private static final String COSMETIC_API_URL = "https://express-api.tlspark.cn";
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    /**
     * 单次 fetch 的合并结果。
     */
    public record FetchedCosmetics(
            Map<Identifier, CosmeticComponent> itemSkins,
            Map<String, PlayerCosmeticsComponent.PlayerSkinEntry> playerSkins
    ) {
        public boolean isEmpty() {
            return itemSkins.isEmpty() && playerSkins.isEmpty();
        }

        public static FetchedCosmetics empty() {
            return new FetchedCosmetics(Map.of(), Map.of());
        }
    }

    /**
     * GET /cosmetics/equipped/v1/{uuid}
     */
    public static CompletableFuture<FetchedCosmetics> fetchPlayerCosmetics(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(COSMETIC_API_URL + "/cosmetics/equipped/v1/" + uuid))
                        .timeout(Duration.ofSeconds(10))
                        .GET()
                        .build();

                HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() != 200) {
                    Wathe.LOGGER.warn("[CosmeticApi] HTTP {}: {}", response.statusCode(), response.body());
                    return FetchedCosmetics.empty();
                }

                return parsePlayerResponse(response.body());
            } catch (Exception e) {
                Wathe.LOGGER.warn("[CosmeticApi] Failed to fetch cosmetics for {}: {}", uuid, e.getMessage());
                return FetchedCosmetics.empty();
            }
        });
    }

    /**
     * Parse equipped-cosmetics response. Top-level shape: { slotId: {...}, slotId: {...}, ... }.
     */
    private static FetchedCosmetics parsePlayerResponse(String json) {
        Map<Identifier, CosmeticComponent> itemSkins = new HashMap<>();
        Map<String, PlayerCosmeticsComponent.PlayerSkinEntry> playerSkins = new HashMap<>();
        try {
            JsonObject items = GSON.fromJson(json, JsonObject.class);
            if (items == null) return new FetchedCosmetics(itemSkins, playerSkins);

            for (Map.Entry<String, JsonElement> e : items.entrySet()) {
                String slotId = e.getKey();
                if (!e.getValue().isJsonObject()) continue;
                JsonObject obj = e.getValue().getAsJsonObject();
                String type = obj.has("type") ? obj.get("type").getAsString() : null;
                if (type == null) continue;

                switch (type) {
                    case "ITEM_SKIN" -> parseItemSkin(slotId, obj, itemSkins);
                    case "PLAYER_SKIN" -> parsePlayerSkin(slotId, obj, playerSkins);
                    default -> { /* unknown type, ignore */ }
                }
            }
        } catch (Exception e) {
            Wathe.LOGGER.warn("[CosmeticApi] Failed to parse player response: {}", e.getMessage());
        }
        return new FetchedCosmetics(itemSkins, playerSkins);
    }

    private static void parseItemSkin(String slotId, JsonObject obj,
                                      Map<Identifier, CosmeticComponent> out) {
        Identifier itemId = Identifier.tryParse(slotId);
        if (itemId == null) return;
        if (!obj.has("cosmeticId") || !obj.has("displayName")
                || !obj.has("rarity") || !obj.has("textureUrl")) {
            Wathe.LOGGER.warn("[CosmeticApi] ITEM_SKIN {} missing required field", slotId);
            return;
        }
        CosmeticComponent component = new CosmeticComponent(
                obj.get("cosmeticId").getAsString(),
                obj.get("displayName").getAsString(),
                obj.get("rarity").getAsString(),
                obj.get("textureUrl").getAsString(),
                obj.has("resources") && !obj.get("resources").isJsonNull()
                        ? obj.get("resources").toString()
                        : ""
        );
        out.put(itemId, component);
    }

    private static void parsePlayerSkin(String slotId, JsonObject obj,
                                        Map<String, PlayerCosmeticsComponent.PlayerSkinEntry> out) {
        if (!slotId.startsWith("player_skin:")) {
            Wathe.LOGGER.warn("[CosmeticApi] PLAYER_SKIN slotId missing prefix: {}", slotId);
            return;
        }
        String slot = slotId.substring("player_skin:".length());
        if (!obj.has("cosmeticId") || obj.get("cosmeticId").isJsonNull()) {
            Wathe.LOGGER.warn("[CosmeticApi] PLAYER_SKIN {} missing cosmeticId", slotId);
            return;
        }
        if (!obj.has("textureUrl") || obj.get("textureUrl").isJsonNull()) {
            Wathe.LOGGER.warn("[CosmeticApi] PLAYER_SKIN {} missing textureUrl", slotId);
            return;
        }
        if (!obj.has("playerSkinModel") || obj.get("playerSkinModel").isJsonNull()) {
            Wathe.LOGGER.warn("[CosmeticApi] PLAYER_SKIN {} missing playerSkinModel", slotId);
            return;
        }
        String url = obj.get("textureUrl").getAsString();
        String model = obj.get("playerSkinModel").getAsString();
        if (!"WIDE".equals(model) && !"SLIM".equals(model)) {
            Wathe.LOGGER.warn("[CosmeticApi] PLAYER_SKIN {} unknown model: {}", slotId, model);
            return;
        }
        out.put(slot, new PlayerCosmeticsComponent.PlayerSkinEntry(
                obj.get("cosmeticId").getAsString(), url, model));
    }
}
