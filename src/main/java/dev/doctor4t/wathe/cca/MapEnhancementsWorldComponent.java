package dev.doctor4t.wathe.cca;

import dev.doctor4t.wathe.Wathe;
import dev.doctor4t.wathe.config.datapack.MapEnhancementsConfiguration;
import dev.doctor4t.wathe.config.datapack.MapEnhancementsConfiguration.SceneryConfig;
import dev.doctor4t.wathe.config.datapack.MapEnhancementsConfiguration.VisibilityConfig;
import dev.doctor4t.wathe.config.datapack.MapEnhancementsConfiguration.FogConfig;
import dev.doctor4t.wathe.config.datapack.MapEnhancementsConfiguration.CameraShakeConfig;
import dev.doctor4t.wathe.config.datapack.MapEnhancementsConfiguration.SnowParticlesConfig;
import dev.doctor4t.wathe.config.datapack.MapEnhancementsConfigurationManager;
import dev.doctor4t.wathe.config.datapack.RoomConfig;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;

import java.util.Optional;

/**
 * 地图增强功能组件
 *
 * 提供上游不支持的增强功能：
 * 1. 房间配置系统（从数据包加载）
 * 2. 渲染效果配置（从数据包加载，并同步到客户端）
 *
 * 这个组件完全独立于 MapVariablesWorldComponent，不影响与上游的兼容性
 */
public class MapEnhancementsWorldComponent implements AutoSyncedComponent {
    public static final ComponentKey<MapEnhancementsWorldComponent> KEY = ComponentRegistry.getOrCreate(Wathe.id("mapEnhancements"), MapEnhancementsWorldComponent.class);
    private final World world;

    public MapEnhancementsWorldComponent(World world) {
        this.world = world;
    }

    public void sync() {
        KEY.sync(this.world);
    }

    // ========== 客户端同步缓存字段 ==========
    // 服务端从数据包读取，通过NBT同步到客户端
    private SceneryConfig syncedScenery;
    private VisibilityConfig syncedVisibility;
    private FogConfig syncedFog;
    private CameraShakeConfig syncedCameraShake;
    private SnowParticlesConfig syncedSnowParticles;

    // ========== 渲染配置 Getter 方法 ==========

    public SceneryConfig getSceneryConfig() {
        // 服务端从数据包读取，客户端使用同步的数据
        if (world.isClient() && syncedScenery != null) {
            return syncedScenery;
        }
        MapEnhancementsConfiguration config = MapEnhancementsConfigurationManager.getInstance().getConfiguration();
        return config != null ? config.getSceneryOrDefault() : SceneryConfig.DEFAULT;
    }

    public VisibilityConfig getVisibilityConfig() {
        if (world.isClient() && syncedVisibility != null) {
            return syncedVisibility;
        }
        MapEnhancementsConfiguration config = MapEnhancementsConfigurationManager.getInstance().getConfiguration();
        return config != null ? config.getVisibilityOrDefault() : VisibilityConfig.DEFAULT;
    }

    public FogConfig getFogConfig() {
        if (world.isClient() && syncedFog != null) {
            return syncedFog;
        }
        MapEnhancementsConfiguration config = MapEnhancementsConfigurationManager.getInstance().getConfiguration();
        return config != null ? config.getFogOrDefault() : FogConfig.DEFAULT;
    }

    public CameraShakeConfig getCameraShakeConfig() {
        if (world.isClient() && syncedCameraShake != null) {
            return syncedCameraShake;
        }
        MapEnhancementsConfiguration config = MapEnhancementsConfigurationManager.getInstance().getConfiguration();
        return config != null ? config.getCameraShakeOrDefault() : CameraShakeConfig.DEFAULT;
    }

    public SnowParticlesConfig getSnowParticlesConfig() {
        if (world.isClient() && syncedSnowParticles != null) {
            return syncedSnowParticles;
        }
        MapEnhancementsConfiguration config = MapEnhancementsConfigurationManager.getInstance().getConfiguration();
        return config != null ? config.getSnowParticlesOrDefault() : SnowParticlesConfig.DEFAULT;
    }

    // ========== 房间配置相关方法（仅服务端）==========

    public int getRoomCount() {
        MapEnhancementsConfiguration config = MapEnhancementsConfigurationManager.getInstance().getConfiguration();
        return config != null ? config.getRoomCount() : 0;
    }

    public Optional<RoomConfig> getRoomConfig(int roomNumber) {
        MapEnhancementsConfiguration config = MapEnhancementsConfigurationManager.getInstance().getConfiguration();
        if (config != null) {
            return config.getRoomConfig(roomNumber);
        }
        return Optional.empty();
    }

