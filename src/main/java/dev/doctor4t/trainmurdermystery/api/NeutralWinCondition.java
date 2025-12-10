package dev.doctor4t.trainmurdermystery.api;

import dev.doctor4t.trainmurdermystery.cca.GameWorldComponent;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

/**
 * Functional interface for defining custom win conditions for neutral roles.
 * Mod developers can implement this interface to create custom victory conditions
 * for their neutral roles.
 */
@FunctionalInterface
public interface NeutralWinCondition {
    /**
     * Checks if the neutral player with this role has met their win condition.
     *
     * @param player        the player with this neutral role
     * @param world         the server world
     * @param gameComponent the game world component containing game state
     * @return true if this player has won, false otherwise
     */
    boolean checkWin(ServerPlayerEntity player, ServerWorld world, GameWorldComponent gameComponent);
}
