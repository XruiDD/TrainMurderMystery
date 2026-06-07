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

        // Pick WHO becomes killer by KILLER-debt weighting (replaces shuffle)
        List<ServerPlayerEntity> chosen = selectByDebt(availablePlayers, killerCount, hist, RoleCategory.KILLER, strength, rng);

        int assignedCount = existingKillerCount;
        for (ServerPlayerEntity player : chosen) {
            Role assignedRole = pickSpecialRole(player, availableSpecialKillerRoles, hist, avoid, rng);
            if (assignedRole == null) {
                assignedRole = WatheRoles.KILLER; // no special role left -> base killer
            }
            gameComponent.addRole(player, assignedRole);
            assignedCount++;
        }
        return assignedCount;
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

        // One role per player & one player per role -> cap by available roles
        int toAssign = Math.min(neutralCount, availableNeutralRoles.size());
        List<ServerPlayerEntity> chosen = selectByDebt(availablePlayers, toAssign, hist, RoleCategory.NEUTRAL, strength, rng);

        int assignedCount = assignedNeutralRoles.size();
        for (ServerPlayerEntity player : chosen) {
            if (availableNeutralRoles.isEmpty()) break;
            Role assignedRole = pickSpecialRole(player, availableNeutralRoles, hist, avoid, rng);
            if (assignedRole == null) break;
            gameComponent.addRole(player, assignedRole);
            assignedCount++;
        }
        return assignedCount;
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

        int assignedCount = 0;
        // Assign civilians to all remaining players (no weighting; civilian is the fallback bucket)
        for (ServerPlayerEntity player : availablePlayers) {
            Role assignedRole = pickSpecialRole(player, availableSpecialCivilianRoles, hist, avoid, rng);
            if (assignedRole == null) {
                assignedRole = WatheRoles.CIVILIAN; // no special role left -> base civilian
            }
            gameComponent.addRole(player, assignedRole);
            assignedCount++;
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
