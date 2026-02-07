package dev.doctor4t.wathe.config.datapack;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.datafixers.util.Either;
import net.minecraft.block.Block;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
    Optional<CameraShakeConfig> cameraShake,
    // 交互黑名单
    Optional<InteractionBlacklistConfig> interactionBlacklist,
    // 游戏参数配置
    Optional<MovementConfig> movement,
    Optional<JumpConfig> jump,
    Optional<AmbienceConfig> ambience
) {

    /**
     * 风景配置
     */
    public record SceneryConfig(int heightOffset, int minX, int maxX, int minZ, int maxZ) {
        public static final SceneryConfig DEFAULT = new SceneryConfig(116, -208, 303, -896, -177);

        public static final Codec<SceneryConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("height_offset", 116).forGetter(SceneryConfig::heightOffset),
            Codec.INT.optionalFieldOf("min_x", -208).forGetter(SceneryConfig::minX),
            Codec.INT.optionalFieldOf("max_x", 303).forGetter(SceneryConfig::maxX),
            Codec.INT.optionalFieldOf("min_z", -896).forGetter(SceneryConfig::minZ),
            Codec.INT.optionalFieldOf("max_z", -177).forGetter(SceneryConfig::maxZ)
        ).apply(instance, SceneryConfig::new));
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

        private static final Codec<Integer> NIGHT_COLOR_CODEC = Codec.either(Codec.INT, Codec.STRING)
            .flatXmap(
                either -> either.map(DataResult::success, FogConfig::parseNightColor),
                value -> DataResult.success(Either.left(value))
            );

        public static final Codec<FogConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.optionalFieldOf("start", 32.0f).forGetter(FogConfig::start),
            Codec.FLOAT.optionalFieldOf("end_moving", 96.0f).forGetter(FogConfig::endMoving),
            Codec.FLOAT.optionalFieldOf("end_stationary", 64.0f).forGetter(FogConfig::endStationary),
            NIGHT_COLOR_CODEC.optionalFieldOf("night_color", 0x0D0D14).forGetter(FogConfig::nightColor)
        ).apply(instance, FogConfig::new));

        private static DataResult<Integer> parseNightColor(String raw) {
            String value = raw.trim();
            if (value.startsWith("#")) {
                value = value.substring(1);
            } else if (value.startsWith("0x") || value.startsWith("0X")) {
                value = value.substring(2);
            }
            if (value.isEmpty()) {
                return DataResult.error(() -> "night_color hex string is empty");
            }
            if (!value.matches("[0-9a-fA-F]+")) {
                return DataResult.error(() -> "night_color must be a hex string like #RRGGBB or 0xAARRGGBB");
            }
            if (value.length() > 8) {
                return DataResult.error(() -> "night_color hex string is too long (max 8 hex digits)");
            }
            try {
                int parsed = (int) Long.parseLong(value, 16);
                return DataResult.success(parsed);
            } catch (NumberFormatException e) {
                return DataResult.error(() -> "Invalid night_color hex string: " + raw);
            }
        }
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

    /**
     * 移动配置
     */
    public record MovementConfig(float walkSpeedMultiplier, float sprintSpeedMultiplier) {
        public static final MovementConfig DEFAULT = new MovementConfig(1.0f, 1.0f);

        public static final Codec<MovementConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.optionalFieldOf("walk_speed_multiplier", 1.0f).forGetter(MovementConfig::walkSpeedMultiplier),
            Codec.FLOAT.optionalFieldOf("sprint_speed_multiplier", 1.0f).forGetter(MovementConfig::sprintSpeedMultiplier)
        ).apply(instance, MovementConfig::new));
    }

    /**
     * 跳跃配置
     */
    public record JumpConfig(boolean allowed, float staminaCost) {
        public static final JumpConfig DEFAULT = new JumpConfig(false, 0f);

        public static final Codec<JumpConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("allowed", false).forGetter(JumpConfig::allowed),
            Codec.FLOAT.optionalFieldOf("stamina_cost", 0f).forGetter(JumpConfig::staminaCost)
        ).apply(instance, JumpConfig::new));
    }

    /**
     * 环境音配置
     * 默认使用列车室内/室外音效，地图可覆盖为其他音效或设为空以关闭
     */
    public record AmbienceConfig(boolean requireTrainMoving, Optional<String> insideSound, Optional<String> outsideSound) {
        public static final String DEFAULT_INSIDE_SOUND = "wathe:ambient.train.inside";
        public static final String DEFAULT_OUTSIDE_SOUND = "wathe:ambient.train.outside";
        public static final AmbienceConfig DEFAULT = new AmbienceConfig(true, Optional.of(DEFAULT_INSIDE_SOUND), Optional.of(DEFAULT_OUTSIDE_SOUND));

        public static final Codec<AmbienceConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("require_train_moving", true).forGetter(AmbienceConfig::requireTrainMoving),
            Codec.STRING.optionalFieldOf("inside_sound", DEFAULT_INSIDE_SOUND).xmap(
                s -> s.isEmpty() ? Optional.<String>empty() : Optional.of(s),
                opt -> opt.orElse("")
            ).forGetter(AmbienceConfig::insideSound),
            Codec.STRING.optionalFieldOf("outside_sound", DEFAULT_OUTSIDE_SOUND).xmap(
                s -> s.isEmpty() ? Optional.<String>empty() : Optional.of(s),
                opt -> opt.orElse("")
            ).forGetter(AmbienceConfig::outsideSound)
        ).apply(instance, AmbienceConfig::new));
    }

    /**
     * 右键交互方块黑名单配置
     * 支持单个方块ID和方块标签
     */
    public record InteractionBlacklistConfig(List<String> blocks, List<String> blockTags) {
        public static final InteractionBlacklistConfig DEFAULT = new InteractionBlacklistConfig(List.of(), List.of());

        public static final Codec<InteractionBlacklistConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.listOf().optionalFieldOf("blocks", List.of()).forGetter(InteractionBlacklistConfig::blocks),
            Codec.STRING.listOf().optionalFieldOf("block_tags", List.of()).forGetter(InteractionBlacklistConfig::blockTags)
        ).apply(instance, InteractionBlacklistConfig::new));

        /**
         * 检查指定方块是否在黑名单中
         */
        public boolean isBlacklisted(Block block) {
            Identifier blockId = Registries.BLOCK.getId(block);

            // 检查单个方块ID
            for (String id : blocks) {
                if (blockId.toString().equals(id)) {
                    return true;
                }
            }

            // 检查方块标签
            for (String tagName : blockTags) {
                TagKey<Block> tagKey = TagKey.of(RegistryKeys.BLOCK, Identifier.tryParse(tagName));
                if (block.getDefaultState().isIn(tagKey)) {
                    return true;
                }
            }

            return false;
        }

        /**
         * 获取所有黑名单方块的集合（用于快速查找）
         */
        public Set<Block> getBlacklistedBlocks() {
            Set<Block> result = new HashSet<>();

            // 添加单个方块
            for (String id : blocks) {
                Identifier identifier = Identifier.tryParse(id);
                if (identifier != null) {
                    Block block = Registries.BLOCK.get(identifier);
                    if (block != null) {
                        result.add(block);
                    }
                }
            }

            // 添加标签中的方块
            for (String tagName : blockTags) {
                TagKey<Block> tagKey = TagKey.of(RegistryKeys.BLOCK, Identifier.tryParse(tagName));
                Registries.BLOCK.iterateEntries(tagKey).forEach(entry -> result.add(entry.value()));
            }

            return result;
        }
    }

    public static final Codec<MapEnhancementsConfiguration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        RoomConfig.CODEC.listOf().optionalFieldOf("rooms", List.of()).forGetter(MapEnhancementsConfiguration::rooms),
        // 可选的渲染配置
        SceneryConfig.CODEC.optionalFieldOf("scenery").forGetter(MapEnhancementsConfiguration::scenery),
        VisibilityConfig.CODEC.optionalFieldOf("visibility").forGetter(MapEnhancementsConfiguration::visibility),
        FogConfig.CODEC.optionalFieldOf("fog").forGetter(MapEnhancementsConfiguration::fog),
        CameraShakeConfig.CODEC.optionalFieldOf("camera_shake").forGetter(MapEnhancementsConfiguration::cameraShake),
        // 交互黑名单
        InteractionBlacklistConfig.CODEC.optionalFieldOf("interaction_blacklist").forGetter(MapEnhancementsConfiguration::interactionBlacklist),
        // 游戏参数配置
        MovementConfig.CODEC.optionalFieldOf("movement").forGetter(MapEnhancementsConfiguration::movement),
        JumpConfig.CODEC.optionalFieldOf("jump").forGetter(MapEnhancementsConfiguration::jump),
        AmbienceConfig.CODEC.optionalFieldOf("ambience").forGetter(MapEnhancementsConfiguration::ambience)
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

    public InteractionBlacklistConfig getInteractionBlacklistOrDefault() {
        return interactionBlacklist.orElse(InteractionBlacklistConfig.DEFAULT);
    }

    public MovementConfig getMovementOrDefault() {
        return movement.orElse(MovementConfig.DEFAULT);
    }

    public JumpConfig getJumpOrDefault() {
        return jump.orElse(JumpConfig.DEFAULT);
    }

    public AmbienceConfig getAmbienceOrDefault() {
        return ambience.orElse(AmbienceConfig.DEFAULT);
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
