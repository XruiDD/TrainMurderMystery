package dev.doctor4t.wathe.cosmetic;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.doctor4t.wathe.Wathe;
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
     * Fetch equipped cosmetics for a single player from the API.
     * GET /cosmetics/equipped/v1/{uuid}
     */
    public static CompletableFuture<Map<Identifier, CosmeticComponent>> fetchPlayerCosmetics(UUID uuid) {
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
                    return Map.<Identifier, CosmeticComponent>of();
                }

                return parsePlayerResponse(response.body());
            } catch (Exception e) {
                Wathe.LOGGER.warn("[CosmeticApi] Failed to fetch cosmetics for {}: {}", uuid, e.getMessage());
                return Map.<Identifier, CosmeticComponent>of();
            }
        });
    }

    /**
     * Parse response for a single player's equipped cosmetics.
     * Only parses ITEM_SKIN type entries (keyed by applicableItemId like "wathe:knife").
     */
    private static Map<Identifier, CosmeticComponent> parsePlayerResponse(String json) {
        Map<Identifier, CosmeticComponent> result = new HashMap<>();
        try {
            JsonObject items = GSON.fromJson(json, JsonObject.class);
            if (items == null) return result;

            for (Map.Entry<String, JsonElement> itemEntry : items.entrySet()) {
                JsonObject obj = itemEntry.getValue().getAsJsonObject();

                // Only handle ITEM_SKIN type on the mod side
                String type = obj.has("type") ? obj.get("type").getAsString() : null;
                if (!"ITEM_SKIN".equals(type)) continue;

                Identifier itemId = Identifier.tryParse(itemEntry.getKey());
                if (itemId == null) continue;

                CosmeticComponent component = new CosmeticComponent(
                        obj.get("cosmeticId").getAsString(),
                        obj.get("displayName").getAsString(),
                        obj.get("rarity").getAsString(),
                        obj.get("textureUrl").getAsString(),
                        obj.has("resources") && !obj.get("resources").isJsonNull()
                                ? obj.get("resources").toString()
                                : ""
                );
                result.put(itemId, component);
            }
        } catch (Exception e) {
            Wathe.LOGGER.warn("[CosmeticApi] Failed to parse player response: {}", e.getMessage());
        }
        return result;
    }
}
