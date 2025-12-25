package dev.doctor4t.wathe.api.event;

import dev.doctor4t.wathe.client.WatheClient;
import net.fabricmc.fabric.api.event.Event;
import net.minecraft.client.network.ClientPlayerEntity;

import static net.fabricmc.fabric.api.event.EventFactory.createArrayBacked;

/**
 * Callback for determining whether a player is allowed to use chat.
 *
 * <p>This event is fired on the CLIENT SIDE ONLY before the default chat restriction
 * logic is applied. It allows third-party mods to override the default chat restrictions
 * and enable chat in custom scenarios.
 *
 * <p><b>OR Logic:</b> If ANY listener returns {@code true}, chat will be allowed,
 * bypassing all default restrictions. If all listeners return {@code false},
 * the default chat restriction logic will be evaluated.
 *
 * <p><b>Execution Order:</b>
 * <ol>
 *   <li>This event is invoked first</li>
 *   <li>If any listener returns {@code true}, chat is immediately allowed</li>
 *   <li>If all listeners return {@code false}, default restrictions are checked</li>
 *   <li>Default: allow chat in lobby (INACTIVE) and for spectators/creative mode</li>
 *   <li>Default: disable chat during game (STARTING/ACTIVE/STOPPING)</li>
 * </ol>
 *
 * <p><b>Use Cases for Third-Party Mods:</b>
 * <ul>
 *   <li>Allow dead players to communicate in a "ghost chat"</li>
 *   <li>Enable specific roles to use chat during gameplay (e.g., investigators)</li>
 *   <li>Create team-based chat systems for killer team coordination</li>
 *   <li>Implement proximity-based chat that's allowed during active gameplay</li>
 *   <li>Add chat permissions based on custom game mechanics or items</li>
 * </ul>
 *
 * <p><b>Client-Side Only:</b> This event is only fired on the client side because
 * chat UI rendering and input are client-side operations. Server-side chat message
 * handling is separate and should be managed through other mechanisms.
 *
 * @see WatheClient#shouldDisableChat()
 */
public interface AllowPlayerChat {

    /**
     * Callback for determining whether a player can use chat.
     *
     * <p>This event uses OR logic: if any listener returns {@code true},
     * chat will be allowed regardless of other listeners or default restrictions.
     */
    Event<AllowPlayerChat> EVENT = createArrayBacked(AllowPlayerChat.class, listeners -> player -> {
        for (AllowPlayerChat listener : listeners) {
            if (listener.allowChat(player)) {
                return true;
            }
        }
        return false;
    });

    /**
     * Determines whether the given player should be allowed to use chat.
     *
     * @param player the client player attempting to use chat
     * @return {@code true} to allow chat (overriding default restrictions),
     *         {@code false} to defer to other listeners or default logic
     */
    boolean allowChat(ClientPlayerEntity player);
}
