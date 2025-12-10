package dev.doctor4t.trainmurdermystery.api;

/**
 * Represents the faction/team a role belongs to.
 * Used for win condition determination and end screen grouping.
 */
public enum Faction {
    /**
     * Civilian faction - roles with isInnocent=true.
     * Win when all killers are eliminated or time runs out.
     */
    CIVILIAN,

    /**
     * Killer faction - roles with canUseKiller=true.
     * Win when all civilians are eliminated.
     */
    KILLER,

    /**
     * Neutral faction - roles with both isInnocent=false and canUseKiller=false.
     * Have custom win conditions defined by the role.
     */
    NEUTRAL;

    /**
     * Determines the faction of a role based on its properties.
     * @param role the role to check
     * @return the faction the role belongs to
     */
    public static Faction fromRole(Role role) {
        if (role == null) return CIVILIAN;
        if (role.isInnocent()) return CIVILIAN;
        if (role.canUseKiller()) return KILLER;
        return NEUTRAL;
    }
}
