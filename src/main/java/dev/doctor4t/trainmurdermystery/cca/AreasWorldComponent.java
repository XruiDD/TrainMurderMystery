package dev.doctor4t.trainmurdermystery.cca;

import dev.doctor4t.trainmurdermystery.TMM;
import dev.doctor4t.trainmurdermystery.config.area.AreaConfiguration;
import dev.doctor4t.trainmurdermystery.config.area.AreaConfigurationManager;
import dev.doctor4t.trainmurdermystery.config.area.RoomConfig;
import dev.doctor4t.trainmurdermystery.config.area.SpawnPoint;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;

import java.util.Optional;

public class AreasWorldComponent implements AutoSyncedComponent {
    public static final ComponentKey<AreasWorldComponent> KEY = ComponentRegistry.getOrCreate(TMM.id("areas"), AreasWorldComponent.class);
    private final World world;

    public static class PosWithOrientation {
        public final Vec3d pos;
        public final float yaw;
        public final float pitch;

        public PosWithOrientation(Vec3d pos, float yaw, float pitch) {
            this.pos = pos;
            this.yaw = yaw;
            this.pitch = pitch;
        }

        PosWithOrientation(double x, double y, double z, float yaw, float pitch) {
            this(new Vec3d(x, y, z), yaw, pitch);
        }

    }

    // ========== NBT 辅助方法（用于客户端同步）==========

    private static Vec3d getVec3dFromNbt(NbtCompound tag, String name) {
        return new Vec3d(tag.getDouble(name + "X"), tag.getDouble(name + "Y"), tag.getDouble(name + "Z"));
    }

    private static void writeVec3dToNbt(NbtCompound tag, Vec3d vec3d, String name) {
        tag.putDouble(name + "X", vec3d.getX());
        tag.putDouble(name + "Y", vec3d.getY());
        tag.putDouble(name + "Z", vec3d.getZ());
    }

    private static PosWithOrientation getPosWithOrientationFromNbt(NbtCompound tag, String name) {
        return new PosWithOrientation(
                tag.getDouble(name + "X"), tag.getDouble(name + "Y"), tag.getDouble(name + "Z"),
                tag.getFloat(name + "Yaw"), tag.getFloat(name + "Pitch")
        );
    }

    private static void writePosWithOrientationToNbt(NbtCompound tag, PosWithOrientation pos, String name) {
        tag.putDouble(name + "X", pos.pos.getX());
        tag.putDouble(name + "Y", pos.pos.getY());
        tag.putDouble(name + "Z", pos.pos.getZ());
        tag.putFloat(name + "Yaw", pos.yaw);
        tag.putFloat(name + "Pitch", pos.pitch);
    }

    private static Box getBoxFromNbt(NbtCompound tag, String name) {
        return new Box(
                tag.getDouble(name + "MinX"), tag.getDouble(name + "MinY"), tag.getDouble(name + "MinZ"),
                tag.getDouble(name + "MaxX"), tag.getDouble(name + "MaxY"), tag.getDouble(name + "MaxZ")
        );
    }

    private static void writeBoxToNbt(NbtCompound tag, Box box, String name) {
        tag.putDouble(name + "MinX", box.minX);
        tag.putDouble(name + "MinY", box.minY);
        tag.putDouble(name + "MinZ", box.minZ);
        tag.putDouble(name + "MaxX", box.maxX);
        tag.putDouble(name + "MaxY", box.maxY);
        tag.putDouble(name + "MaxZ", box.maxZ);
    }

    // ========== 客户端同步缓存字段 ==========
    // 这些字段仅用于客户端接收服务端同步的数据
    private PosWithOrientation syncedSpawnPos;
    private PosWithOrientation syncedSpectatorSpawnPos;
    private Box syncedReadyArea;
    private Vec3d syncedPlayAreaOffset;
    private Box syncedPlayArea;
    private Box syncedResetTemplateArea;
    private Box syncedResetPasteArea;

    // ========== Helper 方法 ==========

    private AreaConfiguration getConfigurationOrNull() {
        return AreaConfigurationManager.getInstance().getConfiguration();
    }

    /**
     * 通用的配置获取方法，优先从配置管理器获取，否则返回同步缓存值
     */
    private <T> T getFromConfigOrSynced(java.util.function.Function<AreaConfiguration, T> configExtractor, T syncedValue) {
        AreaConfiguration config = getConfigurationOrNull();
        return config != null ? configExtractor.apply(config) : syncedValue;
    }

    // ========== Getter 方法 ==========
    // 服务端：从 AreaConfigurationManager 获取
    // 客户端：从同步缓存字段获取

    @Nullable
    public PosWithOrientation getSpawnPos() {
        return getFromConfigOrSynced(c -> c.spawnPos().toPosWithOrientation(), syncedSpawnPos);
    }

    @Nullable
    public PosWithOrientation getSpectatorSpawnPos() {
        return getFromConfigOrSynced(c -> c.spectatorSpawnPos().toPosWithOrientation(), syncedSpectatorSpawnPos);
    }

    @Nullable
    public Box getReadyArea() {
        return getFromConfigOrSynced(c -> c.readyArea().toBox(), syncedReadyArea);
    }

    @Nullable
    public Vec3d getPlayAreaOffset() {
        return getFromConfigOrSynced(c -> c.playAreaOffset().toVec3d(), syncedPlayAreaOffset);
    }

    @Nullable
    public Box getPlayArea() {
        return getFromConfigOrSynced(c -> c.playArea().toBox(), syncedPlayArea);
    }

