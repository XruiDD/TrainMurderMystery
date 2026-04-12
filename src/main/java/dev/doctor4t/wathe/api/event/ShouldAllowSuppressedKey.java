package dev.doctor4t.wathe.api.event;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.event.Event;
import net.minecraft.client.option.KeyBinding;

import static net.fabricmc.fabric.api.event.EventFactory.createArrayBacked;

/**
 * 客户端事件：当 KeyBindingMixin 即将屏蔽某个按键时触发。
 * 任何监听器返回 true 即表示"放行该按键"，不再屏蔽。
 * 所有监听器均返回 false 则照常屏蔽。
 * <p>
 * 用例：灵界使者灵魂状态时需要放行跳跃键以便飞行。
 */
@Environment(EnvType.CLIENT)
public interface ShouldAllowSuppressedKey {

    Event<ShouldAllowSuppressedKey> EVENT = createArrayBacked(ShouldAllowSuppressedKey.class, listeners -> (keyBinding) -> {
        for (ShouldAllowSuppressedKey listener : listeners) {
            if (listener.shouldAllow(keyBinding)) {
                return true;
            }
        }
        return false;
    });

    /**
     * @param keyBinding 即将被屏蔽的按键
     * @return true 表示放行（不屏蔽），false 表示继续屏蔽
     */
    boolean shouldAllow(KeyBinding keyBinding);
}
