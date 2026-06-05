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
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    /**
     * 所有 cosmetic API 请求的 base URL。刻意做成一个稳定命名、非 lambda 的访问器
     */
    private static String apiBaseUrl() {
        return "https://express-api.tlspark.cn";
    }

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
                        .uri(URI.create(apiBaseUrl() + "/cosmetics/equipped/v1/" + uuid))
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

    public record PackManifest(String version, String url, String sha256) {}

    /** GET /cosmetics/pack -> {version,url,sha256}; null when unavailable. */
    public static CompletableFuture<PackManifest> fetchPackManifest() {
        return CompletableFuture.supplyAsync(() -> {
            String url = apiBaseUrl() + "/cosmetics/pack";
            try {
                Wathe.LOGGER.info("[CosmeticApi] fetching pack manifest from {}", url);
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(10))
                        .GET()
                        .build();
                HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) {
                    Wathe.LOGGER.warn("[CosmeticApi] pack manifest HTTP {} from {}", response.statusCode(), url);
                    return null;
                }
                JsonObject o = GSON.fromJson(response.body(), JsonObject.class);
                if (o == null || !o.has("version") || !o.has("url") || !o.has("sha256")) {
                    Wathe.LOGGER.warn("[CosmeticApi] pack manifest empty / missing fields (body: {})", response.body());
                    return null;
                }
                PackManifest pm = new PackManifest(o.get("version").getAsString(),
                        o.get("url").getAsString(), o.get("sha256").getAsString());
                Wathe.LOGGER.info("[CosmeticApi] pack manifest: version={}, url={}", pm.version(), pm.url());
                return pm;
            } catch (Exception e) {
                Wathe.LOGGER.warn("[CosmeticApi] pack manifest fetch from {} failed: {}", url, e.toString());
                return null;
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
        if (!obj.has("cosmeticId") || !obj.has("displayName") || !obj.has("rarity")) {
            Wathe.LOGGER.warn("[CosmeticApi] ITEM_SKIN {} missing required field", slotId);
            return;
        }
        CosmeticComponent component = new CosmeticComponent(
                obj.get("cosmeticId").getAsString(),
                obj.get("displayName").getAsString(),
                obj.get("rarity").getAsString()
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
