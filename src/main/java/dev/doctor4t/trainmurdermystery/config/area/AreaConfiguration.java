package dev.doctor4t.trainmurdermystery.config.area;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.doctor4t.trainmurdermystery.cca.AreasWorldComponent.PosWithOrientation;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.Optional;

/**
 * 完整的地图区域配置
 * 支持从 datapack JSON 加载
 */
public record AreaConfiguration(
    PosData spawnPos,
    PosData spectatorSpawnPos,
    BoxData readyArea,
    Vec3Data playAreaOffset,
    BoxData playArea,
    BoxData resetTemplateArea,
    BoxData resetPasteArea,
    List<RoomConfig> rooms,
    // 可选的渲染配置
    Optional<SceneryConfig> scenery,
    Optional<VisibilityConfig> visibility,
    Optional<FogConfig> fog,
    Optional<CameraShakeConfig> cameraShake,
    Optional<SnowParticlesConfig> snowParticles,
    Optional<VisualConfig> visual
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
     * 可见距离配置（按时间段）
     */
    public record VisibilityConfig(int day, int night, int sundown) {
        public static final VisibilityConfig DEFAULT = new VisibilityConfig(160, 160, 320);

        public static final Codec<VisibilityConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("day").forGetter(VisibilityConfig::day),
            Codec.INT.fieldOf("night").forGetter(VisibilityConfig::night),
            Codec.INT.fieldOf("sundown").forGetter(VisibilityConfig::sundown)
        ).apply(instance, VisibilityConfig::new));
    }

    /**
     * 雾效果配置
     */
    public record FogConfig(boolean enabled, float start, float endMoving, float endStationary, int nightColor) {
        public static final FogConfig DEFAULT = new FogConfig(true, 0f, 130f, 200f, 0xE406060B);

        public static final Codec<FogConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("enabled", true).forGetter(FogConfig::enabled),
            Codec.FLOAT.fieldOf("start").forGetter(FogConfig::start),
            Codec.FLOAT.fieldOf("end_moving").forGetter(FogConfig::endMoving),
            Codec.FLOAT.fieldOf("end_stationary").forGetter(FogConfig::endStationary),
            Codec.STRING.fieldOf("night_color").xmap(
                s -> Integer.parseUnsignedInt(s, 16),
                i -> String.format("%08X", i)
            ).forGetter(FogConfig::nightColor)
        ).apply(instance, FogConfig::new));
    }

    /**
     * 相机震动配置
     */
    public record CameraShakeConfig(boolean enabled, float amplitudeIndoor, float amplitudeOutdoor, float strengthIndoor, float strengthOutdoor) {
        public static final CameraShakeConfig DEFAULT = new CameraShakeConfig(true, 0.0025f, 0.01f, 0.5f, 1.0f);

        public static final Codec<CameraShakeConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.fieldOf("enabled").forGetter(CameraShakeConfig::enabled),
            Codec.FLOAT.fieldOf("amplitude_indoor").forGetter(CameraShakeConfig::amplitudeIndoor),
            Codec.FLOAT.fieldOf("amplitude_outdoor").forGetter(CameraShakeConfig::amplitudeOutdoor),
            Codec.FLOAT.fieldOf("strength_indoor").forGetter(CameraShakeConfig::strengthIndoor),
            Codec.FLOAT.fieldOf("strength_outdoor").forGetter(CameraShakeConfig::strengthOutdoor)
        ).apply(instance, CameraShakeConfig::new));
    }

    /**
     * 雪花粒子配置
     */
    public record SnowParticlesConfig(boolean enabled, int count, float spawnOffsetX, float spawnRangeY, float spawnRangeZ) {
        public static final SnowParticlesConfig DEFAULT = new SnowParticlesConfig(true, 200, -20f, 10f, 10f);

        public static final Codec<SnowParticlesConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("enabled", true).forGetter(SnowParticlesConfig::enabled),
            Codec.INT.fieldOf("count").forGetter(SnowParticlesConfig::count),
            Codec.FLOAT.fieldOf("spawn_offset_x").forGetter(SnowParticlesConfig::spawnOffsetX),
            Codec.FLOAT.fieldOf("spawn_range_y").forGetter(SnowParticlesConfig::spawnRangeY),
            Codec.FLOAT.fieldOf("spawn_range_z").forGetter(SnowParticlesConfig::spawnRangeZ)
        ).apply(instance, SnowParticlesConfig::new));
    }

    /**
     * 视觉效果配置 - 列车运行时的默认视觉参数
     */
    public record VisualConfig(boolean staticMap, boolean hud, int trainSpeed, String timeOfDay) {
        public static final VisualConfig DEFAULT = new VisualConfig(false, true, 130, "NIGHT");

        public static final Codec<VisualConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("static_map", false).forGetter(VisualConfig::staticMap),
            Codec.BOOL.optionalFieldOf("hud", true).forGetter(VisualConfig::hud),
            Codec.INT.optionalFieldOf("train_speed", 130).forGetter(VisualConfig::trainSpeed),
            Codec.STRING.optionalFieldOf("time_of_day", "NIGHT").forGetter(VisualConfig::timeOfDay)
        ).apply(instance, VisualConfig::new));
    }

    /**
     * 位置+朝向数据
     */
    public record PosData(double x, double y, double z, float yaw, float pitch) {
        public static final Codec<PosData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.DOUBLE.fieldOf("x").forGetter(PosData::x),
            Codec.DOUBLE.fieldOf("y").forGetter(PosData::y),
            Codec.DOUBLE.fieldOf("z").forGetter(PosData::z),
            Codec.FLOAT.fieldOf("yaw").forGetter(PosData::yaw),
            Codec.FLOAT.fieldOf("pitch").forGetter(PosData::pitch)
        ).apply(instance, PosData::new));

        public PosWithOrientation toPosWithOrientation() {
            return new PosWithOrientation(new Vec3d(x, y, z), yaw, pitch);
        }
    }

    /**
     * Vec3d 数据
     */
    public record Vec3Data(double x, double y, double z) {
        public static final Codec<Vec3Data> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.DOUBLE.fieldOf("x").forGetter(Vec3Data::x),
            Codec.DOUBLE.fieldOf("y").forGetter(Vec3Data::y),
            Codec.DOUBLE.fieldOf("z").forGetter(Vec3Data::z)
        ).apply(instance, Vec3Data::new));

        public Vec3d toVec3d() {
            return new Vec3d(x, y, z);
        }
    }

    /**
     * Box 数据
     */
    public record BoxData(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        public static final Codec<BoxData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.DOUBLE.fieldOf("minX").forGetter(BoxData::minX),
            Codec.DOUBLE.fieldOf("minY").forGetter(BoxData::minY),
            Codec.DOUBLE.fieldOf("minZ").forGetter(BoxData::minZ),
            Codec.DOUBLE.fieldOf("maxX").forGetter(BoxData::maxX),
            Codec.DOUBLE.fieldOf("maxY").forGetter(BoxData::maxY),
            Codec.DOUBLE.fieldOf("maxZ").forGetter(BoxData::maxZ)
        ).apply(instance, BoxData::new));

        public Box toBox() {
            return new Box(minX, minY, minZ, maxX, maxY, maxZ);
        }
    }

    public static final Codec<AreaConfiguration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        PosData.CODEC.fieldOf("spawn_pos").forGetter(AreaConfiguration::spawnPos),
        PosData.CODEC.fieldOf("spectator_spawn_pos").forGetter(AreaConfiguration::spectatorSpawnPos),
        BoxData.CODEC.fieldOf("ready_area").forGetter(AreaConfiguration::readyArea),
        Vec3Data.CODEC.fieldOf("play_area_offset").forGetter(AreaConfiguration::playAreaOffset),
        BoxData.CODEC.fieldOf("play_area").forGetter(AreaConfiguration::playArea),
        BoxData.CODEC.fieldOf("reset_template_area").forGetter(AreaConfiguration::resetTemplateArea),
        BoxData.CODEC.fieldOf("reset_paste_area").forGetter(AreaConfiguration::resetPasteArea),
        RoomConfig.CODEC.listOf().fieldOf("rooms").forGetter(AreaConfiguration::rooms),
        // 可选的渲染配置
        SceneryConfig.CODEC.optionalFieldOf("scenery").forGetter(AreaConfiguration::scenery),
        VisibilityConfig.CODEC.optionalFieldOf("visibility").forGetter(AreaConfiguration::visibility),
        FogConfig.CODEC.optionalFieldOf("fog").forGetter(AreaConfiguration::fog),
        CameraShakeConfig.CODEC.optionalFieldOf("camera_shake").forGetter(AreaConfiguration::cameraShake),
        SnowParticlesConfig.CODEC.optionalFieldOf("snow_particles").forGetter(AreaConfiguration::snowParticles),
        VisualConfig.CODEC.optionalFieldOf("visual").forGetter(AreaConfiguration::visual)
    ).apply(instance, AreaConfiguration::new));

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

    public SnowParticlesConfig getSnowParticlesOrDefault() {
        return snowParticles.orElse(SnowParticlesConfig.DEFAULT);
    }

    public VisualConfig getVisualOrDefault() {
        return visual.orElse(VisualConfig.DEFAULT);
    }

    /**
     * 获取房间数量
     */
    public int getRoomCount() {
        return rooms.size();
    }

    /**
     * 根据房间号获取房间配置（1-based）
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
    public Optional<SpawnPoint> getSpawnPointForPlayer(int roomNumber, int playerIndexInRoom) {
        return getRoomConfig(roomNumber).map(room -> room.getSpawnPoint(playerIndexInRoom));
    }

    /**
     * 计算所有房间的总容量
     */
    public int getTotalCapacity() {
        return rooms.stream().mapToInt(RoomConfig::getMaxPlayers).sum();
    }
}
