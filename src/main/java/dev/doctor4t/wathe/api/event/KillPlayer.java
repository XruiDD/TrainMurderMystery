package dev.doctor4t.wathe.api.event;

import net.fabricmc.fabric.api.event.Event;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import static net.fabricmc.fabric.api.event.EventFactory.createArrayBacked;

/**
 * Events related to player being killed.
 * These events are fired on the SERVER side only.
 */
public final class KillPlayer {

    private KillPlayer() {
    }

    /**
     * Called BEFORE a player is killed (after AllowPlayerDeath check passes).
     * Listeners can modify or react to the kill.
     * <p>
     * Return values:
     * <ul>
     *   <li>{@code KillResult.allow()}: Allow the kill to proceed</li>
     *   <li>{@code KillResult.cancel()}: Cancel the kill</li>
     *   <li>{@code null}: Defer to next listener or default behavior</li>
     * </ul>
     */
    public static final Event<Before> BEFORE = createArrayBacked(Before.class, listeners -> (victim, killer, deathReason) -> {
        for (Before listener : listeners) {
            KillResult result = listener.beforeKillPlayer(victim, killer, deathReason);
            if (result != null) {
                return result;
            }
        }
        return null;
    });

    /**
     * Called AFTER a player has been killed.
     * Cannot cancel the kill - use BEFORE event for that.
     * Useful for tracking, scoring, or triggering win conditions.
     */
    public static final Event<After> AFTER = createArrayBacked(After.class, listeners -> (victim, killer, deathReason) -> {
        for (After listener : listeners) {
            listener.afterKillPlayer(victim, killer, deathReason);
        }
    });

    @FunctionalInterface
    public interface Before {
        /**
         * Called before a player is killed.
         *
         * @param victim The player being killed
         * @param killer The player who killed them (may be null for environmental deaths)
         * @param deathReason The identifier of the death reason
         * @return {@code KillResult} to override, or {@code null} to defer
         */
        @Nullable
        KillResult beforeKillPlayer(ServerPlayerEntity victim, @Nullable ServerPlayerEntity killer, Identifier deathReason);
    }

    @FunctionalInterface
    public interface After {
        /**
         * Called after a player has been killed.
         *
         * @param victim The player that was killed
         * @param killer The player who killed them (may be null for environmental deaths)
         * @param deathReason The identifier of the death reason
         */
        void afterKillPlayer(ServerPlayerEntity victim, @Nullable ServerPlayerEntity killer, Identifier deathReason);
    }

    /**
     * Result of a kill validation.
     * @param cancelled Whether the kill should be cancelled
     * @param spawnBody Whether to spawn a body (null means use the default value passed to killPlayer)
     */
    public record KillResult(boolean cancelled, Boolean spawnBody) {

        /**
         * Allow the kill to proceed with default body spawning behavior.
         */
        public static KillResult allow() {
            return new KillResult(false, null);
        }

        /**
         * Allow the kill to proceed with specified body spawning behavior.
         * @param spawnBody true to spawn body, false to not spawn body
         */
        public static KillResult allow(boolean spawnBody) {
            return new KillResult(false, spawnBody);
        }

        /**
         * Allow the kill to proceed without spawning a body.
         */
        public static KillResult allowWithoutBody() {
            return new KillResult(false, false);
        }

        /**
         * Cancel the kill.
         */
        public static KillResult cancel() {
            return new KillResult(true, null);
        }
    }
}
