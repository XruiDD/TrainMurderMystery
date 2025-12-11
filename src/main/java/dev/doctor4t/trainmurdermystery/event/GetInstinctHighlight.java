package dev.doctor4t.trainmurdermystery.event;

import net.fabricmc.fabric.api.event.Event;
import net.minecraft.entity.Entity;

import static net.fabricmc.fabric.api.event.EventFactory.createArrayBacked;

/**
 * 允许附属模组为特定实体添加自定义的本能高亮颜色。
 * <p>
 * 当玩家使用本能技能时，此事件在默认高亮逻辑之前触发。
 * 附属模组可以为特定角色或状态的实体返回自定义颜色。
 * <p>
 * 返回第一个非 -1 的结果，如果所有监听器都返回 -1，则使用默认逻辑。
 */
public interface GetInstinctHighlight {

    Event<GetInstinctHighlight> EVENT = createArrayBacked(GetInstinctHighlight.class, listeners -> target -> {
        for (GetInstinctHighlight listener : listeners) {
            int color = listener.getHighlight(target);
            if (color != -1) {
                return color;
            }
        }
        return -1;
    });

    /**
     * 获取实体的本能高亮颜色
     *
     * @param target 目标实体
     * @return RGB 颜色值，或 -1 表示不处理（使用默认逻辑）
     */
    int getHighlight(Entity target);
}
