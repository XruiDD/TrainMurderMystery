package dev.doctor4t.wathe.config.datapack;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.Optional;

/**
 * 房间配置
 */
public record RoomConfig(
    List<SpawnPoint> spawnPoints,
    Optional<Integer> maxPlayers,
    Optional<String> name
) {
    /**
     * 出生点配置
     */
    public record SpawnPoint(
            double x,
            double y,
            double z,
            float yaw,
            float pitch
    ) {
        public static final Codec<SpawnPoint> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.DOUBLE.fieldOf("x").forGetter(SpawnPoint::x),
                Codec.DOUBLE.fieldOf("y").forGetter(SpawnPoint::y),
                Codec.DOUBLE.fieldOf("z").forGetter(SpawnPoint::z),
                Codec.FLOAT.fieldOf("yaw").forGetter(SpawnPoint::yaw),
                Codec.FLOAT.fieldOf("pitch").forGetter(SpawnPoint::pitch)
        ).apply(instance, SpawnPoint::new));

        public Vec3d toVec3d() {
            return new Vec3d(x, y, z);
        }
    }
    public static final Codec<RoomConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        SpawnPoint.CODEC.listOf().fieldOf("spawn_points").forGetter(RoomConfig::spawnPoints),
        Codec.INT.optionalFieldOf("max_players").forGetter(RoomConfig::maxPlayers),
        Codec.STRING.optionalFieldOf("name").forGetter(RoomConfig::name)
    ).apply(instance, RoomConfig::new));

    /**
     * 获取房间名称
     * 如果没有设置则返回 "Room X"（X为房间号）
     * @param roomNumber 房间号（1-based）
     */
    public String getName(int roomNumber) {
        return name.orElse("Room " + roomNumber);
    }

    /**
     * 获取房间最大玩家数
     * 如果没有设置则返回出生点数量
     */
    public int getMaxPlayers() {
        return maxPlayers.orElse(spawnPoints.size());
    }

    /**
     * 获取指定索引的出生点（循环使用）
     * @param playerIndex 玩家在房间中的索引（0-based）
     */
    public SpawnPoint getSpawnPoint(int playerIndex) {
        if (spawnPoints.isEmpty()) {
            throw new IllegalStateException("Room has no spawn points configured");
        }
        return spawnPoints.get(playerIndex % spawnPoints.size());
    }
}
