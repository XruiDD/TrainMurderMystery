package dev.doctor4t.wathe.util;

/**
 * 记录玩家最近一次"由右键交互（开门、用物品、右键实体等）触发的挥手"所在 tick。
 *
 * <p>原版交互成功时服务端会调用 {@code swingHand(hand, true)}（{@code fromServerPlayer=true}），
 * 而左键攻击/空挥走 {@code swingHand(hand)} → {@code swingHand(hand, false)}，永远不会传 true。
 * 据此可以区分左右键触发的挥手，让球棒的攻击冷却重置只跳过右键交互、保留左键。
 */
public interface InteractionSwingTracker {
    long wathe$lastInteractionSwingTick();
}
