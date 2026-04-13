package dev.doctor4t.wathe.api.event;

import net.fabricmc.fabric.api.event.Event;
import net.minecraft.server.network.ServerPlayerEntity;

import static net.fabricmc.fabric.api.event.EventFactory.createArrayBacked;

/**
 * 疯魔模式生命周期事件。
 * <p>
 * 当玩家进入或退出疯魔模式时触发，允许附属模组做出响应。
 */
public final class PsychoModeEvents {

    private PsychoModeEvents() {
    }

    /**
     * 玩家进入疯魔模式后触发。
     */
    public static final Event<OnPsychoStart> ON_PSYCHO_START = createArrayBacked(OnPsychoStart.class, listeners -> (player, trackActive) -> {
        for (OnPsychoStart listener : listeners) {
            listener.onPsychoStart(player, trackActive);
        }
    });

    /**
     * 玩家退出疯魔模式后触发。
     */
    public static final Event<OnPsychoEnd> ON_PSYCHO_END = createArrayBacked(OnPsychoEnd.class, listeners -> (player, trackActive) -> {
        for (OnPsychoEnd listener : listeners) {
            listener.onPsychoEnd(player, trackActive);
        }
    });

    @FunctionalInterface
    public interface OnPsychoStart {
        /**
         * @param player      进入疯魔模式的玩家
         * @param trackActive 是否为公开疯魔（false = 静语者等静默疯魔）
         */
        void onPsychoStart(ServerPlayerEntity player, boolean trackActive);
    }

    @FunctionalInterface
    public interface OnPsychoEnd {
        /**
         * @param player      退出疯魔模式的玩家
         * @param trackActive 是否为公开疯魔
         */
        void onPsychoEnd(ServerPlayerEntity player, boolean trackActive);
    }
}
