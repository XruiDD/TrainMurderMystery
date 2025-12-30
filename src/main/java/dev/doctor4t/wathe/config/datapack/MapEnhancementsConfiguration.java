package dev.doctor4t.wathe.config.datapack;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;
import java.util.Optional;

/**
 * 地图增强配置（数据包）
 * 只包含上游不支持的增强功能：
 * 1. 房间配置系统
 * 2. 渲染效果配置
 *
 * 基础地图变量（spawn pos, ready area等）应使用 /wathe:setMapVariable 命令设置
 */
public record MapEnhancementsConfiguration(
    List<RoomConfig> rooms,
    // 可选的渲染配置
    Optional<SceneryConfig> scenery,
    Optional<VisibilityConfig> visibility,
    Optional<FogConfig> fog,
    Optional<CameraShakeConfig> cameraShake
) {

    /**
     * 风景瓦片配置
     */
    public record SceneryConfig(int tileWidthChunks, int tileLengthChunks, int heightOffset) {
        public static final SceneryConfig DEFAULT = new SceneryConfig(15, 32, 116);

        public static final Codec<SceneryConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("tile_width_chunks").forGetter(SceneryConfig::tileWidthChunks),
            Codec.INT.fieldOf("tile_length_chunks").forGetter(SceneryConfig::tileLengthChunks),
            Codec.INT.fieldOf("height_offset").forGetter(SceneryConfig::heightOffset)
        ).apply(instance, SceneryConfig::new));

        public int getTileWidth() { return tileWidthChunks * 16; }
        public int getTileLength() { return tileLengthChunks * 16; }
        public int getTileSize() { return getTileLength() * 3; }
    }

    /**
     * 可见距离配置
     */
    public record VisibilityConfig(int day, int night, int sundown) {
        public static final VisibilityConfig DEFAULT = new VisibilityConfig(400, 200, 300);

        public static final Codec<VisibilityConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("day").forGetter(VisibilityConfig::day),
            Codec.INT.fieldOf("night").forGetter(VisibilityConfig::night),
            Codec.INT.fieldOf("sundown").forGetter(VisibilityConfig::sundown)
        ).apply(instance, VisibilityConfig::new));
    }

    /**
     * 雾效果配置
     */
    public record FogConfig(float start, float endMoving, float endStationary, int nightColor) {
        public static final FogConfig DEFAULT = new FogConfig(32.0f, 96.0f, 64.0f, 0x0D0D14);

        public static final Codec<FogConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.optionalFieldOf("start", 32.0f).forGetter(FogConfig::start),
            Codec.FLOAT.optionalFieldOf("end_moving", 96.0f).forGetter(FogConfig::endMoving),
            Codec.FLOAT.optionalFieldOf("end_stationary", 64.0f).forGetter(FogConfig::endStationary),
            Codec.INT.optionalFieldOf("night_color", 0x0D0D14).forGetter(FogConfig::nightColor)
        ).apply(instance, FogConfig::new));
    }

    /**
     * 相机震动配置
     */
    public record CameraShakeConfig(boolean enabled, float amplitudeIndoor, float amplitudeOutdoor, float strengthIndoor, float strengthOutdoor) {
        public static final CameraShakeConfig DEFAULT = new CameraShakeConfig(true, 0.002f, 0.006f, 0.04f, 0.08f);

        public static final Codec<CameraShakeConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("enabled", true).forGetter(CameraShakeConfig::enabled),
            Codec.FLOAT.optionalFieldOf("amplitude_indoor", 0.002f).forGetter(CameraShakeConfig::amplitudeIndoor),
            Codec.FLOAT.optionalFieldOf("amplitude_outdoor", 0.006f).forGetter(CameraShakeConfig::amplitudeOutdoor),
            Codec.FLOAT.optionalFieldOf("strength_indoor", 0.04f).forGetter(CameraShakeConfig::strengthIndoor),
            Codec.FLOAT.optionalFieldOf("strength_outdoor", 0.08f).forGetter(CameraShakeConfig::strengthOutdoor)
        ).apply(instance, CameraShakeConfig::new));
    }

    /**
     * 雪花粒子配置
     */
    public record SnowParticlesConfig(int count, float spawnOffsetX, float spawnRangeY, float spawnRangeZ) {
        public static final SnowParticlesConfig DEFAULT = new SnowParticlesConfig(350, -180f, 48f, 32f);

        public static final Codec<SnowParticlesConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("count", 350).forGetter(SnowParticlesConfig::count),
            Codec.FLOAT.optionalFieldOf("spawn_offset_x", -180f).forGetter(SnowParticlesConfig::spawnOffsetX),
            Codec.FLOAT.optionalFieldOf("spawn_range_y", 48f).forGetter(SnowParticlesConfig::spawnRangeY),
            Codec.FLOAT.optionalFieldOf("spawn_range_z", 32f).forGetter(SnowParticlesConfig::spawnRangeZ)
        ).apply(instance, SnowParticlesConfig::new));
    }

    public static final Codec<MapEnhancementsConfiguration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        RoomConfig.CODEC.listOf().optionalFieldOf("rooms", List.of()).forGetter(MapEnhancementsConfiguration::rooms),
        // 可选的渲染配置
        SceneryConfig.CODEC.optionalFieldOf("scenery").forGetter(MapEnhancementsConfiguration::scenery),
        VisibilityConfig.CODEC.optionalFieldOf("visibility").forGetter(MapEnhancementsConfiguration::visibility),
        FogConfig.CODEC.optionalFieldOf("fog").forGetter(MapEnhancementsConfiguration::fog),
        CameraShakeConfig.CODEC.optionalFieldOf("camera_shake").forGetter(MapEnhancementsConfiguration::cameraShake)
    ).apply(instance, MapEnhancementsConfiguration::new));

    // ========== 便捷获取方法（带默认值）==========

    public SceneryConfig getSceneryOrDefault() {
        return scenery.orElse(SceneryConfig.DEFAULT);
    }

    public VisibilityConfig getVisibilityOrDefault() {
        return visibility.orElse(VisibilityConfig.DEFAULT);
    }

    public FogConfig getFogOrDefault() {
        return fog.orElse(FogConfig.DEFAULT);
    }

    public CameraShakeConfig getCameraShakeOrDefault() {
        return cameraShake.orElse(CameraShakeConfig.DEFAULT);
    }

    // ========== 房间配置相关方法 ==========

    /**
     * 获取房间数量
     */
    public int getRoomCount() {
        return rooms.size();
    }

    /**
     * 获取房间配置（1-based）
     */
    public Optional<RoomConfig> getRoomConfig(int roomNumber) {
        if (roomNumber < 1 || roomNumber > rooms.size()) {
            return Optional.empty();
        }
        return Optional.of(rooms.get(roomNumber - 1));
    }

    /**
     * 获取指定房间中指定玩家索引的出生点
     * @param roomNumber 房间号（1-based）
     * @param playerIndexInRoom 玩家在房间中的索引（0-based）
     */
    public Optional<RoomConfig.SpawnPoint> getSpawnPointForPlayer(int roomNumber, int playerIndexInRoom) {
        return getRoomConfig(roomNumber)
            .map(room -> room.getSpawnPoint(playerIndexInRoom));
    }

    /**
     * 获取所有房间的总容量
     */
    public int getTotalCapacity() {
        return rooms.stream()
            .mapToInt(RoomConfig::getMaxPlayers)
            .sum();
    }
}
