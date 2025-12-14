package dev.doctor4t.trainmurdermystery.event;

import net.fabricmc.fabric.api.event.Event;
import net.minecraft.entity.Entity;
import org.jetbrains.annotations.Nullable;

import static net.fabricmc.fabric.api.event.EventFactory.createArrayBacked;

/**
 * 允许附属模组为特定实体添加自定义的本能高亮效果。
 * <p>
 * 当检查实体高亮时，此事件在默认高亮逻辑之前触发。
 * 附属模组可以为特定角色或状态的实体返回自定义高亮结果。
 * <p>
 * 事件处理逻辑：
 * <ol>
 *   <li>按优先级排序，返回第一个非 null 的 {@link HighlightResult}</li>
 *   <li>如果所有监听器都返回 null，则使用默认逻辑</li>
 *   <li>返回 {@link HighlightResult#skip()} 可以阻止该实体被高亮</li>
 * </ol>
 * <p>
 * 使用示例：
 * <pre>{@code
 * // 注册监听器 - 为特定角色添加始终可见的高亮
 * GetInstinctHighlight.EVENT.register(target -> {
 *     if (target instanceof PlayerEntity player) {
 *         if (isMyCustomRole(player)) {
 *             // 始终显示紫色高亮，不需要按键
 *             return HighlightResult.always(0x9932CC);
 *         }
 *     }
 *     return null; // 不处理，使用默认逻辑
 * });
 *
 * // 需要按键触发的高亮
 * GetInstinctHighlight.EVENT.register(target -> {
 *     if (shouldHighlight(target)) {
 *         return HighlightResult.withKeybind(0xFF0000);
 *     }
 *     return null;
 * });
 *
 * // 阻止特定实体被高亮
 * GetInstinctHighlight.EVENT.register(target -> {
 *     if (shouldHide(target)) {
 *         return HighlightResult.skip();
 *     }
 *     return null;
 * });
 * }</pre>
 */
public interface GetInstinctHighlight {

    Event<GetInstinctHighlight> EVENT = createArrayBacked(GetInstinctHighlight.class, listeners -> target -> {
        HighlightResult bestResult = null;
        for (GetInstinctHighlight listener : listeners) {
            HighlightResult result = listener.getHighlight(target);
            if (result != null) {
                if (bestResult == null || result.priority() > bestResult.priority()) {
                    bestResult = result;
                }
            }
        }
        return bestResult;
    });

    /**
     * 获取实体的本能高亮效果
     *
     * @param target 目标实体
     * @return 高亮结果，或 null 表示不处理（使用默认逻辑）
     */
    @Nullable
    HighlightResult getHighlight(Entity target);

    /**
     * 高亮结果，包含颜色和额外配置。
     *
     * @param color          RGB 颜色值，-1 表示跳过高亮
     * @param requiresKeybind 是否需要按下本能键才显示
     * @param priority       优先级，数值越高越优先
     */
    record HighlightResult(int color, boolean requiresKeybind, int priority) {
        /** 默认优先级 */
        public static final int PRIORITY_DEFAULT = 0;
        /** 高优先级（覆盖默认逻辑） */
        public static final int PRIORITY_HIGH = 100;
        /** 低优先级（作为后备） */
        public static final int PRIORITY_LOW = -100;

        /**
         * 创建需要按键触发的高亮（标准用法）
         *
         * @param color RGB 颜色值
         * @return 高亮结果
         */
        public static HighlightResult withKeybind(int color) {
            return new HighlightResult(color, true, PRIORITY_DEFAULT);
        }

        /**
         * 创建需要按键触发的高亮，指定优先级
         *
         * @param color    RGB 颜色值
         * @param priority 优先级（数值越高越优先）
         * @return 高亮结果
         */
        public static HighlightResult withKeybind(int color, int priority) {
            return new HighlightResult(color, true, priority);
        }

        /**
         * 创建始终显示的高亮（不需要按键）
         *
         * @param color RGB 颜色值
         * @return 高亮结果
         */
        public static HighlightResult always(int color) {
            return new HighlightResult(color, false, PRIORITY_DEFAULT);
        }

        /**
         * 创建始终显示的高亮，指定优先级
         *
         * @param color    RGB 颜色值
         * @param priority 优先级
         * @return 高亮结果
         */
        public static HighlightResult always(int color, int priority) {
            return new HighlightResult(color, false, priority);
        }

        /**
         * 阻止该实体被高亮（显式跳过）
         *
         * @return 跳过高亮的结果
         */
        public static HighlightResult skip() {
            return new HighlightResult(-1, false, PRIORITY_HIGH);
        }

        /**
         * 检查此结果是否为跳过标记
         *
         * @return true 如果应跳过高亮
         */
        public boolean isSkip() {
            return color == -1;
        }
    }
}
