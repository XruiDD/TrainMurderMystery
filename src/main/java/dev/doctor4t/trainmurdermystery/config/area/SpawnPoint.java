package dev.doctor4t.trainmurdermystery.config.area;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.math.Vec3d;

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
