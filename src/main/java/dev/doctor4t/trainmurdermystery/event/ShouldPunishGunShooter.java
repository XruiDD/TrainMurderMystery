package dev.doctor4t.trainmurdermystery.event;

import net.fabricmc.fabric.api.event.Event;
import net.minecraft.entity.player.PlayerEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static net.fabricmc.fabric.api.event.EventFactory.createArrayBacked;

/**
 * Event fired before applying punishment for shooting an innocent player with a gun.
 * Mods can use this to:
 * - Cancel the punishment entirely (no gun drop, no penalty)
 * - Allow normal punishment to proceed
 * - Apply custom punishment logic instead
 * <p>
 * Note: This event is only fired when an innocent player is shot, as killer shooting
 * innocents does not trigger punishment in the base game.
 */
public interface ShouldPunishGunShooter {

    /**
     * Called before punishment is applied for shooting an innocent.
     * <p>
     * Listeners are called in order. The first listener to return a non-null
     * {@link PunishResult} determines the outcome. If all listeners return null,
     * normal punishment logic proceeds.
     */
    Event<ShouldPunishGunShooter> EVENT = createArrayBacked(ShouldPunishGunShooter.class, listeners -> (shooter, victim) -> {
        for (ShouldPunishGunShooter listener : listeners) {
            PunishResult result = listener.shouldPunish(shooter, victim);
            if (result != null) {
                return result;
            }
        }
        return null; // No mod intervened, proceed with normal punishment
    });

    /**
     * Check if the shooter should be punished for shooting an innocent.
     *
     * @param shooter the player who shot the gun
     * @param victim  the innocent player who was shot
     * @return a {@link PunishResult} to override the punishment, or null to continue checking
     */
    @Nullable
    PunishResult shouldPunish(@NotNull PlayerEntity shooter, @NotNull PlayerEntity victim);

    /**
     * Result of a punishment check.
     */
    final class PunishResult {
        private final PunishType type;
        private final Runnable customPunishment;

        private PunishResult(PunishType type, Runnable customPunishment) {
            this.type = type;
            this.customPunishment = customPunishment;
        }

        /**
         * Creates a result that cancels the punishment entirely (no gun drop, no penalty).
         */
        public static PunishResult cancel() {
            return new PunishResult(PunishType.CANCEL, null);
        }

        /**
         * Creates a result that allows normal punishment to proceed.
         */
        public static PunishResult allow() {
            return new PunishResult(PunishType.ALLOW, null);
        }

        /**
         * Creates a result with custom punishment logic.
         * The provided Runnable will be executed instead of the normal punishment.
         *
         * @param customPunishment the custom punishment logic to execute
         */
        public static PunishResult custom(@NotNull Runnable customPunishment) {
            return new PunishResult(PunishType.CUSTOM, customPunishment);
        }

        public PunishType getType() {
            return type;
        }

        /**
         * @return true if punishment should be applied (either normal or custom)
         */
        public boolean shouldPunish() {
            return type != PunishType.CANCEL;
        }

        /**
         * @return true if this result has custom punishment logic
         */
        public boolean hasCustomPunishment() {
            return type == PunishType.CUSTOM && customPunishment != null;
        }

        /**
         * Execute the custom punishment if present.
         */
        public void executeCustomPunishment() {
            if (customPunishment != null) {
                customPunishment.run();
            }
        }

        public enum PunishType {
            /** Cancel punishment entirely */
            CANCEL,
            /** Allow normal punishment to proceed */
            ALLOW,
            /** Apply custom punishment instead of normal */
            CUSTOM
        }
    }
}
