package dev.doctor4t.wathe.api.event;

import net.fabricmc.fabric.api.event.Event;
import net.minecraft.server.network.ServerPlayerEntity;

import static net.fabricmc.fabric.api.event.EventFactory.createArrayBacked;

/**
 * Event called when a player is reset to their initial state.
 * This event is fired on the SERVER side only.
 * <p>
 * This event is triggered when:
 * <ul>
 *   <li>A player dies and needs to be respawned</li>
 *   <li>A player is being reset for a new game round</li>
 *   <li>Game state requires resetting player-specific data</li>
 * </ul>
 * <p>
 * Listeners should use this event to:
 * <ul>
 *   <li>Clear player-specific component data</li>
 *   <li>Reset custom player states</li>
 *   <li>Clean up player-related resources</li>
 * </ul>
 * <p>
 * Note: Basic player state (inventory, position, health) is handled separately.
 * This event is for custom mod-specific state only.
 */
public interface ResetPlayer {

    /**
     * Called when a player is being reset.
     * All listeners are invoked in registration order.
     */
    Event<ResetPlayer> EVENT = createArrayBacked(ResetPlayer.class, listeners -> player -> {
        for (ResetPlayer listener : listeners) {
            listener.onReset(player);
        }
    });

    /**
     * Perform reset operations on the player.
     *
     * @param player the player being reset (server-side only)
     */
    void onReset(ServerPlayerEntity player);
}
