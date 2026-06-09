package dev.doctor4t.wathe.api.event;

import net.fabricmc.fabric.api.event.Event;

import static net.fabricmc.fabric.api.event.EventFactory.createArrayBacked;

/**
 * 低心情"幻觉"视觉效果的客户端抑制事件。
 * <p>
 * 当任一监听器返回 {@code true} 时，{@link dev.doctor4t.wathe.cca.PlayerMoodComponent}
 * 客户端的低心情幻觉物品（psychosis items）将停止生成并清空，从而让附属模组
 * （如 NoellesRoles 的小丑时刻）可以无冲突地接管玩家外观。
 * <p>
 * 该抑制由"状态"驱动而非"心情值"驱动：附属模组应在其特殊时刻激活期间返回 {@code true}，
 * 这样即使玩家在此期间把心情值恢复到阈值以上，抑制依然成立。
 */
public final class MoodVisualEvents {

    private MoodVisualEvents() {
    }

    /**
     * 是否抑制低心情幻觉物品。任一监听器返回 {@code true} 即抑制。
     */
    public static final Event<SuppressPsychosis> SUPPRESS_PSYCHOSIS = createArrayBacked(SuppressPsychosis.class, listeners -> () -> {
        for (SuppressPsychosis listener : listeners) {
            if (listener.shouldSuppress()) {
                return true;
            }
        }
        return false;
    });

    /**
     * @return 当前是否应抑制低心情幻觉物品
     */
    public static boolean isPsychosisSuppressed() {
        return SUPPRESS_PSYCHOSIS.invoker().shouldSuppress();
    }

    @FunctionalInterface
    public interface SuppressPsychosis {
        /**
         * @return 是否抑制低心情幻觉物品
         */
        boolean shouldSuppress();
    }
}
