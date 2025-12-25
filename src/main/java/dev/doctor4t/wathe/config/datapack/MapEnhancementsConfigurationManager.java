package dev.doctor4t.wathe.config.datapack;

import dev.doctor4t.wathe.Wathe;
import org.jetbrains.annotations.Nullable;

/**
 * 地图配置管理器（单例模式）
 * 仅从 datapack 加载配置
 */
public class MapEnhancementsConfigurationManager {
    private static final MapEnhancementsConfigurationManager INSTANCE = new MapEnhancementsConfigurationManager();

    @Nullable
    private MapEnhancementsConfiguration configuration;

    private MapEnhancementsConfigurationManager() {
    }

    public static MapEnhancementsConfigurationManager getInstance() {
        return INSTANCE;
    }

    /**
     * 设置配置（由 Reloader 调用）
     */
    public void setConfiguration(@Nullable MapEnhancementsConfiguration configuration) {
        this.configuration = configuration;
        if (configuration != null) {
            Wathe.LOGGER.info("Loaded area configuration with {} rooms (total capacity: {})",
                configuration.getRoomCount(), configuration.getTotalCapacity());
        } else {
            Wathe.LOGGER.warn("No area configuration loaded from datapack");
        }
    }

    /**
     * 获取当前配置
     * @return 配置，如果没有加载则返回 null
     */
    @Nullable
    public MapEnhancementsConfiguration getConfiguration() {
        return configuration;
    }

    /**
     * 是否已加载配置
     */
    public boolean hasConfiguration() {
        return configuration != null;
    }

    /**
     * 清除配置
     */
    public void clearConfiguration() {
        this.configuration = null;
        Wathe.LOGGER.info("Cleared area configuration");
    }
}
