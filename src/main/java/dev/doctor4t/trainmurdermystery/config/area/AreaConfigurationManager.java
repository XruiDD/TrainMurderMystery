package dev.doctor4t.trainmurdermystery.config.area;

import dev.doctor4t.trainmurdermystery.TMM;
import org.jetbrains.annotations.Nullable;

/**
 * 地图配置管理器（单例模式）
 * 仅从 datapack 加载配置
 */
public class AreaConfigurationManager {
    private static final AreaConfigurationManager INSTANCE = new AreaConfigurationManager();

    @Nullable
    private AreaConfiguration configuration;

    private AreaConfigurationManager() {
    }

    public static AreaConfigurationManager getInstance() {
        return INSTANCE;
    }

    /**
     * 设置配置（由 Reloader 调用）
     */
    public void setConfiguration(@Nullable AreaConfiguration configuration) {
        this.configuration = configuration;
        if (configuration != null) {
            TMM.LOGGER.info("Loaded area configuration with {} rooms (total capacity: {})",
                configuration.getRoomCount(), configuration.getTotalCapacity());
        } else {
            TMM.LOGGER.warn("No area configuration loaded from datapack");
        }
    }

    /**
     * 获取当前配置
     * @return 配置，如果没有加载则返回 null
     */
    @Nullable
    public AreaConfiguration getConfiguration() {
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
        TMM.LOGGER.info("Cleared area configuration");
    }
}
