package dev.doctor4t.wathe.api.event;

import net.fabricmc.fabric.api.event.Event;
import net.minecraft.server.network.ServerPlayerEntity;

import static net.fabricmc.fabric.api.event.EventFactory.createArrayBacked;

/**
 * 疯魔模式生命周期事件。
 * <p>
 * 当玩家进入或退出疯魔模式时触发，允许附属模组做出响应。
 * 所有 {@link PsychoType} 均会派发（含 {@link PsychoType#SILENT}），监听者按需过滤。
 */
public final class PsychoModeEvents {

    private PsychoModeEvents() {
    }

    /**
     * 玩家进入疯魔模式后触发。
     */
    public static final Event<OnPsychoStart> ON_PSYCHO_START = createArrayBacked(OnPsychoStart.class, listeners -> (player, type) -> {
        for (OnPsychoStart listener : listeners) {
            listener.onPsychoStart(player, type);
        }
    });

    /**
     * 玩家退出疯魔模式后触发。
     */
    public static final Event<OnPsychoEnd> ON_PSYCHO_END = createArrayBacked(OnPsychoEnd.class, listeners -> (player, type) -> {
        for (OnPsychoEnd listener : listeners) {
            listener.onPsychoEnd(player, type);
        }
    });

    @FunctionalInterface
    public interface OnPsychoStart {
        /**
         * @param player 进入疯魔模式的玩家
         * @param type   疯魔类型
         */
        void onPsychoStart(ServerPlayerEntity player, PsychoType type);
    }

    @FunctionalInterface
    public interface OnPsychoEnd {
        /**
         * @param player 退出疯魔模式的玩家
         * @param type   疯魔类型
         */
        void onPsychoEnd(ServerPlayerEntity player, PsychoType type);
    }
}
