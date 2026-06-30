package dev.doctor4t.wathe.cca;

import dev.doctor4t.wathe.Wathe;
import dev.doctor4t.wathe.api.Faction;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.RoleSelectionContext;
import dev.doctor4t.wathe.api.WatheRoles;
import dev.doctor4t.wathe.game.rotation.RoleCategory;
import dev.doctor4t.wathe.game.rotation.RoleRotation;
import dev.doctor4t.wathe.game.rotation.RotationStrength;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;

import java.util.*;
import java.util.function.DoubleSupplier;

import static net.minecraft.util.Util.shuffle;

public class ScoreboardRoleSelectorComponent implements AutoSyncedComponent {
    public static final ComponentKey<ScoreboardRoleSelectorComponent> KEY = ComponentRegistry.getOrCreate(Wathe.id("rolecounter"), ScoreboardRoleSelectorComponent.class);
    public final Scoreboard scoreboard;
    public final MinecraftServer server;
    public final Map<Role, List<UUID>> forcedRoles = new HashMap<>();

    public ScoreboardRoleSelectorComponent(Scoreboard scoreboard, @Nullable MinecraftServer server) {
        this.scoreboard = scoreboard;
        this.server = server;
    }

    public List<UUID> getForcedForRole(Role role) {
        return forcedRoles.computeIfAbsent(role, k -> new ArrayList<>());
    }

    /**
     * Adds a player to the forced role list for the specified role.
     * If the player is already in another role's forced list, they will be removed from it first.
     * This prevents the same player from being forced into multiple roles.
     *
     * @param role the role to force the player into
     * @param uuid the UUID of the player
     */
    public void addForcedRole(Role role, UUID uuid) {
        // Remove player from all other forced role lists first
        for (Map.Entry<Role, List<UUID>> entry : forcedRoles.entrySet()) {
            entry.getValue().remove(uuid);
        }
        // Add to the specified role's list
        getForcedForRole(role).add(uuid);
    }

    /**
     * Removes a player from all forced role lists.
     *
     * @param uuid the UUID of the player to remove
     */
    public void removeForcedRole(UUID uuid) {
        for (List<UUID> uuids : forcedRoles.values()) {
            uuids.remove(uuid);
        }
    }

