package dev.doctor4t.wathe.api.event;

import dev.doctor4t.wathe.entity.PlayerBodyEntity;
import net.fabricmc.fabric.api.event.Event;
import net.minecraft.entity.Entity;

import static net.fabricmc.fabric.api.event.EventFactory.createArrayBacked;

/**
 * 控制玩家是否能用准星选中尸体(PlayerBodyEntity)。
 * <p>
 * 默认情况下，所有尸体都可以被选中。
 * 附属模组可以注册监听器来阻止选中特定尸体（如清道夫隐藏的尸体）。
 * <p>
 * 使用"与"逻辑：任何监听器返回 false 即表示该尸体不可被选中。
 */
public interface CanTargetBody {

    Event<CanTargetBody> EVENT = createArrayBacked(CanTargetBody.class, listeners -> (player, body) -> {
        for (CanTargetBody listener : listeners) {
            if (!listener.canTarget(player, body)) {
                return false;
            }
        }
        return true;
    });

    /**
     * 判断玩家是否能用准星选中该尸体
     *
     * @param player 当前玩家
     * @param body   目标尸体实体
     * @return 如果允许选中返回 true，阻止选中返回 false
     */
    boolean canTarget(Entity player, PlayerBodyEntity body);
}
