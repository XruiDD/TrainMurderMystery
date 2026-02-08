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
    private static final Map<Identifier, ReplayEventFormatter> ITEM_USE_FORMATTERS = new HashMap<>();
    private static final Map<Identifier, ReplayEventFormatter> PLATTER_TAKE_FORMATTERS = new HashMap<>();
    private static final Map<Identifier, ReplayEventFormatter> GLOBAL_EVENT_FORMATTERS = new HashMap<>();

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
     * 注册物品使用专属格式化器
     * 仅注册了格式化器的物品使用事件才会出现在回放中，未注册的将被忽略
     *
     * @param itemId    物品标识符（如 Identifier.of("wathe", "poison_vial")）
     * @param formatter 格式化器实现
     */
    public static void registerItemUseFormatter(Identifier itemId, ReplayEventFormatter formatter) {
        ITEM_USE_FORMATTERS.put(itemId, formatter);
    }

    /**
     * 注册餐盘拿取专属格式化器
     * 仅注册了格式化器的物品才会出现在回放中，未注册的将被忽略
     *
     * @param itemId    物品标识符
     * @param formatter 格式化器实现
     */
    public static void registerPlatterTakeFormatter(Identifier itemId, ReplayEventFormatter formatter) {
        PLATTER_TAKE_FORMATTERS.put(itemId, formatter);
    }

    /**
     * 注册全局事件专属格式化器，优先于默认 globalEvent 格式化器
     *
     * @param eventId   全局事件标识符（如 Identifier.of("noellesroles", "jester_moment_start")）
     * @param formatter 格式化器实现
     */
    public static void registerGlobalEventFormatter(Identifier eventId, ReplayEventFormatter formatter) {
        GLOBAL_EVENT_FORMATTERS.put(eventId, formatter);
    }

    @Nullable
    public static ReplayEventFormatter getSkillFormatter(Identifier skillId) {
        return SKILL_FORMATTERS.get(skillId);
    }

    @Nullable
    public static ReplayEventFormatter getItemUseFormatter(Identifier itemId) {
        return ITEM_USE_FORMATTERS.get(itemId);
    }

    @Nullable
    public static ReplayEventFormatter getPlatterTakeFormatter(Identifier itemId) {
        return PLATTER_TAKE_FORMATTERS.get(itemId);
    }

    @Nullable
    public static ReplayEventFormatter getGlobalEventFormatter(Identifier eventId) {
        return GLOBAL_EVENT_FORMATTERS.get(eventId);
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
