package dev.doctor4t.wathe.game.rotation;

/**
 * 一名玩家在一局里的历史条目。
 * killer/vigilante/neutralShare = 当局对应阵营的应得份额(目标人数/总人数)。
 * category = 本人当局实际落入的桶；roleId = 本人当局拿到的具体角色 id（用于避重）。
 * 纯数据，禁止依赖 Minecraft。
 */
public record GameEntry(
        double killerShare,
        double vigilanteShare,
        double neutralShare,
        RoleCategory category,
        String roleId
) {
    public double shareFor(RoleCategory c) {
        return switch (c) {
            case KILLER -> killerShare;
            case VIGILANTE -> vigilanteShare;
            case NEUTRAL -> neutralShare;
            case CIVILIAN -> 0.0;
        };
    }
}