    /**
     * Gets the forced role for a specific player, if any.
     *
     * @param uuid the UUID of the player
     * @return the role the player is forced into, or null if not forced
     */
    public @Nullable Role getForcedRoleForPlayer(UUID uuid) {
        for (Map.Entry<Role, List<UUID>> entry : forcedRoles.entrySet()) {
            if (entry.getValue().contains(uuid)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public int reset() {
        this.forcedRoles.clear();
        return 1;
    }

    public void assignForcedRoles(ServerWorld world, GameWorldComponent gameComponent, @NotNull List<ServerPlayerEntity> players) {
        for (Map.Entry<Role, List<UUID>> entry : forcedRoles.entrySet()) {
            Role role = entry.getKey();
            List<UUID> uuids = entry.getValue();
            for (UUID uuid : uuids) {
                PlayerEntity player = world.getPlayerByUuid(uuid);
                if (player instanceof ServerPlayerEntity serverPlayer && players.contains(serverPlayer)) {
                    gameComponent.addRole(player, role);
                }
            }
            uuids.clear();
        }
        forcedRoles.clear();
    }

    /**
     * Creates a RoleSelectionContext with the current game configuration.
     * This context only contains static information (player count, target counts)
     * that doesn't change during role assignment.
     */
    public RoleSelectionContext createSelectionContext(ServerWorld world, GameWorldComponent gameComponent, @NotNull List<ServerPlayerEntity> players) {
        int totalPlayerCount = players.size();
        int targetKillerCount = (int) Math.floor((double) totalPlayerCount / gameComponent.getKillerDividend());
        int targetNeutralCount = (int) Math.floor((double) totalPlayerCount / gameComponent.getNeutralDividend());
        int targetVigilanteCount = (int) Math.floor((double) totalPlayerCount / gameComponent.getVigilanteDividend());

        return new RoleSelectionContext(
                world,
                gameComponent,
                Collections.unmodifiableList(players),
                totalPlayerCount,
                targetKillerCount,
                targetNeutralCount,
                targetVigilanteCount
        );
    }

    public int assignKillers(ServerWorld world, GameWorldComponent gameComponent, @NotNull List<ServerPlayerEntity> players, int killerCount) {
        // Collect already-assigned killer roles (from forced roles) and count them
        Set<Role> assignedKillerRoles = new HashSet<>();
        int existingKillerCount = 0;
        for (ServerPlayerEntity player : players) {
            Role role = gameComponent.getRole(player);
            if (role != null && role.getFaction() == Faction.KILLER) {
                assignedKillerRoles.add(role);
                existingKillerCount++;
            }
        }

        // Adjust killer count by subtracting existing killers
        killerCount = Math.max(0, killerCount - existingKillerCount);

        ArrayList<ServerPlayerEntity> availablePlayers = getAvailablePlayers(world, gameComponent, players);
        RoleSelectionContext context = createSelectionContext(world, gameComponent, players);

        // Collect available non-vanilla killer faction roles (each can only be assigned once)
        ArrayList<Role> availableSpecialKillerRoles = new ArrayList<>();
        for (Role role : WatheRoles.ROLES) {
            if (role.getFaction() == Faction.KILLER && !WatheRoles.VANILLA_ROLES.contains(role) && gameComponent.isRoleEnabled(role) && !assignedKillerRoles.contains(role) && role.shouldAppear(context)) {
                availableSpecialKillerRoles.add(role);
            }
        }

        RoleHistoryComponent hist = RoleHistoryComponent.KEY.get(this.scoreboard);
        RotationStrength strength = gameComponent.getRoleRotationStrength();
        boolean avoid = gameComponent.isRoleSpecificRoleAvoidance();
        DoubleSupplier rng = () -> world.getRandom().nextDouble();

        // Pick WHO becomes killer by KILLER-debt weighting (replaces shuffle)；成组角色预留额外人数
        int playersNeeded = Math.min(availablePlayers.size(), killerCount + extraPlayersForGroups(availableSpecialKillerRoles));
        List<ServerPlayerEntity> chosen = selectByDebt(availablePlayers, playersNeeded, hist, RoleCategory.KILLER, strength, rng);
        // 兜底基础杀手，统一处理 成组/槽位/互斥
        return existingKillerCount + assignSpecialRolesBySlots(gameComponent, chosen, availableSpecialKillerRoles, killerCount, hist, avoid, rng, WatheRoles.KILLER);
    }

    public void assignVigilantes(ServerWorld world, GameWorldComponent gameComponent, @NotNull List<ServerPlayerEntity> players, int vigilanteCount) {
        // Count already-assigned vigilantes and veterans (from forced roles)
        int existingVigilanteCount = 0;
        int existingVeteranCount = 0;
        for (ServerPlayerEntity player : players) {
            Role role = gameComponent.getRole(player);
            if (role == WatheRoles.VIGILANTE) {
                existingVigilanteCount++;
            } else if (role == WatheRoles.VETERAN) {
                existingVeteranCount++;
            }
        }

        int existingTotal = existingVigilanteCount + existingVeteranCount;
        int remainingToAssign = Math.max(0, vigilanteCount - existingTotal);

        ArrayList<ServerPlayerEntity> availablePlayers = getAvailablePlayers(world, gameComponent, players);
        RoleHistoryComponent hist = RoleHistoryComponent.KEY.get(this.scoreboard);
        RotationStrength strength = gameComponent.getRoleRotationStrength();
        DoubleSupplier rng = () -> world.getRandom().nextDouble();

        // Pick WHO becomes a vigilante-type by VIGILANTE-debt weighting (replaces shuffle)
        List<ServerPlayerEntity> chosen = selectByDebt(availablePlayers, remainingToAssign, hist, RoleCategory.VIGILANTE, strength, rng);

        // Keep the existing 2 vigilantes : 1 veteran alternation
        int vigilanteAssigned = existingVigilanteCount;
        int veteranAssigned = existingVeteranCount;
        for (ServerPlayerEntity player : chosen) {
            int totalAssigned = vigilanteAssigned + veteranAssigned;
            if (totalAssigned % 3 != 2) {
                gameComponent.addRole(player, WatheRoles.VIGILANTE);
                vigilanteAssigned++;
            } else {
                gameComponent.addRole(player, WatheRoles.VETERAN);
                veteranAssigned++;
            }
        }
    }

    public int assignNeutrals(ServerWorld world, GameWorldComponent gameComponent, @NotNull List<ServerPlayerEntity> players, int neutralCount) {
        // Collect already-assigned neutral roles (from forced roles)
        Set<Role> assignedNeutralRoles = new HashSet<>();
        for (ServerPlayerEntity player : players) {
            Role role = gameComponent.getRole(player);
            if (role != null && role.getFaction() == Faction.NEUTRAL && role != WatheRoles.NO_ROLE) {
                assignedNeutralRoles.add(role);
            }
        }

        // Adjust neutral count by subtracting existing neutrals
        neutralCount = Math.max(0, neutralCount - assignedNeutralRoles.size());

        // Create selection context for checking role appearance conditions
        RoleSelectionContext context = createSelectionContext(world, gameComponent, players);

        // Collect available non-vanilla neutral faction roles (each can only be assigned once)
        ArrayList<Role> availableNeutralRoles = new ArrayList<>();
        for (Role role : WatheRoles.ROLES) {
            if (role.getFaction() == Faction.NEUTRAL && !WatheRoles.VANILLA_ROLES.contains(role) && gameComponent.isRoleEnabled(role) && !assignedNeutralRoles.contains(role) && role.shouldAppear(context)) {
                availableNeutralRoles.add(role);
            }
        }

        // If no special neutral roles registered, skip neutral assignment
        if (availableNeutralRoles.isEmpty()) {
            return assignedNeutralRoles.size();
        }

        ArrayList<ServerPlayerEntity> availablePlayers = getAvailablePlayers(world, gameComponent, players);
        RoleHistoryComponent hist = RoleHistoryComponent.KEY.get(this.scoreboard);
        RotationStrength strength = gameComponent.getRoleRotationStrength();
        boolean avoid = gameComponent.isRoleSpecificRoleAvoidance();
        DoubleSupplier rng = () -> world.getRandom().nextDouble();

        // 中立名额(槽位)预算：成组角色按 slotCost 占槽，但一槽可多名玩家，故额外多选备用人数
        int playersNeeded = Math.min(availablePlayers.size(), neutralCount + extraPlayersForGroups(availableNeutralRoles));
        List<ServerPlayerEntity> chosen = selectByDebt(availablePlayers, playersNeeded, hist, RoleCategory.NEUTRAL, strength, rng);
        // 不兜底：剩余中立槽位留空，对应玩家由后续平民阶段接管
        return assignedNeutralRoles.size() + assignSpecialRolesBySlots(gameComponent, chosen, availableNeutralRoles, neutralCount, hist, avoid, rng, null);
    }

    /** 移除与已分配角色互斥的候选角色（立即生效，保证互斥与分配顺序无关）。 */
    private void removeExcludedRoles(Role assigned, List<Role> available) {
        if (assigned.getMutualExclusions().isEmpty()) return;
        available.removeIf(assigned::excludes);
    }

    /** 成组角色比其槽位多需要的玩家数之和：用于按槽位选人时预留额外名额。 */
    private int extraPlayersForGroups(List<Role> roles) {
        int extra = 0;
        for (Role role : roles) extra += Math.max(0, role.getSpawnGroupSize() - role.getSlotCost());
        return extra;
    }

    /**
     * 按「槽位预算」为已选玩家分配特殊角色，统一处理三种通用能力：
     * 成组生成({@link Role#getSpawnGroupSize()} 一次给多名玩家)、
     * 槽位占用({@link Role#getSlotCost()} 占用阵营名额数)、
     * 互斥({@link Role#getMutualExclusions()} 选中即把互斥角色移出候选)。
     *
     * @param chosen     已按债务选好的玩家（应已用 {@link #extraPlayersForGroups} 预留成组额外人数）
     * @param available  候选特殊角色（可变，会被消耗）
     * @param slotBudget 槽位预算
     * @param fallback   候选耗尽/凑不齐时的兜底角色（占 1 槽 1 人）；为 null 表示不兜底（剩余槽位留空）
     * @return 实际分配到角色的玩家数
     */
    private int assignSpecialRolesBySlots(GameWorldComponent gameComponent, List<ServerPlayerEntity> chosen,
                                          List<Role> available, int slotBudget, RoleHistoryComponent hist,
                                          boolean avoid, DoubleSupplier rng, Role fallback) {
        int assigned = 0;
        int slotsRemaining = slotBudget;
        int i = 0;
        while (slotsRemaining > 0 && i < chosen.size()) {
            ServerPlayerEntity player = chosen.get(i);
            Role role = pickSpecialRole(player, available, hist, avoid, rng);
            if (role == null) {
                if (fallback == null) break; // 不兜底：剩余槽位留空，玩家交由后续阶段处理
                gameComponent.addRole(player, fallback);
                assigned++;
                i++;
                slotsRemaining--;
                continue;
            }
            int groupSize = Math.max(1, role.getSpawnGroupSize());
            int cost = Math.max(1, role.getSlotCost());
            if (cost > slotsRemaining || chosen.size() - i < groupSize) {
                // 槽位或玩家凑不齐该角色：放弃它（已被 pickSpecialRole 移除，故不应用其互斥）
                if (fallback != null) {
                    gameComponent.addRole(player, fallback);
                    assigned++;
                    i++;
                    slotsRemaining--;
                }
                // 无兜底则不推进 i，下一轮用同一玩家试下一个候选；候选耗尽后 role==null 退出，不会死循环
                continue;
            }
            for (int g = 0; g < groupSize; g++) {
                gameComponent.addRole(chosen.get(i + g), role);
                assigned++;
            }
            i += groupSize;
            slotsRemaining -= cost;
            removeExcludedRoles(role, available);
        }
        return assigned;
    }

    public int assignCivilians(ServerWorld world, GameWorldComponent gameComponent, @NotNull List<ServerPlayerEntity> players) {
        ArrayList<ServerPlayerEntity> availablePlayers = getAvailablePlayers(world, gameComponent, players);

        // Collect already-assigned civilian roles (from forced roles)
        Set<Role> assignedCivilianRoles = new HashSet<>();
        for (ServerPlayerEntity player : players) {
            Role role = gameComponent.getRole(player);
            if (role != null && role.getFaction() == Faction.CIVILIAN) {
                assignedCivilianRoles.add(role);
            }
        }

        // Create selection context for checking role appearance conditions
        RoleSelectionContext context = createSelectionContext(world, gameComponent, players);

        // Collect available non-vanilla civilian faction roles (each can only be assigned once)
        ArrayList<Role> availableSpecialCivilianRoles = new ArrayList<>();
        for (Role role : WatheRoles.ROLES) {
            if (role.getFaction() == Faction.CIVILIAN && !WatheRoles.VANILLA_ROLES.contains(role) && gameComponent.isRoleEnabled(role) && !assignedCivilianRoles.contains(role) && role.shouldAppear(context)) {
                availableSpecialCivilianRoles.add(role);
            }
        }

        RoleHistoryComponent hist = RoleHistoryComponent.KEY.get(this.scoreboard);
        boolean avoid = gameComponent.isRoleSpecificRoleAvoidance();
        DoubleSupplier rng = () -> world.getRandom().nextDouble();

        // 平民是兜底桶：给所有剩余玩家分配（无权重）。支持 成组生成 + 互斥；slotCost 不适用(人人都要分到)
        int assignedCount = 0;
        int i = 0;
        while (i < availablePlayers.size()) {
            ServerPlayerEntity player = availablePlayers.get(i);
            Role role = pickSpecialRole(player, availableSpecialCivilianRoles, hist, avoid, rng);
            if (role == null) {
                gameComponent.addRole(player, WatheRoles.CIVILIAN); // 无特殊角色 → 基础平民
                assignedCount++;
                i++;
                continue;
            }
            int groupSize = Math.max(1, role.getSpawnGroupSize());
            if (availablePlayers.size() - i < groupSize) {
                // 剩余玩家凑不齐整组 → 该玩家给基础平民（放弃该特殊角色，不应用其互斥）
                gameComponent.addRole(player, WatheRoles.CIVILIAN);
                assignedCount++;
                i++;
                continue;
            }
            for (int g = 0; g < groupSize; g++) {
                gameComponent.addRole(availablePlayers.get(i + g), role);
                assignedCount++;
            }
            i += groupSize;
            removeExcludedRoles(role, availableSpecialCivilianRoles);
        }
        return assignedCount;
    }

    private ArrayList<ServerPlayerEntity> getAvailablePlayers(ServerWorld world, GameWorldComponent gameComponent, @NotNull List<ServerPlayerEntity> players) {
        ArrayList<ServerPlayerEntity> availablePlayers = new ArrayList<>();
        for (ServerPlayerEntity player : players) {
            if (!gameComponent.hasAnyRole(player)) {
                availablePlayers.add(player);
            }
        }
        shuffle(availablePlayers, world.getRandom());
        return availablePlayers;
    }

    /** 按某阵营债务对玩家加权，无放回抽 count 个。OFF 档权重恒等 = 均匀随机。 */
    private List<ServerPlayerEntity> selectByDebt(List<ServerPlayerEntity> pool, int count,
                                                  RoleHistoryComponent hist, RoleCategory cat,
                                                  RotationStrength strength, DoubleSupplier rng) {
        return RoleRotation.selectWeighted(pool,
                p -> RoleRotation.weight(hist.debt(p.getUuid(), cat), strength),
                count, rng);
    }

    /** 为玩家从可用特殊角色里挑一个(尽量没玩过)；avoid=false 时纯随机。命中后从 available 移除。 */
    private Role pickSpecialRole(ServerPlayerEntity player, List<Role> available,
                                 RoleHistoryComponent hist, boolean avoid, DoubleSupplier rng) {
        Set<String> recent = avoid ? hist.recentRoleIds(player.getUuid()) : Collections.emptySet();
        Role role = RoleRotation.pickAvoidingRecent(available,
                r -> recent.contains(r.identifier().toString()), rng);
        if (role != null) available.remove(role);
        return role;
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
    }
}
