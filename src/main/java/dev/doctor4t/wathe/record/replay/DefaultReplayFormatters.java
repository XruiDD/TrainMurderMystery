package dev.doctor4t.wathe.record.replay;

import dev.doctor4t.wathe.record.GameRecordEvent;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.item.Item;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;

/**
 * 默认回放事件格式化器
 * 提供基础事件类型的格式化实现
 */
public final class DefaultReplayFormatters {
    private DefaultReplayFormatters() {
    }

    // 缓存，用于存储当前回放的玩家信息
    private static Map<UUID, ReplayGenerator.PlayerInfo> currentPlayerInfoCache;

    /**
     * 设置当前玩家信息缓存（在生成回放前调用）
     */
    public static void setPlayerInfoCache(Map<UUID, ReplayGenerator.PlayerInfo> cache) {
        currentPlayerInfoCache = cache;
    }

    /**
     * 格式化死亡事件
     */
    @Nullable
    public static Text formatDeath(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        if (currentPlayerInfoCache == null) {
            currentPlayerInfoCache = ReplayGenerator.getPlayerInfoCache(match);
        }

        NbtCompound data = event.data();
        UUID victimUuid = data.containsUuid("target") ? data.getUuid("target") : null;
        UUID killerUuid = data.containsUuid("actor") ? data.getUuid("actor") : null;
        String deathReason = data.getString("death_reason");

        if (victimUuid == null) {
            return null;
        }

        Text victimText = ReplayGenerator.formatPlayerName(victimUuid, currentPlayerInfoCache);
        Text killerText = killerUuid != null ? ReplayGenerator.formatPlayerName(killerUuid, currentPlayerInfoCache) : null;

        // 构建翻译键
        String translationKey = buildDeathTranslationKey(deathReason, killerUuid != null);

        if (killerText != null) {
            return Text.translatable(translationKey, victimText, killerText);
        } else {
            return Text.translatable(translationKey, victimText);
        }
    }

    /**
     * 构建死亡事件的翻译键
     */
    private static String buildDeathTranslationKey(String deathReason, boolean hasKiller) {
        // deathReason 格式: "wathe:knife_stab" 或 "noellesroles:assassinated"
        String suffix = hasKiller ? ".killed" : ".died";

        if (deathReason != null && !deathReason.isEmpty()) {
            Identifier id = Identifier.tryParse(deathReason);
            if (id != null) {
                // 转换为: replay.death.wathe.knife_stab.killed
                return "replay.death." + id.getNamespace() + "." + id.getPath() + suffix;
            }
        }

        return "replay.death.unknown" + suffix;
    }

    /**
     * 格式化商店购买事件
     */
    @Nullable
    public static Text formatShopPurchase(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        if (currentPlayerInfoCache == null) {
            currentPlayerInfoCache = ReplayGenerator.getPlayerInfoCache(match);
        }

        NbtCompound data = event.data();
        UUID actorUuid = data.containsUuid("actor") ? data.getUuid("actor") : null;
        int price = data.getInt("price_paid");

        if (actorUuid == null) {
            return null;
        }

        Text playerText = ReplayGenerator.formatPlayerName(actorUuid, currentPlayerInfoCache);

        // 获取物品名称（支持自定义名称和翻译）
        Text itemName = getItemDisplayText(data, world);

        return Text.translatable("replay.shop_purchase", playerText, itemName, price);
    }

    /**
     * 获取物品显示名称（使用序列化的 Text，让客户端根据语言设置解析）
     */
    private static Text getItemDisplayText(NbtCompound data, ServerWorld world) {
        // 优先使用存储的物品名称（Text.translatable 序列化后的 JSON）
        if (data.contains("item_name")) {
            String nameJson = data.getString("item_name");
            if (nameJson != null && !nameJson.isEmpty()) {
                Text name = Text.Serialization.fromJson(nameJson, world.getRegistryManager());
                if (name != null) {
                    return name;
                }
            }
        }

        // 兼容旧记录：使用物品翻译键
        String itemId = data.getString("item");
        if (itemId != null && !itemId.isEmpty()) {
            Identifier id = Identifier.tryParse(itemId);
            if (id != null) {
                Item item = Registries.ITEM.get(id);
                if (item != null) {
                    return Text.translatable(item.getTranslationKey());
                }
            }
        }
        return Text.literal("unknown");
    }

