package dev.doctor4t.wathe.config.datapack;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import dev.doctor4t.wathe.Wathe;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 从 datapack 加载地图配置的资源重载监听器
 * 配置路径:
 *   - data/wathe/maps/*.json (多地图注册表配置)
 *   - data/wathe/areas/*.json (legacy 单地图配置，自动注册为 overworld)
 */
public class MapEnhancementsConfigurationReloader implements SimpleSynchronousResourceReloadListener {

    private static final Gson GSON = new GsonBuilder().setLenient().create();
    private static final String LEGACY_DATA_PATH = "areas";
    private static final String MAPS_DATA_PATH = "maps";

    public static void register() {
        ResourceManagerHelper.get(ResourceType.SERVER_DATA)
            .registerReloadListener(new MapEnhancementsConfigurationReloader());
        Wathe.LOGGER.info("Registered map configuration reloader");
    }

    @Override
    public Identifier getFabricId() {
        return Wathe.id("area_configuration");
    }

    @Override
    public void reload(ResourceManager manager) {
        Wathe.LOGGER.info("Reloading map configurations...");

        // Clear registry
        MapRegistry.getInstance().clear();

        // === Load multi-map configs from data/wathe/maps/*.json ===
        Map<Identifier, Resource> mapResources = manager.findResources(
            MAPS_DATA_PATH,
            id -> id.getPath().endsWith(".json")
        );

        for (Map.Entry<Identifier, Resource> entry : mapResources.entrySet()) {
            Identifier resourceId = entry.getKey();

            // Only load wathe namespace
            if (!resourceId.getNamespace().equals(Wathe.MOD_ID)) {
                continue;
            }

            try (InputStreamReader reader = new InputStreamReader(
                    entry.getValue().getInputStream(),
                    StandardCharsets.UTF_8)) {

                JsonElement json = GSON.fromJson(reader, JsonElement.class);

                Optional<MapRegistryEntry> result = MapRegistryEntry.CODEC
                    .parse(JsonOps.INSTANCE, json)
                    .resultOrPartial(error ->
                        Wathe.LOGGER.error("Failed to parse map config {}: {}", resourceId, error));

                if (result.isPresent()) {
                    // Extract map ID from resource path: data/wathe/maps/<name>.json
                    String path = resourceId.getPath();
                    String name = path.substring(MAPS_DATA_PATH.length() + 1, path.length() - 5); // strip prefix and .json
                    Identifier mapId = Identifier.of(resourceId.getNamespace(), name);
                    MapRegistry.getInstance().register(mapId, result.get());
                    Wathe.LOGGER.info("Registered map '{}' (dimension: {}) from {}",
                        mapId, result.get().dimensionId(), resourceId);
                }

            } catch (Exception e) {
                Wathe.LOGGER.error("Error loading map config from {}", resourceId, e);
            }
        }

        // === Legacy compatibility: load from data/wathe/areas/*.json as overworld map ===
        // Only if no maps were loaded from the new path
        if (MapRegistry.getInstance().getMapCount() == 0) {
            Map<Identifier, Resource> legacyResources = manager.findResources(
                LEGACY_DATA_PATH,
                id -> id.getPath().endsWith(".json")
            );

            for (Map.Entry<Identifier, Resource> entry : legacyResources.entrySet()) {
                Identifier resourceId = entry.getKey();

                if (!resourceId.getNamespace().equals(Wathe.MOD_ID)) {
                    continue;
                }

                try (InputStreamReader reader = new InputStreamReader(
                        entry.getValue().getInputStream(),
                        StandardCharsets.UTF_8)) {

                    JsonElement json = GSON.fromJson(reader, JsonElement.class);

                    Optional<MapEnhancementsConfiguration> result = MapEnhancementsConfiguration.CODEC
                        .parse(JsonOps.INSTANCE, json)
                        .resultOrPartial(error ->
                            Wathe.LOGGER.error("Failed to parse legacy area config {}: {}", resourceId, error));

                    if (result.isPresent()) {
                        // Register as overworld map entry
                        Identifier overworldDimension = Identifier.ofVanilla("overworld");
                        MapRegistryEntry legacyEntry = new MapRegistryEntry(
                            overworldDimension,
                            "Overworld",
                            Optional.empty(),
                            result.get(),
                            0,
                            100
                        );
                        MapRegistry.getInstance().register(
                            Identifier.of(Wathe.MOD_ID, "legacy_overworld"),
                            legacyEntry
                        );
                        Wathe.LOGGER.info("Loaded legacy area config from {} as overworld map", resourceId);
                        break; // Only load first valid legacy config
                    }

                } catch (Exception e) {
                    Wathe.LOGGER.error("Error loading legacy area config from {}", resourceId, e);
                }
            }
        }

        Wathe.LOGGER.info("Map registry loaded: {} maps registered", MapRegistry.getInstance().getMapCount());
    }
}
