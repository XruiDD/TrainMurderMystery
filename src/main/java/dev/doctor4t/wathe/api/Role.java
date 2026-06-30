package dev.doctor4t.wathe.api;

import net.minecraft.util.Identifier;

import java.util.HashSet;
import java.util.Set;

public final class Role {
    private final Identifier identifier;
    private final int color;
    private final boolean isInnocent;
    private final boolean canUseKiller;
    private final MoodType moodType;
    private final int maxSprintTime;
    private final boolean canSeeTime;
    private RoleAppearanceCondition appearanceCondition = RoleAppearanceCondition.ALWAYS;
    private boolean mapSpecific = false;
    private int spawnGroupSize = 1;
    private int slotCost = 1;
    private final Set<Identifier> mutualExclusions = new HashSet<>();

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

    public Role(Identifier identifier, int color, boolean isInnocent, boolean canUseKiller, MoodType moodType, int maxSprintTime, boolean canSeeTime, RoleAppearanceCondition condition) {
        this(identifier,color,isInnocent,canUseKiller,moodType,maxSprintTime,canSeeTime);
        this.appearanceCondition = condition;
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
     * 标记此角色为地图专属角色。
     * 地图专属角色默认关闭，仅当地图配置中 special_roles.enabled_roles 包含此角色ID时才会启用。
     *
     * @return this role for method chaining
     */
    public Role setMapSpecific(boolean mapSpecific) {
        this.mapSpecific = mapSpecific;
        return this;
    }

    /**
     * @return 此角色是否为地图专属角色
     */
    public boolean isMapSpecific() {
        return mapSpecific;
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

    /**
     * 设置此角色被选中时一次性生成的玩家数量（成组生成）。
     * <p>默认 1（普通角色一局至多一人）。设为 N 时，分配阶段一旦选中此角色，
     * 会把同一角色分配给 N 名玩家、占用 N 个对应阵营名额。
     *
     * @param size 组内人数，最小为 1
     * @return this role for method chaining
     */
    public Role setSpawnGroupSize(int size) {
        this.spawnGroupSize = Math.max(1, size);
        return this;
    }

    /**
     * @return 此角色被选中时一次性生成的玩家数量（默认 1）
     */
    public int getSpawnGroupSize() {
        return spawnGroupSize;
    }

    /**
     * 设置此角色被选中时占用的阵营名额(槽位)数量。
     * <p>默认 1。与 {@link #setSpawnGroupSize} 相互独立：例如「成组生成 2 人但只占 1 个名额」=
     * {@code setSpawnGroupSize(2).setSlotCost(1)}。
     *
     * @param cost 占用的槽位数，最小为 1
     * @return this role for method chaining
     */
    public Role setSlotCost(int cost) {
        this.slotCost = Math.max(1, cost);
        return this;
    }

    /**
     * @return 此角色被选中时占用的阵营名额(槽位)数量（默认 1）
     */
    public int getSlotCost() {
        return slotCost;
    }

    /**
     * 将此角色与另一角色设为互斥（一局中至多出现其中一个）。
     * <p>关系是对称的：调用后两个角色互相排除。分配阶段一旦其中一个被选中，
     * 另一个会立即从候选池移除，因此与分配顺序无关。
     *
     * @param other 互斥的另一角色
     * @return this role for method chaining
     */
    public Role addMutualExclusion(Role other) {
        if (other != null && other != this) {
            this.mutualExclusions.add(other.identifier());
            other.mutualExclusions.add(this.identifier());
        }
        return this;
    }

    /**
     * @return 与此角色互斥的角色标识集合
     */
    public Set<Identifier> getMutualExclusions() {
        return mutualExclusions;
    }

    /**
     * @param other 另一角色
     * @return true 如果此角色与 other 互斥
     */
    public boolean excludes(Role other) {
        return other != null && this.mutualExclusions.contains(other.identifier());
    }
}
