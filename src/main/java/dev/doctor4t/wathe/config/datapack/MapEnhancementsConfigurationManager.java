package dev.doctor4t.wathe.config.datapack;

import dev.doctor4t.wathe.Wathe;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

/**
 * 地图配置管理器（单例模式）
 * 所有配置通过 MapRegistry 管理，按维度ID获取
 */
public class MapEnhancementsConfigurationManager {
    private static final MapEnhancementsConfigurationManager INSTANCE = new MapEnhancementsConfigurationManager();

    private MapEnhancementsConfigurationManager() {
    }

    public static MapEnhancementsConfigurationManager getInstance() {
        return INSTANCE;
    }

    /**
     * 获取指定维度地图的增强配置
     * @param dimensionId 维度ID
     * @return 配置，如果不存在返回 null
     */
    @Nullable
    public MapEnhancementsConfiguration getConfiguration(Identifier dimensionId) {
        // 先按 map ID 查找
        MapRegistryEntry entry = MapRegistry.getInstance().getMap(dimensionId);
        if (entry != null) {
            return entry.enhancements();
        }
        // 再按维度ID在所有地图中查找匹配的
        for (MapRegistryEntry mapEntry : MapRegistry.getInstance().getMaps().values()) {
            if (mapEntry.dimensionId().equals(dimensionId)) {
                return mapEntry.enhancements();
            }
        }
        return null;
    }

    /**
     * 获取当前世界的配置（向后兼容：使用 overworld 的配置）
     * @return 配置，如果没有则返回 null
     */
    @Nullable
    public MapEnhancementsConfiguration getConfiguration() {
        // 向后兼容：返回 overworld 的配置
        return getConfiguration(Identifier.ofVanilla("overworld"));
    }

    /**
     * 是否已加载任何配置
     */
    public boolean hasConfiguration() {
        return MapRegistry.getInstance().getMapCount() > 0;
    }

    /**
     * 是否有指定维度的配置
     */
    public boolean hasConfiguration(Identifier dimensionId) {
        return getConfiguration(dimensionId) != null;
    }
}
