package dev.doctor4t.wathe.api.event;

import dev.doctor4t.wathe.util.ShopUtils;
import net.fabricmc.fabric.api.event.Event;
import net.minecraft.entity.player.PlayerEntity;
import org.jetbrains.annotations.Nullable;

import static net.fabricmc.fabric.api.event.EventFactory.createArrayBacked;

/**
 * 控制玩家是否能看到金币/余额显示。
 * <p>
 * 此事件优先级比 {@link dev.doctor4t.wathe.util.ShopUtils#canAccessShop} 更高，
 * 允许附属模组在不改变商店访问权限的情况下控制金币显示。
 * <p>
 * 使用场景示例：
 * <ul>
 *   <li>某些角色可以看到金币但无法购买商品</li>
 *   <li>临时隐藏金币显示（如某些游戏阶段）</li>
 * </ul>
 * <p>
 * 事件阶段（优先级从高到低）：
 * <ul>
 *   <li>{@link Event#DEFAULT_PHASE} - 默认阶段，用于常规检查</li>
 * </ul>
 */
public interface CanSeeMoney {


    /**
     * 事件回调结果
     */
    enum Result {
        /**
         * 允许玩家看到金币
         */
        ALLOW,
        /**
         * 拒绝玩家看到金币
         */
        DENY
    }

    /**
     * 金币显示可见性事件。
     * <p>
     * 监听器返回值含义：
     * <ul>
     *   <li>{@link Result#ALLOW} - 立即允许显示，不再调用后续监听器</li>
     *   <li>{@link Result#DENY} - 立即拒绝显示，不再调用后续监听器</li>
     *   <li>NULL - 继续调用下一个监听器</li>
     * </ul>
     * <p>
     * 如果所有监听器都返回 PASS，则默认为不显示（false）。
     */
    Event<CanSeeMoney> EVENT = createArrayBacked(CanSeeMoney.class, listeners -> player -> {
        for (CanSeeMoney listener : listeners) {
            Result result = listener.canSee(player);
            if (result == Result.ALLOW) {
                return Result.ALLOW;
            } else if (result == Result.DENY) {
                return Result.DENY;
            }
        }
        return ShopUtils.canAccessShop(player) ? Result.ALLOW : Result.DENY;
    });

    /**
     * 判断玩家是否能看到金币显示
     *
     * @param player 要检查的玩家实体
     * @return 结果：ALLOW 允许、DENY 拒绝、PASS 不做判断
     */
    @Nullable
    Result canSee(PlayerEntity player);
}