    public Optional<RoomConfig.SpawnPoint> getSpawnPointForPlayer(int roomNumber, int playerIndexInRoom) {
        MapEnhancementsConfiguration config = MapEnhancementsConfigurationManager.getInstance().getConfiguration();
        if (config != null) {
            return config.getSpawnPointForPlayer(roomNumber, playerIndexInRoom);
        }
        return Optional.empty();
    }

    public int getTotalRoomCapacity() {
        MapEnhancementsConfiguration config = MapEnhancementsConfigurationManager.getInstance().getConfiguration();
        return config != null ? config.getTotalCapacity() : 0;
    }

    public boolean hasAreaConfiguration() {
        return MapEnhancementsConfigurationManager.getInstance().hasConfiguration();
    }

    // ========== NBT 序列化（同步渲染配置到客户端）==========

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.@NotNull WrapperLookup registryLookup) {
        // 客户端读取服务端同步的渲染配置
        if (tag.contains("sceneryTileWidthChunks")) {
            this.syncedScenery = new SceneryConfig(
                tag.getInt("sceneryTileWidthChunks"),
                tag.getInt("sceneryTileLengthChunks"),
                tag.getInt("sceneryHeightOffset")
            );
        }
        if (tag.contains("visibilityDay")) {
            this.syncedVisibility = new VisibilityConfig(
                tag.getInt("visibilityDay"),
                tag.getInt("visibilityNight"),
                tag.getInt("visibilitySundown")
            );
        }
        if (tag.contains("fogEnabled")) {
            this.syncedFog = new FogConfig(
                tag.getFloat("fogStart"),
                tag.getFloat("fogEndMoving"),
                tag.getFloat("fogEndStationary"),
                tag.getInt("fogNightColor")
            );
        }
        if (tag.contains("cameraShakeEnabled")) {
            this.syncedCameraShake = new CameraShakeConfig(
                tag.getBoolean("cameraShakeEnabled"),
                tag.getFloat("cameraShakeAmplitudeIndoor"),
                tag.getFloat("cameraShakeAmplitudeOutdoor"),
                tag.getFloat("cameraShakeStrengthIndoor"),
                tag.getFloat("cameraShakeStrengthOutdoor")
            );
        }
        if (tag.contains("snowParticlesEnabled")) {
            this.syncedSnowParticles = new SnowParticlesConfig(
                tag.getInt("snowParticlesCount"),
                tag.getFloat("snowParticlesSpawnOffsetX"),
                tag.getFloat("snowParticlesSpawnRangeY"),
                tag.getFloat("snowParticlesSpawnRangeZ")
            );
        }
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.@NotNull WrapperLookup registryLookup) {
        // 服务端从数据包读取配置并写入NBT，同步到客户端
        SceneryConfig scenery = getSceneryConfig();
        tag.putInt("sceneryTileWidthChunks", scenery.tileWidthChunks());
        tag.putInt("sceneryTileLengthChunks", scenery.tileLengthChunks());
        tag.putInt("sceneryHeightOffset", scenery.heightOffset());

        VisibilityConfig visibility = getVisibilityConfig();
        tag.putInt("visibilityDay", visibility.day());
        tag.putInt("visibilityNight", visibility.night());
        tag.putInt("visibilitySundown", visibility.sundown());

        FogConfig fog = getFogConfig();
        tag.putFloat("fogStart", fog.start());
        tag.putFloat("fogEndMoving", fog.endMoving());
        tag.putFloat("fogEndStationary", fog.endStationary());
        tag.putInt("fogNightColor", fog.nightColor());

        CameraShakeConfig cameraShake = getCameraShakeConfig();
        tag.putBoolean("cameraShakeEnabled", cameraShake.enabled());
        tag.putFloat("cameraShakeAmplitudeIndoor", cameraShake.amplitudeIndoor());
        tag.putFloat("cameraShakeAmplitudeOutdoor", cameraShake.amplitudeOutdoor());
        tag.putFloat("cameraShakeStrengthIndoor", cameraShake.strengthIndoor());
        tag.putFloat("cameraShakeStrengthOutdoor", cameraShake.strengthOutdoor());

        SnowParticlesConfig snowParticles = getSnowParticlesConfig();
        tag.putInt("snowParticlesCount", snowParticles.count());
        tag.putFloat("snowParticlesSpawnOffsetX", snowParticles.spawnOffsetX());
        tag.putFloat("snowParticlesSpawnRangeY", snowParticles.spawnRangeY());
        tag.putFloat("snowParticlesSpawnRangeZ", snowParticles.spawnRangeZ());
    }
}