    @Nullable
    public Box getResetTemplateArea() {
        return getFromConfigOrSynced(c -> c.resetTemplateArea().toBox(), syncedResetTemplateArea);
    }

    @Nullable
    public Box getResetPasteArea() {
        return getFromConfigOrSynced(c -> c.resetPasteArea().toBox(), syncedResetPasteArea);
    }

    // ========== Setter 方法 ==========
    // 这些方法用于设置同步缓存字段，主要用于运行时动态修改

    public void setSpawnPos(PosWithOrientation spawnPos) {
        this.syncedSpawnPos = spawnPos;
        this.sync();
    }

    public void setSpectatorSpawnPos(PosWithOrientation spectatorSpawnPos) {
        this.syncedSpectatorSpawnPos = spectatorSpawnPos;
        this.sync();
    }

    public void setReadyArea(Box readyArea) {
        this.syncedReadyArea = readyArea;
        this.sync();
    }

    public void setPlayAreaOffset(Vec3d playAreaOffset) {
        this.syncedPlayAreaOffset = playAreaOffset;
        this.sync();
    }

    public void setPlayArea(Box playArea) {
        this.syncedPlayArea = playArea;
        this.sync();
    }

    public void setResetTemplateArea(Box resetTemplateArea) {
        this.syncedResetTemplateArea = resetTemplateArea;
        this.sync();
    }

    public void setResetPasteArea(Box resetPasteArea) {
        this.syncedResetPasteArea = resetPasteArea;
        this.sync();
    }

    // ========== 房间配置相关方法 ==========

    /**
     * 获取房间数量
     * 如果没有配置则返回0
     */
    public int getRoomCount() {
        AreaConfiguration config = AreaConfigurationManager.getInstance().getConfiguration();
        return config != null ? config.getRoomCount() : 0;
    }

    /**
     * 获取房间配置（1-based）
     */
    public Optional<RoomConfig> getRoomConfig(int roomNumber) {
        AreaConfiguration config = AreaConfigurationManager.getInstance().getConfiguration();
        if (config != null) {
            return config.getRoomConfig(roomNumber);
        }
        return Optional.empty();
    }

    /**
     * 获取指定房间中指定玩家索引的出生点
     * @param roomNumber 房间号（1-based）
     * @param playerIndexInRoom 玩家在房间中的索引（0-based）
     */
    public Optional<SpawnPoint> getSpawnPointForPlayer(int roomNumber, int playerIndexInRoom) {
        AreaConfiguration config = AreaConfigurationManager.getInstance().getConfiguration();
        if (config != null) {
            return config.getSpawnPointForPlayer(roomNumber, playerIndexInRoom);
        }
        return Optional.empty();
    }

    /**
     * 获取所有房间的总容量
     * 如果没有配置则返回0
     */
    public int getTotalRoomCapacity() {
        AreaConfiguration config = AreaConfigurationManager.getInstance().getConfiguration();
        return config != null ? config.getTotalCapacity() : 0;
    }

    /**
     * 是否已加载区域配置
     */
    public boolean hasAreaConfiguration() {
        return AreaConfigurationManager.getInstance().hasConfiguration();
    }

    public AreasWorldComponent(World world) {
        this.world = world;
    }

    public void sync() {
        KEY.sync(this.world);
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.@NotNull WrapperLookup registryLookup) {
        // 客户端从 NBT 读取同步数据
        if (tag.contains("spawnPosX")) {
            this.syncedSpawnPos = getPosWithOrientationFromNbt(tag, "spawnPos");
        }
        if (tag.contains("spectatorSpawnPosX")) {
            this.syncedSpectatorSpawnPos = getPosWithOrientationFromNbt(tag, "spectatorSpawnPos");
        }
        if (tag.contains("readyAreaMinX")) {
            this.syncedReadyArea = getBoxFromNbt(tag, "readyArea");
        }
        if (tag.contains("playAreaOffsetX")) {
            this.syncedPlayAreaOffset = getVec3dFromNbt(tag, "playAreaOffset");
        }
        if (tag.contains("playAreaMinX")) {
            this.syncedPlayArea = getBoxFromNbt(tag, "playArea");
        }
        if (tag.contains("resetTemplateAreaMinX")) {
            this.syncedResetTemplateArea = getBoxFromNbt(tag, "resetTemplateArea");
        }
        if (tag.contains("resetPasteAreaMinX")) {
            this.syncedResetPasteArea = getBoxFromNbt(tag, "resetPasteArea");
        }
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.@NotNull WrapperLookup registryLookup) {
        // 服务端从配置管理器获取数据写入 NBT 用于同步
        AreaConfiguration config = AreaConfigurationManager.getInstance().getConfiguration();
        if (config != null) {
            writePosWithOrientationToNbt(tag, config.spawnPos().toPosWithOrientation(), "spawnPos");
            writePosWithOrientationToNbt(tag, config.spectatorSpawnPos().toPosWithOrientation(), "spectatorSpawnPos");
            writeBoxToNbt(tag, config.readyArea().toBox(), "readyArea");
            writeVec3dToNbt(tag, config.playAreaOffset().toVec3d(), "playAreaOffset");
            writeBoxToNbt(tag, config.playArea().toBox(), "playArea");
            writeBoxToNbt(tag, config.resetTemplateArea().toBox(), "resetTemplateArea");
            writeBoxToNbt(tag, config.resetPasteArea().toBox(), "resetPasteArea");
        }
    }
}
