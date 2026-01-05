package dev.doctor4t.wathe.api;

import net.minecraft.util.Identifier;

public final class Role {
    private final Identifier identifier;
    private final int color;
    private final boolean isInnocent;
    private final boolean canUseKiller;
    private final MoodType moodType;
    private final int maxSprintTime;
    private final boolean canSeeTime;
    private RoleAppearanceCondition appearanceCondition = RoleAppearanceCondition.ALWAYS;

    public enum MoodType {
        NONE, REAL, FAKE
    }

    /**
     * @param identifier    the mod id and name of the role
     * @param color         the role announcement color
     * @param isInnocent    whether the gun drops when a person with this role is shot and is considered a civilian to the win conditions
     * @param canUseKiller  can see and use the killer features
     * @param moodType      the mood type a role has
     * @param maxSprintTime the maximum sprint time in ticks
     * @param canSeeTime    if the role can see the game timer
     */
    public Role(Identifier identifier, int color, boolean isInnocent, boolean canUseKiller, MoodType moodType, int maxSprintTime, boolean canSeeTime) {
        this.identifier = identifier;
        this.color = color;
        this.isInnocent = isInnocent;
        this.canUseKiller = canUseKiller;
        this.moodType = moodType;
        if (maxSprintTime == Integer.MAX_VALUE){
            maxSprintTime = -1;
        }
        this.maxSprintTime = maxSprintTime;
        this.canSeeTime = canSeeTime;
    }

    public Identifier identifier() {
        return identifier;
    }

    public int color() {
        return color;
    }

    public boolean isInnocent() {
        return isInnocent;
    }

    public boolean canUseKiller() {
        return canUseKiller;
    }

    public MoodType getMoodType() {
        return moodType;
    }

    public int getMaxSprintTime() {
        return maxSprintTime;
    }

    public boolean canSeeTime() {
        return canSeeTime;
    }

    /**
     * @return true if this role is neutral (neither innocent nor killer)
     */
    public boolean isNeutral() {
        return !this.isInnocent && !this.canUseKiller;
    }

    /**
     * @return the faction this role belongs to
     */
    public Faction getFaction() {
        return Faction.fromRole(this);
    }

    /**
     * Gets the appearance condition for this role.
     *
     * @return the appearance condition
     */
    public RoleAppearanceCondition getAppearanceCondition() {
        return appearanceCondition;
    }

    /**
     * Sets the appearance condition for this role.
     * This determines whether the role should be available for selection in a game.
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * // Role only appears when player count >= 8
     * role.setAppearanceCondition(ctx -> ctx.getTotalPlayerCount() >= 8);
     *
     * // Role only appears when there are multiple killers
     * role.setAppearanceCondition(ctx -> ctx.getTargetKillerCount() > 1);
     *
     * // Using static factory methods
     * role.setAppearanceCondition(RoleAppearanceCondition.minPlayers(8));
     * role.setAppearanceCondition(RoleAppearanceCondition.minKillers(2));
     * }</pre>
     *
     * @param condition the appearance condition
     * @return this role for method chaining
     */
    public Role setAppearanceCondition(RoleAppearanceCondition condition) {
        this.appearanceCondition = condition != null ? condition : RoleAppearanceCondition.ALWAYS;
        return this;
    }

    /**
     * Checks whether this role should appear in the current game based on the selection context.
     *
     * @param context the role selection context
     * @return true if the role should be available for selection
     */
    public boolean shouldAppear(RoleSelectionContext context) {
        return appearanceCondition.shouldAppear(context);
    }
}
