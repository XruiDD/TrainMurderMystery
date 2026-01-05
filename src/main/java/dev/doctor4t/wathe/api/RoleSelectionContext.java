package dev.doctor4t.wathe.api;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;

/**
 * Context information for role selection during game initialization.
 * Provides information about the current game configuration to help determine
 * whether a role should appear in the current game.
 *
 * <p>Note: This context only contains static game configuration (player count, target counts).
 * For role dependencies (role A requires role B), use {@link Role#bindWith(Role...)} instead
 * of checking assigned roles, as the assignment order makes such checks unreliable.</p>
 */
public record RoleSelectionContext(
        ServerWorld world,
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
}
