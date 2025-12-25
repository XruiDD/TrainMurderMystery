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
import java.util.Map;
import java.util.Optional;

/**
 * 从 datapack 加载地图配置的资源重载监听器
 * 配置路径: data/wathe/areas/*.json
 */
public class MapEnhancementsConfigurationReloader implements SimpleSynchronousResourceReloadListener {

    private static final Gson GSON = new GsonBuilder().setLenient().create();
    private static final String DATA_PATH = "areas";

    public static void register() {
        ResourceManagerHelper.get(ResourceType.SERVER_DATA)
            .registerReloadListener(new MapEnhancementsConfigurationReloader());
        Wathe.LOGGER.info("Registered area configuration reloader");
    }

    @Override
    public Identifier getFabricId() {
        return Wathe.id("area_configuration");
    }

    @Override
    public void reload(ResourceManager manager) {
        Wathe.LOGGER.info("Reloading area configurations...");

        MapEnhancementsConfiguration loadedConfig = null;

        // 查找所有 wathe_areas 目录下的 JSON 文件
        Map<Identifier, Resource> resources = manager.findResources(
            DATA_PATH,
            id -> id.getPath().endsWith(".json")
        );

        for (Map.Entry<Identifier, Resource> entry : resources.entrySet()) {
            Identifier resourceId = entry.getKey();

            // 只加载 wathe 命名空间的配置
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
                        Wathe.LOGGER.error("Failed to parse area config {}: {}", resourceId, error));

                if (result.isPresent()) {
                    loadedConfig = result.get();
                    Wathe.LOGGER.info("Loaded area configuration from {}", resourceId);
                    // 只使用第一个成功加载的配置
                    break;
                }

            } catch (Exception e) {
                Wathe.LOGGER.error("Error loading area configuration from {}", resourceId, e);
            }
        }

        // 更新配置管理器
        MapEnhancementsConfigurationManager.getInstance().setConfiguration(loadedConfig);
    }
}