    /**
     * 格式化物品拾取事件
     */
    @Nullable
    public static Text formatItemPickup(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        if (currentPlayerInfoCache == null) {
            currentPlayerInfoCache = ReplayGenerator.getPlayerInfoCache(match);
        }

        NbtCompound data = event.data();
        UUID actorUuid = data.containsUuid("actor") ? data.getUuid("actor") : null;

        if (actorUuid == null) {
            return null;
        }

        Text playerText = ReplayGenerator.formatPlayerName(actorUuid, currentPlayerInfoCache);
        Text itemName = getItemDisplayText(data, world);
        int count = data.getInt("count");

        if (count > 1) {
            return Text.translatable("replay.item_pickup.multiple", playerText, itemName, count);
        }
        return Text.translatable("replay.item_pickup", playerText, itemName);
    }

    /**
     * 格式化下毒事件
     */
    @Nullable
    public static Text formatPoisoned(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        if (currentPlayerInfoCache == null) {
            currentPlayerInfoCache = ReplayGenerator.getPlayerInfoCache(match);
        }

        NbtCompound data = event.data();
        UUID victimUuid = data.containsUuid("target") ? data.getUuid("target") : null;
        UUID poisonerUuid = data.containsUuid("actor") ? data.getUuid("actor") : null;

        if (victimUuid == null) {
            return null;
        }

        Text victimText = ReplayGenerator.formatPlayerName(victimUuid, currentPlayerInfoCache);

        if (poisonerUuid != null) {
            Text poisonerText = ReplayGenerator.formatPlayerName(poisonerUuid, currentPlayerInfoCache);
            return Text.translatable("replay.poisoned.by", victimText, poisonerText);
        } else {
            return Text.translatable("replay.poisoned", victimText);
        }
    }

    /**
     * 格式化技能使用事件
     */
    @Nullable
    public static Text formatSkillUse(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        if (currentPlayerInfoCache == null) {
            currentPlayerInfoCache = ReplayGenerator.getPlayerInfoCache(match);
        }

        NbtCompound data = event.data();
        String skillId = data.getString("skill");

        // 优先查找技能专属格式化器
        if (skillId != null && !skillId.isEmpty()) {
            Identifier id = Identifier.tryParse(skillId);
            if (id != null) {
                ReplayEventFormatter skillFormatter = ReplayRegistry.getSkillFormatter(id);
                if (skillFormatter != null) {
                    return skillFormatter.format(event, match, world);
                }
            }
        }

        UUID actorUuid = data.containsUuid("actor") ? data.getUuid("actor") : null;
        UUID targetUuid = data.containsUuid("target") ? data.getUuid("target") : null;

        if (actorUuid == null) {
            return null;
        }

        Text actorText = ReplayGenerator.formatPlayerName(actorUuid, currentPlayerInfoCache);

        // 构建技能翻译键
        String translationKey = buildSkillTranslationKey(skillId, targetUuid != null);

        if (targetUuid != null) {
            Text targetText = ReplayGenerator.formatPlayerName(targetUuid, currentPlayerInfoCache);
            return Text.translatable(translationKey, actorText, targetText);
        } else {
            return Text.translatable(translationKey, actorText);
        }
    }

    /**
     * 构建技能使用的翻译键
     */
    private static String buildSkillTranslationKey(String skillId, boolean hasTarget) {
        String suffix = hasTarget ? ".target" : "";

        if (skillId != null && !skillId.isEmpty()) {
            Identifier id = Identifier.tryParse(skillId);
            if (id != null) {
                // 转换为: replay.skill.noellesroles.morphling.target
                return "replay.skill." + id.getNamespace() + "." + id.getPath() + suffix;
            }
        }

        return "replay.skill.unknown" + suffix;
    }

    /**
     * 格式化全局事件
     */
    @Nullable
    public static Text formatGlobalEvent(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        if (currentPlayerInfoCache == null) {
            currentPlayerInfoCache = ReplayGenerator.getPlayerInfoCache(match);
        }

        NbtCompound data = event.data();
        UUID actorUuid = data.containsUuid("actor") ? data.getUuid("actor") : null;
        String eventId = data.getString("event");

        // 构建全局事件翻译键
        String translationKey = buildGlobalEventTranslationKey(eventId);

        if (actorUuid != null) {
            Text actorText = ReplayGenerator.formatPlayerName(actorUuid, currentPlayerInfoCache);
            return Text.translatable(translationKey, actorText);
        } else {
            return Text.translatable(translationKey);
        }
    }

    /**
     * 构建全局事件的翻译键
     */
    private static String buildGlobalEventTranslationKey(String eventId) {
        if (eventId != null && !eventId.isEmpty()) {
            Identifier id = Identifier.tryParse(eventId);
            if (id != null) {
                // 转换为: replay.global.noellesroles.jester_stasis
                return "replay.global." + id.getNamespace() + "." + id.getPath();
            }
        }

        return "replay.global.unknown";
    }
}
