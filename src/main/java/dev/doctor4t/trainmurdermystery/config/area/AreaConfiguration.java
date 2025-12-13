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
    List<RoomConfig> rooms
) {

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
        RoomConfig.CODEC.listOf().fieldOf("rooms").forGetter(AreaConfiguration::rooms)
    ).apply(instance, AreaConfiguration::new));

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
