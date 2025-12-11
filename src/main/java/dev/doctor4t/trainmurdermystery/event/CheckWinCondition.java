package dev.doctor4t.trainmurdermystery.event;

import dev.doctor4t.trainmurdermystery.cca.GameWorldComponent;
import dev.doctor4t.trainmurdermystery.game.GameFunctions;
import net.fabricmc.fabric.api.event.Event;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.jetbrains.annotations.Nullable;

import static net.fabricmc.fabric.api.event.EventFactory.createArrayBacked;

/**
 * Event for checking and modifying win conditions.
 * Mods can use this to:
 * 1. Declare a neutral player as the winner
 * 2. Block other factions from winning (by returning NONE when they would win)
 */
public interface CheckWinCondition {

    /**
     * Called every tick to check win conditions.
     * Listeners are called in order. The first listener to return a non-null WinResult
     * with a WinStatus other than NONE will determine the winner.
     *
     * If a listener returns WinResult with WinStatus.NONE, it blocks the current
     * win condition from being checked further (useful for preventing killer/civilian wins).
     */
    Event<CheckWinCondition> EVENT = createArrayBacked(CheckWinCondition.class, listeners -> (world, gameComponent, currentStatus) -> {
        for (CheckWinCondition listener : listeners) {
            WinResult result = listener.checkWin(world, gameComponent, currentStatus);
            if (result != null) {
                return result;
            }
        }
        return null;
    });

    /**
     * Check win conditions.
     *
     * @param world         the server world
     * @param gameComponent the game world component
     * @param currentStatus the current win status being checked (KILLERS, PASSENGERS, TIME, or NONE)
     * @return a WinResult to override the win condition, or null to continue checking
     */
    @Nullable
    WinResult checkWin(ServerWorld world, GameWorldComponent gameComponent, GameFunctions.WinStatus currentStatus);

    /**
     * Result of a win condition check.
     */
    record WinResult(GameFunctions.WinStatus status, @Nullable ServerPlayerEntity winner) {
        /**
         * Creates a result where a specific neutral player wins.
         */
        public static WinResult neutralWin(ServerPlayerEntity winner) {
            return new WinResult(GameFunctions.WinStatus.NEUTRAL, winner);
        }

        /**
         * Creates a result that blocks the current win condition (game continues).
         */
        public static WinResult block() {
            return new WinResult(GameFunctions.WinStatus.NONE, null);
        }

        /**
         * Creates a result that allows the specified faction to win.
         */
        public static WinResult allow(GameFunctions.WinStatus status) {
            return new WinResult(status, null);
        }
    }
}
