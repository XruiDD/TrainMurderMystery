package dev.doctor4t.trainmurdermystery.event;

import dev.doctor4t.trainmurdermystery.cca.PlayerMoodComponent;
import net.fabricmc.fabric.api.event.Event;
import net.minecraft.server.network.ServerPlayerEntity;

import static net.fabricmc.fabric.api.event.EventFactory.createArrayBacked;

/**
 * Event fired when a player completes a task.
 * This event is fired on the SERVER side only.
 * <p>
 * Task types include:
 * <ul>
 *   <li>{@code SLEEP} - Player slept for required duration</li>
 *   <li>{@code OUTSIDE} - Player stayed outside for required duration</li>
 *   <li>{@code EAT} - Player ate food</li>
 *   <li>{@code DRINK} - Player drank a cocktail</li>
 * </ul>
 * <p>
 * Listeners can use this event to:
 * <ul>
 *   <li>Track player task completion statistics</li>
 *   <li>Trigger role-specific effects on task completion</li>
 *   <li>Award bonuses or apply modifiers</li>
 * </ul>
 */
public interface TaskComplete {

    /**
     * Called when a player completes a task.
     * All listeners are invoked in registration order.
     */
    Event<TaskComplete> EVENT = createArrayBacked(TaskComplete.class, listeners -> (player, taskType) -> {
        for (TaskComplete listener : listeners) {
            listener.onTaskComplete(player, taskType);
        }
    });

    /**
     * Called when a player completes a task.
     *
     * @param player   the player who completed the task (server-side only)
     * @param taskType the type of task that was completed
     */
    void onTaskComplete(ServerPlayerEntity player, PlayerMoodComponent.Task taskType);
}
