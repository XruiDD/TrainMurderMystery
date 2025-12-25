package dev.doctor4t.wathe.api.event;

import net.fabricmc.fabric.api.event.Event;
import net.minecraft.entity.player.PlayerEntity;
import org.jetbrains.annotations.Nullable;

import static net.fabricmc.fabric.api.event.EventFactory.createArrayBacked;

/**
 * 控制当杀手看向某个玩家时，是否应该显示"cohort"（同伙）提示。
 * <p>
 * 默认情况下，只有真正的杀手之间才会显示同伙提示。
 * 附属模组可以注册监听器来自定义此行为（如让某些伪装角色显示为同伙，或隐藏真杀手）。
 * <p>
 * 事件处理逻辑：
 * <ol>
 *   <li>按优先级排序，返回第一个非 null 的 {@link CohortResult}</li>
 *   <li>如果所有监听器都返回 null，则使用默认逻辑</li>
 *   <li>返回 {@link CohortResult#hide()} 可以阻止显示同伙提示</li>
 * </ol>
 * <p>
 * 使用示例：
 * <pre>{@code
 * // 注册监听器 - 让卧底角色显示为杀手同伙
 * ShouldShowCohort.EVENT.register((viewer, target) -> {
 *     if (isUndercover(target)) {
 *         return CohortResult.show();
 *     }
 *     return null; // 不处理，使用默认逻辑
 * });
 *
 * // 隐藏特定角色的同伙提示
 * ShouldShowCohort.EVENT.register((viewer, target) -> {
 *     if (shouldHideCohort(target)) {
 *         return CohortResult.hide();
 *     }
 *     return null;
 * });
 * }</pre>
 */
public interface ShouldShowCohort {

    Event<ShouldShowCohort> EVENT = createArrayBacked(ShouldShowCohort.class, listeners -> (viewer, target) -> {
        CohortResult bestResult = null;
        for (ShouldShowCohort listener : listeners) {
            CohortResult result = listener.getCohortResult(viewer, target);
            if (result != null) {
                if (bestResult == null || result.priority() > bestResult.priority()) {
                    bestResult = result;
                }
            }
        }
        return bestResult;
    });

    /**
     * 获取是否应该显示同伙提示的结果
     *
     * @param viewer 查看的杀手玩家
     * @param target 被查看的目标玩家
     * @return 同伙显示结果，或 null 表示不处理（使用默认逻辑）
     */
    @Nullable
    CohortResult getCohortResult(PlayerEntity viewer, PlayerEntity target);

    /**
     * 同伙显示结果
     *
     * @param shouldShow 是否显示同伙提示
     * @param priority   优先级，数值越高越优先
     */
    record CohortResult(boolean shouldShow, int priority) {
        /** 默认优先级 */
        public static final int PRIORITY_DEFAULT = 0;
        /** 高优先级（覆盖默认逻辑） */
        public static final int PRIORITY_HIGH = 100;
        /** 低优先级（作为后备） */
        public static final int PRIORITY_LOW = -100;

        /**
         * 创建显示同伙提示的结果（标准用法）
         *
         * @return 同伙显示结果
         */
        public static CohortResult show() {
            return new CohortResult(true, PRIORITY_DEFAULT);
        }

        /**
         * 创建显示同伙提示的结果，指定优先级
         *
         * @param priority 优先级（数值越高越优先）
         * @return 同伙显示结果
         */
        public static CohortResult show(int priority) {
            return new CohortResult(true, priority);
        }

        /**
         * 创建隐藏同伙提示的结果
         *
         * @return 同伙隐藏结果
         */
        public static CohortResult hide() {
            return new CohortResult(false, PRIORITY_HIGH);
        }

        /**
         * 创建隐藏同伙提示的结果，指定优先级
         *
         * @param priority 优先级
         * @return 同伙隐藏结果
         */
        public static CohortResult hide(int priority) {
            return new CohortResult(false, priority);
        }
    }
}
