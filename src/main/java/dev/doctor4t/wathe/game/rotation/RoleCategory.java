package dev.doctor4t.wathe.game.rotation;

/** 角色分配的债务统计桶，与四个分配阶段一一对应。纯逻辑，禁止依赖 Minecraft。 */
public enum RoleCategory {
    KILLER,
    VIGILANTE,
    NEUTRAL,
    CIVILIAN
}
