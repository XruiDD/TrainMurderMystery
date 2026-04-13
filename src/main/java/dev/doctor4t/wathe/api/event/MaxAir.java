package dev.doctor4t.wathe.api.event;

import net.fabricmc.fabric.api.event.Event;
import net.minecraft.entity.player.PlayerEntity;

import static net.fabricmc.fabric.api.event.EventFactory.createArrayBacked;

/**
 * 修改玩家最大氧气值的事件。
 * 在 getMaxAir() 返回后触发，监听器可以修改返回值。
 * 多个监听器依次处理，后者接收前者的结果。
 */
public final class MaxAir {

    private MaxAir() {
    }

    public static final Event<Modifier> EVENT = createArrayBacked(Modifier.class, listeners -> (player, maxAir) -> {
        int result = maxAir;
        for (Modifier listener : listeners) {
            result = listener.modifyMaxAir(player, result);
        }
        return result;
    });

    @FunctionalInterface
    public interface Modifier {
        /**
         * @param player 玩家
         * @param maxAir 当前最大氧气值（可能已被前一个监听器修改）
         * @return 修改后的最大氧气值
         */
        int modifyMaxAir(PlayerEntity player, int maxAir);
    }
}
