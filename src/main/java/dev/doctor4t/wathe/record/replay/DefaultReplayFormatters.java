package dev.doctor4t.wathe.record.replay;

import dev.doctor4t.wathe.record.GameRecordEvent;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
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
        String itemId = data.getString("item");
        int price = data.getInt("price_paid");

        if (actorUuid == null) {
            return null;
        }

        Text playerText = ReplayGenerator.formatPlayerName(actorUuid, currentPlayerInfoCache);

        // 获取物品名称
        String itemName = getItemDisplayName(itemId);

        return Text.translatable("replay.shop_purchase", playerText, itemName, price);
    }

    /**
     * 获取物品显示名称
     */
    private static String getItemDisplayName(String itemId) {
        if (itemId == null || itemId.isEmpty()) {
            return "unknown";
        }
        Identifier id = Identifier.tryParse(itemId);
        if (id != null) {
            // 返回物品翻译键，让 Text.translatable 处理
            return id.getPath();
        }
        return itemId;
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
        UUID actorUuid = data.containsUuid("actor") ? data.getUuid("actor") : null;
        UUID targetUuid = data.containsUuid("target") ? data.getUuid("target") : null;
        String skillId = data.getString("skill");

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
