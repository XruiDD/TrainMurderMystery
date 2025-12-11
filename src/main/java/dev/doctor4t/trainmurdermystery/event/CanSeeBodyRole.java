package dev.doctor4t.trainmurdermystery.event;

import net.fabricmc.fabric.api.event.Event;
import net.minecraft.entity.Entity;

import static net.fabricmc.fabric.api.event.EventFactory.createArrayBacked;

/**
 * 控制玩家是否能看到尸体(PlayerBodyEntity)的角色信息。
 * <p>
 * 默认情况下，只有旁观者/创造模式玩家可以看到尸体的角色。
 * 附属模组可以注册监听器来扩展此功能（如验尸官、秃鹫等角色）。
 * <p>
 * 使用"或"逻辑：任何监听器返回 true 即表示该玩家可以看到尸体角色。
 */
public interface CanSeeBodyRole {

    Event<CanSeeBodyRole> EVENT = createArrayBacked(CanSeeBodyRole.class, listeners -> player -> {
        for (CanSeeBodyRole listener : listeners) {
            if (listener.canSee(player)) {
                return true;
            }
        }
        return false;
    });

    /**
     * 判断玩家是否能看到尸体的角色信息
     *
     * @param player 要检查的玩家实体
     * @return 如果玩家可以看到尸体角色则返回 true
     */
    boolean canSee(Entity player);
}
