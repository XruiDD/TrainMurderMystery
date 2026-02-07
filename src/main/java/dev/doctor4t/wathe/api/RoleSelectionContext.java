package dev.doctor4t.wathe.api;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;

/**
 * Context information for role selection during game initialization.
 * Provides information about the current game configuration to help determine
 * whether a role should appear in the current game.
 *
 * <p>Note: Killers are assigned before vigilantes, vigilantes before neutrals,
 * and neutrals before civilians. {@link #isRoleAssigned(Role)} checks roles
 * already assigned to players at the time of the query, so it is reliable for
 * checking roles from earlier phases (e.g., a civilian role checking if a
 * killer role was assigned).</p>
 */
public record RoleSelectionContext(
        ServerWorld world,
        GameWorldComponent gameComponent,
        @Unmodifiable List<ServerPlayerEntity> players,
        int totalPlayerCount,
        int targetKillerCount,
        int targetNeutralCount,
        int targetVigilanteCount
) {
    /**
     * @return the total number of players in the game
     */
    public int getTotalPlayerCount() {
        return totalPlayerCount;
    }

    /**
     * @return the target number of killers for this game
     */
    public int getTargetKillerCount() {
        return targetKillerCount;
    }

    /**
     * @return the target number of neutral roles for this game
     */
    public int getTargetNeutralCount() {
        return targetNeutralCount;
    }

    /**
     * @return the target number of vigilantes for this game
     */
    public int getTargetVigilanteCount() {
        return targetVigilanteCount;
    }

    /**
     * @return the server world
     */
    public ServerWorld getWorld() {
        return world;
    }

    /**
     * @return unmodifiable list of all players in the game
     */
    @Unmodifiable
    public List<ServerPlayerEntity> getPlayers() {
        return players;
    }

    /**
     * Checks whether a specific role has been assigned to any player in the current game.
     * Since roles are assigned in order (killers → vigilantes → neutrals → civilians),
     * this is reliable for checking roles from earlier assignment phases.
     *
     * <p>Example: A civilian role can reliably check if a killer role was assigned,
     * because all killer roles are assigned before civilian roles.</p>
     *
     * @param role the role to check
     * @return true if at least one player has been assigned this role
     */
    public boolean isRoleAssigned(Role role) {
        return !gameComponent.getAllWithRole(role).isEmpty();
    }
}
