package dev.doctor4t.wathe.cca;

import dev.doctor4t.wathe.Wathe;
import dev.doctor4t.wathe.api.Faction;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.RoleSelectionContext;
import dev.doctor4t.wathe.api.WatheRoles;
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
        // Collect already assigned killer roles (from forced roles)
        Set<Role> assignedKillerRoles = new HashSet<>();
        for (ServerPlayerEntity player : players) {
            Role role = gameComponent.getRole(player);
            if (role != null && role.getFaction() == Faction.KILLER) {
                assignedKillerRoles.add(role);
            }
        }

        // Count already assigned killers (from forced roles)
        int existingKillerCount = 0;
        for (ServerPlayerEntity player : players) {
            Role role = gameComponent.getRole(player);
            if (role != null && role.getFaction() == Faction.KILLER) {
                existingKillerCount++;
            }
        }

        // Adjust killer count by subtracting existing killers
        killerCount = Math.max(0, killerCount - existingKillerCount);

        ArrayList<ServerPlayerEntity> availablePlayers = getAvailablePlayers(world, gameComponent, players);

        // Create selection context for checking role appearance conditions
        RoleSelectionContext context = createSelectionContext(world, gameComponent, players);

        // Collect available non-vanilla killer faction roles (each can only be assigned once)
        // Filter out roles that are already assigned or don't meet appearance conditions
        ArrayList<Role> availableSpecialKillerRoles = new ArrayList<>();
        for (Role role : WatheRoles.ROLES) {
            if (role.getFaction() == Faction.KILLER && !WatheRoles.VANILLA_ROLES.contains(role) && gameComponent.isRoleEnabled(role) && !assignedKillerRoles.contains(role) && role.shouldAppear(context)) {
                availableSpecialKillerRoles.add(role);
            }
        }
        shuffle(availableSpecialKillerRoles, world.getRandom());

        int assignedCount = existingKillerCount;
        // Assign killers randomly
        for (ServerPlayerEntity player : availablePlayers) {
            if (killerCount <= 0) break;

            Role assignedRole;
            if (!availableSpecialKillerRoles.isEmpty()) {
                // Assign a special non-vanilla killer role
                assignedRole = availableSpecialKillerRoles.removeFirst();
            } else {
                // No special roles left, assign basic KILLER
                assignedRole = WatheRoles.KILLER;
            }

            gameComponent.addRole(player, assignedRole);
            assignedCount++;
            killerCount--;
        }

        return assignedCount;
    }

    public void assignVigilantes(ServerWorld world, GameWorldComponent gameComponent, @NotNull List<ServerPlayerEntity> players, int vigilanteCount) {
        // Count already assigned vigilantes and veterans (from forced roles)
        int existingVigilanteCount = 0;
        int existingVeteranCount = 0;
        for (ServerPlayerEntity player : players) {
            Role role = gameComponent.getRole(player);
            if (role != null && role == WatheRoles.VIGILANTE) {
                existingVigilanteCount++;
            } else if (role != null && role == WatheRoles.VETERAN) {
                existingVeteranCount++;
            }
        }

        // Calculate total vigilante-type roles needed (vigilante + veteran)
        int totalVigilanteTypeCount = vigilanteCount;
        int existingTotal = existingVigilanteCount + existingVeteranCount;
        int remainingToAssign = Math.max(0, totalVigilanteTypeCount - existingTotal);

        // Get available players
        ArrayList<ServerPlayerEntity> availablePlayers = getAvailablePlayers(world, gameComponent, players);

        // Assign alternating: vigilante, veteran, vigilante, veteran...
        // Track how many of each we've assigned so far (including existing forced roles)
        int vigilanteAssigned = existingVigilanteCount;
        int veteranAssigned = existingVeteranCount;

        for (ServerPlayerEntity player : availablePlayers) {
            if (remainingToAssign <= 0) break;

            // Assign based on ratio: 2 vigilantes per 1 veteran
            // Pattern: 1st vigilante, 2nd vigilante, 3rd veteran, 4th vigilante, 5th vigilante, 6th veteran...
            int totalAssigned = vigilanteAssigned + veteranAssigned;
            if (totalAssigned % 3 != 2) {
                // Index 0, 1, 3, 4, 6, 7... -> assign vigilante
                gameComponent.addRole(player, WatheRoles.VIGILANTE);
                vigilanteAssigned++;
            } else {
                // Index 2, 5, 8... -> assign veteran
                gameComponent.addRole(player, WatheRoles.VETERAN);
                veteranAssigned++;
            }
            remainingToAssign--;
        }
    }

    public int assignNeutrals(ServerWorld world, GameWorldComponent gameComponent, @NotNull List<ServerPlayerEntity> players, int neutralCount) {
        // Collect already assigned neutral roles (from forced roles)
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
        // Filter out roles that are already assigned or don't meet appearance conditions
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

        shuffle(availableNeutralRoles, world.getRandom());

        // Get available players
        ArrayList<ServerPlayerEntity> availablePlayers = getAvailablePlayers(world, gameComponent, players);

        int assignedCount = assignedNeutralRoles.size();
        // Assign neutral roles randomly (one role per player, one player per role)
        for (ServerPlayerEntity player : availablePlayers) {
            if (neutralCount <= 0) break;
            if (availableNeutralRoles.isEmpty()) break;

            Role assignedRole = availableNeutralRoles.removeFirst();
            gameComponent.addRole(player, assignedRole);
            assignedCount++;
            neutralCount--;
        }

        return assignedCount;
    }

    public int assignCivilians(ServerWorld world, GameWorldComponent gameComponent, @NotNull List<ServerPlayerEntity> players) {
        // Get available players
        ArrayList<ServerPlayerEntity> availablePlayers = getAvailablePlayers(world, gameComponent, players);

        // Collect already assigned civilian roles (from forced roles)
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
        // Filter out roles that are already assigned or don't meet appearance conditions
        ArrayList<Role> availableSpecialCivilianRoles = new ArrayList<>();
        for (Role role : WatheRoles.ROLES) {
            if (role.getFaction() == Faction.CIVILIAN && !WatheRoles.VANILLA_ROLES.contains(role) && gameComponent.isRoleEnabled(role) && !assignedCivilianRoles.contains(role) && role.shouldAppear(context)) {
                availableSpecialCivilianRoles.add(role);
            }
        }
        shuffle(availableSpecialCivilianRoles, world.getRandom());

        int assignedCount = 0;
        // Assign civilians to all remaining players
        for (ServerPlayerEntity player : availablePlayers) {
            Role assignedRole;
            if (!availableSpecialCivilianRoles.isEmpty()) {
                // Assign a special non-vanilla civilian role
                assignedRole = availableSpecialCivilianRoles.removeFirst();
            } else {
                // No special roles left, assign basic CIVILIAN
                assignedRole = WatheRoles.CIVILIAN;
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

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
    }
}