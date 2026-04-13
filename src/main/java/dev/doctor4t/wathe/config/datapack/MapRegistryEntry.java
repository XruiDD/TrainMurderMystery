package dev.doctor4t.wathe.config.datapack;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.doctor4t.wathe.WatheConfig;
import net.minecraft.util.Identifier;

import java.util.Optional;

/**
 * 地图注册表条目
 * 每个条目代表一张可投票的地图，对应一个维度
 */
public record MapRegistryEntry(
    Identifier dimensionId,
    String displayName,
    Optional<String> description,
    MapEnhancementsConfiguration enhancements,
    int minPlayers,
    int maxPlayers
) {
    public static final Codec<MapRegistryEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Identifier.CODEC.fieldOf("dimension").forGetter(MapRegistryEntry::dimensionId),
        Codec.STRING.fieldOf("display_name").forGetter(MapRegistryEntry::displayName),
        Codec.STRING.optionalFieldOf("description").forGetter(MapRegistryEntry::description),
        MapEnhancementsConfiguration.CODEC.optionalFieldOf("enhancements", new MapEnhancementsConfiguration(
            java.util.List.of(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()
        )).forGetter(MapRegistryEntry::enhancements),
        Codec.INT.optionalFieldOf("min_players", 0).forGetter(MapRegistryEntry::minPlayers),
        Codec.INT.optionalFieldOf("max_players", 100).forGetter(MapRegistryEntry::maxPlayers)
    ).apply(instance, MapRegistryEntry::new));

    /**
     * 检查给定人数是否满足此地图的人数限制
     * 当 ignoreMapPlayerLimit 开启时，所有地图都视为符合条件（人数仅作为推荐）
     */
    public boolean isEligible(int playerCount) {
        if (WatheConfig.ignoreMapPlayerLimit) {
            return true;
        }
        return playerCount >= minPlayers && playerCount <= maxPlayers;
    }
}
