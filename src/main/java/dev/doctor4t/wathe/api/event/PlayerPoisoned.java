package dev.doctor4t.wathe.api.event;

import net.fabricmc.fabric.api.event.Event;
import net.minecraft.entity.player.PlayerEntity;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

import static net.fabricmc.fabric.api.event.EventFactory.createArrayBacked;

/**
 * Events related to player poisoning.
 * These events are fired on the SERVER side only.
 */
public final class PlayerPoisoned {

    private PlayerPoisoned() {
    }

    /**
     * Called BEFORE a player is poisoned.
     * Listeners can cancel the poison effect.
     * <p>
     * Return values:
     * <ul>
     *   <li>{@code PoisonResult.allow()}: Allow the poison effect</li>
     *   <li>{@code PoisonResult.cancel()}: Cancel the poison effect</li>
     *   <li>{@code null}: Defer to next listener or default behavior</li>
     * </ul>
     */
    public static final Event<Before> BEFORE = createArrayBacked(Before.class, listeners -> (player, ticks, poisoner) -> {
        for (Before listener : listeners) {
            PoisonResult result = listener.beforePlayerPoisoned(player, ticks, poisoner);
            if (result != null) {
                return result;
            }
        }
        return null;
    });

    /**
     * Called AFTER a player has been poisoned.
     * Cannot cancel the poison - use BEFORE event for that.
     * Useful for tracking or side effects.
     */
    public static final Event<After> AFTER = createArrayBacked(After.class, listeners -> (player, ticks, poisoner) -> {
        for (After listener : listeners) {
            listener.afterPlayerPoisoned(player, ticks, poisoner);
        }
    });

    @FunctionalInterface
    public interface Before {
        /**
         * Called before a player is poisoned.
         *
         * @param player The player being poisoned
         * @param ticks The number of ticks until death
         * @param poisoner The UUID of the player who poisoned them (may be null)
         * @return {@code PoisonResult} to override, or {@code null} to defer
         */
        @Nullable
        PoisonResult beforePlayerPoisoned(PlayerEntity player, int ticks, UUID poisoner);
    }

    @FunctionalInterface
    public interface After {
        /**
         * Called after a player has been poisoned.
         *
         * @param player The player that was poisoned
         * @param ticks The number of ticks until death
         * @param poisoner The UUID of the player who poisoned them (may be null)
         */
        void afterPlayerPoisoned(PlayerEntity player, int ticks, UUID poisoner);
    }

    /**
     * Result of a poison validation.
     */
    public record PoisonResult(boolean cancelled) {

        /**
         * Allow the poison effect.
         */
        public static PoisonResult allow() {
            return new PoisonResult(false);
        }

        /**
         * Cancel the poison effect.
         */
        public static PoisonResult cancel() {
            return new PoisonResult(true);
        }
    }
}
