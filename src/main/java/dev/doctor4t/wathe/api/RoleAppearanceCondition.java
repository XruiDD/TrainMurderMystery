package dev.doctor4t.wathe.api;

/**
 * Functional interface for determining whether a role should appear in the current game.
 * Addon mods can implement this interface to define custom appearance conditions for roles.
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * // Role only appears when player count >= 8
 * role.setAppearanceCondition(ctx -> ctx.getTotalPlayerCount() >= 8);
 *
 * // Role only appears when there are multiple killers
 * role.setAppearanceCondition(ctx -> ctx.getTargetKillerCount() > 1);
 *
 * // Role only appears when another specific role is assigned
 * role.setAppearanceCondition(ctx -> ctx.isRoleAssigned(OtherRoles.SOME_ROLE));
 *
 * // Role only appears when player count is between 6 and 12
 * role.setAppearanceCondition(ctx -> ctx.getTotalPlayerCount() >= 6 && ctx.getTotalPlayerCount() <= 12);
 * }</pre>
 */
@FunctionalInterface
public interface RoleAppearanceCondition {
    /**
     * The default condition that always allows the role to appear.
     */
    RoleAppearanceCondition ALWAYS = context -> true;

    /**
     * Determines whether the role should be available for selection in the current game.
     *
     * @param context the role selection context containing game state information
     * @return true if the role should be available, false otherwise
     */
    boolean shouldAppear(RoleSelectionContext context);

    /**
     * Returns a condition that is the logical AND of this condition and another.
     *
     * @param other the other condition
     * @return a composed condition
     */
    default RoleAppearanceCondition and(RoleAppearanceCondition other) {
        return context -> this.shouldAppear(context) && other.shouldAppear(context);
    }

    /**
     * Returns a condition that is the logical OR of this condition and another.
     *
     * @param other the other condition
     * @return a composed condition
     */
    default RoleAppearanceCondition or(RoleAppearanceCondition other) {
        return context -> this.shouldAppear(context) || other.shouldAppear(context);
    }

    /**
     * Returns a condition that is the logical negation of this condition.
     *
     * @return a negated condition
     */
    default RoleAppearanceCondition negate() {
        return context -> !this.shouldAppear(context);
    }

    // ==================== Static Factory Methods ====================

    /**
     * Creates a condition that requires minimum player count.
     *
     * @param minPlayers minimum number of players
     * @return a condition that returns true if player count >= minPlayers
     */
    static RoleAppearanceCondition minPlayers(int minPlayers) {
        return context -> context.getTotalPlayerCount() >= minPlayers;
    }

    /**
     * Creates a condition that requires maximum player count.
     *
     * @param maxPlayers maximum number of players
     * @return a condition that returns true if player count <= maxPlayers
     */
    static RoleAppearanceCondition maxPlayers(int maxPlayers) {
        return context -> context.getTotalPlayerCount() <= maxPlayers;
    }

    /**
     * Creates a condition that requires player count within a range.
     *
     * @param minPlayers minimum number of players (inclusive)
     * @param maxPlayers maximum number of players (inclusive)
     * @return a condition that returns true if player count is within range
     */
    static RoleAppearanceCondition playerCountBetween(int minPlayers, int maxPlayers) {
        return context -> context.getTotalPlayerCount() >= minPlayers && context.getTotalPlayerCount() <= maxPlayers;
    }

    /**
     * Creates a condition that requires minimum killer count.
     *
     * @param minKillers minimum number of target killers
     * @return a condition that returns true if target killer count >= minKillers
     */
    static RoleAppearanceCondition minKillers(int minKillers) {
        return context -> context.getTargetKillerCount() >= minKillers;
    }

    /**
     * Creates a condition that requires maximum killer count.
     *
     * @param maxKillers maximum number of target killers
     * @return a condition that returns true if target killer count <= maxKillers
     */
    static RoleAppearanceCondition maxKillers(int maxKillers) {
        return context -> context.getTargetKillerCount() <= maxKillers;
    }

    /**
     * Creates a condition that requires minimum neutral count.
     *
     * @param minNeutrals minimum number of target neutrals
     * @return a condition that returns true if target neutral count >= minNeutrals
     */
    static RoleAppearanceCondition minNeutrals(int minNeutrals) {
        return context -> context.getTargetNeutralCount() >= minNeutrals;
    }

    /**
     * Creates a condition that requires maximum neutral count.
     *
     * @param maxNeutrals maximum number of target neutrals
     * @return a condition that returns true if target neutral count <= maxNeutrals
     */
    static RoleAppearanceCondition maxNeutrals(int maxNeutrals) {
        return context -> context.getTargetNeutralCount() <= maxNeutrals;
    }
}
