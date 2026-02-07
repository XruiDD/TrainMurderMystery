package dev.doctor4t.wathe.cca;

import dev.doctor4t.wathe.Wathe;
import dev.doctor4t.wathe.config.datapack.MapEnhancementsConfiguration;
import dev.doctor4t.wathe.config.datapack.MapEnhancementsConfiguration.SceneryConfig;
import dev.doctor4t.wathe.config.datapack.MapEnhancementsConfiguration.VisibilityConfig;
import dev.doctor4t.wathe.config.datapack.MapEnhancementsConfiguration.FogConfig;
import dev.doctor4t.wathe.config.datapack.MapEnhancementsConfiguration.CameraShakeConfig;
import dev.doctor4t.wathe.config.datapack.MapEnhancementsConfiguration.SnowParticlesConfig;
import dev.doctor4t.wathe.config.datapack.MapEnhancementsConfiguration.InteractionBlacklistConfig;
import dev.doctor4t.wathe.config.datapack.MapEnhancementsConfiguration.MovementConfig;
import dev.doctor4t.wathe.config.datapack.MapEnhancementsConfiguration.JumpConfig;
import dev.doctor4t.wathe.config.datapack.MapEnhancementsConfiguration.AmbienceConfig;
import dev.doctor4t.wathe.config.datapack.MapEnhancementsConfigurationManager;
import dev.doctor4t.wathe.config.datapack.RoomConfig;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Identifier;
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
    public static final ComponentKey<MapEnhancementsWorldComponent> KEY = ComponentRegistry.getOrCreate(Wathe.id("map_enhancements"), MapEnhancementsWorldComponent.class);
    private final World world;

    public MapEnhancementsWorldComponent(World world) {
        this.world = world;
    }

    public void sync() {
        KEY.sync(this.world);
    }

    /**
     * 获取当前世界维度对应的地图配置
     */
    private MapEnhancementsConfiguration getConfigForCurrentWorld() {
        Identifier dimId = world.getRegistryKey().getValue();
        MapEnhancementsConfiguration config = MapEnhancementsConfigurationManager.getInstance().getConfiguration(dimId);
        if (config != null) return config;
        // Fallback: 无参版本（overworld 兼容）
        return MapEnhancementsConfigurationManager.getInstance().getConfiguration();
    }

    // ========== 客户端同步缓存字段 ==========
    // 服务端从数据包读取，通过NBT同步到客户端
    private SceneryConfig syncedScenery;
    private VisibilityConfig syncedVisibility;
    private FogConfig syncedFog;
    private CameraShakeConfig syncedCameraShake;
    private InteractionBlacklistConfig syncedInteractionBlacklist;
    private MovementConfig syncedMovement;
    private JumpConfig syncedJump;
    private AmbienceConfig syncedAmbience;
    // ========== 渲染配置 Getter 方法 ==========

    public SceneryConfig getSceneryConfig() {
        // 服务端从数据包读取，客户端使用同步的数据
        if (world.isClient() && syncedScenery != null) {
            return syncedScenery;
        }
        MapEnhancementsConfiguration config = getConfigForCurrentWorld();
        return config != null ? config.getSceneryOrDefault() : SceneryConfig.DEFAULT;
    }

    public VisibilityConfig getVisibilityConfig() {
        if (world.isClient() && syncedVisibility != null) {
            return syncedVisibility;
        }
        MapEnhancementsConfiguration config = getConfigForCurrentWorld();
        return config != null ? config.getVisibilityOrDefault() : VisibilityConfig.DEFAULT;
    }

    public FogConfig getFogConfig() {
        if (world.isClient() && syncedFog != null) {
            return syncedFog;
        }
        MapEnhancementsConfiguration config = getConfigForCurrentWorld();
        return config != null ? config.getFogOrDefault() : FogConfig.DEFAULT;
    }

    public CameraShakeConfig getCameraShakeConfig() {
        if (world.isClient() && syncedCameraShake != null) {
            return syncedCameraShake;
        }
        MapEnhancementsConfiguration config = getConfigForCurrentWorld();
        return config != null ? config.getCameraShakeOrDefault() : CameraShakeConfig.DEFAULT;
    }

    /**
     * 获取交互黑名单配置（同步到客户端）
     */
    public InteractionBlacklistConfig getInteractionBlacklistConfig() {
        if (world.isClient() && syncedInteractionBlacklist != null) {
            return syncedInteractionBlacklist;
        }
        MapEnhancementsConfiguration config = getConfigForCurrentWorld();
        return config != null ? config.getInteractionBlacklistOrDefault() : InteractionBlacklistConfig.DEFAULT;
    }

    public MovementConfig getMovementConfig() {
        if (world.isClient() && syncedMovement != null) {
            return syncedMovement;
        }
        MapEnhancementsConfiguration config = getConfigForCurrentWorld();
        return config != null ? config.getMovementOrDefault() : MovementConfig.DEFAULT;
    }

    public JumpConfig getJumpConfig() {
        if (world.isClient() && syncedJump != null) {
            return syncedJump;
        }
        MapEnhancementsConfiguration config = getConfigForCurrentWorld();
        return config != null ? config.getJumpOrDefault() : JumpConfig.DEFAULT;
    }

    public AmbienceConfig getAmbienceConfig() {
        if (world.isClient() && syncedAmbience != null) {
            return syncedAmbience;
        }
        MapEnhancementsConfiguration config = getConfigForCurrentWorld();
        return config != null ? config.getAmbienceOrDefault() : AmbienceConfig.DEFAULT;
    }

    // ========== 房间配置相关方法（仅服务端）==========

    public int getRoomCount() {
        MapEnhancementsConfiguration config = getConfigForCurrentWorld();
        return config != null ? config.getRoomCount() : 0;
    }

    public Optional<RoomConfig> getRoomConfig(int roomNumber) {
        MapEnhancementsConfiguration config = getConfigForCurrentWorld();
        if (config != null) {
            return config.getRoomConfig(roomNumber);
        }
        return Optional.empty();
    }

    public Optional<RoomConfig.SpawnPoint> getSpawnPointForPlayer(int roomNumber, int playerIndexInRoom) {
        MapEnhancementsConfiguration config = getConfigForCurrentWorld();
        if (config != null) {
            return config.getSpawnPointForPlayer(roomNumber, playerIndexInRoom);
        }
        return Optional.empty();
    }

    public int getTotalRoomCapacity() {
        MapEnhancementsConfiguration config = getConfigForCurrentWorld();
        return config != null ? config.getTotalCapacity() : 0;
    }

    public boolean hasAreaConfiguration() {
        return getConfigForCurrentWorld() != null;
    }

    // ========== NBT 序列化（同步渲染配置到客户端）==========

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.@NotNull WrapperLookup registryLookup) {
        // 客户端读取服务端同步的渲染配置
        if (tag.contains("sceneryHeightOffset")) {
            this.syncedScenery = new SceneryConfig(
                tag.getInt("sceneryHeightOffset"),
                tag.getInt("sceneryMinX"),
                tag.getInt("sceneryMaxX"),
                tag.getInt("sceneryMinZ"),
                tag.getInt("sceneryMaxZ")
            );
        }
        if (tag.contains("visibilityDay")) {
            this.syncedVisibility = new VisibilityConfig(
                tag.getInt("visibilityDay"),
                tag.getInt("visibilityNight"),
                tag.getInt("visibilitySundown")
            );
        }
        if (tag.contains("fogStart")) {
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
        // 反序列化交互黑名单配置
        if (tag.contains("blacklistBlocksCount")) {
            int blocksCount = tag.getInt("blacklistBlocksCount");
            java.util.List<String> blocks = new java.util.ArrayList<>();
            for (int i = 0; i < blocksCount; i++) {
                blocks.add(tag.getString("blacklistBlock_" + i));
            }

            int tagsCount = tag.getInt("blacklistTagsCount");
            java.util.List<String> blockTags = new java.util.ArrayList<>();
            for (int i = 0; i < tagsCount; i++) {
                blockTags.add(tag.getString("blacklistTag_" + i));
            }

            this.syncedInteractionBlacklist = new InteractionBlacklistConfig(blocks, blockTags);
        }
        // 反序列化移动配置
        if (tag.contains("movementWalkMultiplier")) {
            this.syncedMovement = new MovementConfig(
                tag.getFloat("movementWalkMultiplier"),
                tag.getFloat("movementSprintMultiplier")
            );
        }
        // 反序列化跳跃配置
        if (tag.contains("jumpAllowed")) {
            this.syncedJump = new JumpConfig(
                tag.getBoolean("jumpAllowed"),
                tag.getFloat("jumpStaminaCost")
            );
        }
        // 反序列化环境音配置
        if (tag.contains("ambienceInsideSound")) {
            boolean requireTrainMoving = !tag.contains("ambienceRequireTrainMoving") || tag.getBoolean("ambienceRequireTrainMoving");
            String inside = tag.getString("ambienceInsideSound");
            String outside = tag.getString("ambienceOutsideSound");
            this.syncedAmbience = new AmbienceConfig(
                requireTrainMoving,
                inside.isEmpty() ? java.util.Optional.empty() : java.util.Optional.of(inside),
                outside.isEmpty() ? java.util.Optional.empty() : java.util.Optional.of(outside)
            );
        }
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.@NotNull WrapperLookup registryLookup) {
        // 服务端从数据包读取配置并写入NBT，同步到客户端
        SceneryConfig scenery = getSceneryConfig();
        tag.putInt("sceneryHeightOffset", scenery.heightOffset());
        tag.putInt("sceneryMinX", scenery.minX());
        tag.putInt("sceneryMaxX", scenery.maxX());
        tag.putInt("sceneryMinZ", scenery.minZ());
        tag.putInt("sceneryMaxZ", scenery.maxZ());

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

        // 序列化交互黑名单配置
        InteractionBlacklistConfig blacklist = getInteractionBlacklistConfig();
        tag.putInt("blacklistBlocksCount", blacklist.blocks().size());
        for (int i = 0; i < blacklist.blocks().size(); i++) {
            tag.putString("blacklistBlock_" + i, blacklist.blocks().get(i));
        }
        tag.putInt("blacklistTagsCount", blacklist.blockTags().size());
        for (int i = 0; i < blacklist.blockTags().size(); i++) {
            tag.putString("blacklistTag_" + i, blacklist.blockTags().get(i));
        }

        // 序列化移动配置
        MovementConfig movement = getMovementConfig();
        tag.putFloat("movementWalkMultiplier", movement.walkSpeedMultiplier());
        tag.putFloat("movementSprintMultiplier", movement.sprintSpeedMultiplier());

        // 序列化跳跃配置
        JumpConfig jump = getJumpConfig();
        tag.putBoolean("jumpAllowed", jump.allowed());
        tag.putFloat("jumpStaminaCost", jump.staminaCost());

        // 序列化环境音配置
        AmbienceConfig ambience = getAmbienceConfig();
        tag.putBoolean("ambienceRequireTrainMoving", ambience.requireTrainMoving());
        tag.putString("ambienceInsideSound", ambience.insideSound().orElse(""));
        tag.putString("ambienceOutsideSound", ambience.outsideSound().orElse(""));
    }
}
