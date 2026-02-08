package dev.doctor4t.wathe.record.replay;

import dev.doctor4t.wathe.record.GameRecordEvent;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.nbt.NbtCompound;
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
     * 构建下毒事件的翻译键
     */
    private static String buildPoisonedTranslationKey(String source, boolean hasPoisoner) {
        // source 格式: "wathe:food" 或 "othermod:custom_poison"
        String suffix = hasPoisoner ? ".by" : "";

        if (source != null && !source.isEmpty()) {
            Identifier id = Identifier.tryParse(source);
            if (id != null) {
                // 转换为: replay.poisoned.wathe.food.by
                return "replay.poisoned." + id.getNamespace() + "." + id.getPath() + suffix;
            }
        }

        return "replay.poisoned" + suffix;
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
        Text itemName = ReplayGenerator.formatItemName(data, world);

        return Text.translatable("replay.shop_purchase", playerText, itemName, price);
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
        Text itemName = ReplayGenerator.formatItemName(data, world);
        int count = data.getInt("count");

        if (count > 1) {
            return Text.translatable("replay.item_pickup.multiple", playerText, itemName, count);
        }
        return Text.translatable("replay.item_pickup", playerText, itemName);
    }

    /**
     * 格式化物品使用事件（分发给按物品 ID 注册的子格式化器）
     * 未注册格式化器的物品使用事件将被忽略
     */
    @Nullable
    public static Text formatItemUse(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        NbtCompound data = event.data();
        String itemIdStr = data.getString("item");
        if (itemIdStr == null || itemIdStr.isEmpty()) {
            return null;
        }
        Identifier itemId = Identifier.tryParse(itemIdStr);
        if (itemId == null) {
            return null;
        }
        ReplayEventFormatter formatter = ReplayRegistry.getItemUseFormatter(itemId);
        if (formatter == null) {
            return null; // 未注册的物品使用事件，忽略
        }
        return formatter.format(event, match, world);
    }

    /**
     * 格式化餐盘拿取事件（分发给按物品 ID 注册的子格式化器）
     * 未注册格式化器的餐盘拿取事件将被忽略
     */
    @Nullable
    public static Text formatPlatterTake(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        NbtCompound data = event.data();
        String itemIdStr = data.getString("item");
        if (itemIdStr == null || itemIdStr.isEmpty()) {
            return null;
        }
        Identifier itemId = Identifier.tryParse(itemIdStr);
        if (itemId == null) {
            return null;
        }
        ReplayEventFormatter formatter = ReplayRegistry.getPlatterTakeFormatter(itemId);
        if (formatter == null) {
            return null; // 未注册的餐盘拿取事件，忽略
        }
        return formatter.format(event, match, world);
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
        String source = data.contains("source") ? data.getString("source") : null;
        String translationKey = buildPoisonedTranslationKey(source, poisonerUuid != null);

        if (poisonerUuid != null) {
            Text poisonerText = ReplayGenerator.formatPlayerName(poisonerUuid, currentPlayerInfoCache);
            return Text.translatable(translationKey, victimText, poisonerText);
        } else {
            return Text.translatable(translationKey, victimText);
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
