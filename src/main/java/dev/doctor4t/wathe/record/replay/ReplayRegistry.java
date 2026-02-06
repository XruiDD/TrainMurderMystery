package dev.doctor4t.wathe.record.replay;

import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * 回放事件格式化器注册表
 * 管理事件类型与其格式化器的映射关系
 */
public final class ReplayRegistry {
    private ReplayRegistry() {
    }

    private static final Map<String, ReplayEventFormatter> FORMATTERS = new HashMap<>();
    private static final Map<Identifier, ReplayEventFormatter> SKILL_FORMATTERS = new HashMap<>();

    /**
     * 注册事件格式化器
     *
     * @param eventType 事件类型（如 "death", "shop_purchase" 等）
     * @param formatter 格式化器实现
     */
    public static void registerFormatter(String eventType, ReplayEventFormatter formatter) {
        FORMATTERS.put(eventType, formatter);
    }

    /**
     * 注册技能专属格式化器，优先于默认 skillUse 格式化器
     *
     * @param skillId   技能标识符（如 Identifier.of("noellesroles", "swapper")）
     * @param formatter 格式化器实现
     */
    public static void registerSkillFormatter(Identifier skillId, ReplayEventFormatter formatter) {
        SKILL_FORMATTERS.put(skillId, formatter);
    }

    /**
     * 获取技能专属格式化器
     *
     * @param skillId 技能标识符
     * @return 格式化器，如果未注册则返回 null
     */
    @Nullable
    public static ReplayEventFormatter getSkillFormatter(Identifier skillId) {
        return SKILL_FORMATTERS.get(skillId);
    }

    /**
     * 检查事件类型是否有注册的格式化器
     *
     * @param eventType 事件类型
     * @return 如果已注册则返回 true
     */
    public static boolean isIncluded(String eventType) {
        return FORMATTERS.containsKey(eventType);
    }

    /**
     * 获取事件格式化器
     *
     * @param eventType 事件类型
     * @return 格式化器，如果未注册则返回 null
     */
    @Nullable
    public static ReplayEventFormatter getFormatter(String eventType) {
        return FORMATTERS.get(eventType);
    }
}
