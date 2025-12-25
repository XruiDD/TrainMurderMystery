package dev.doctor4t.wathe.api.event;

import dev.doctor4t.wathe.util.ShopEntry;
import net.fabricmc.fabric.api.event.Event;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

import static net.fabricmc.fabric.api.event.EventFactory.createArrayBacked;

/**
 * Events related to shop purchases.
 * These events are fired on the SERVER side only.
 */
public final class ShopPurchase {

    private ShopPurchase() {
    }

    /**
     * Called BEFORE a purchase attempt is processed.
     * Listeners can cancel the purchase or modify the effective price.
     * <p>
     * Return values:
     * <ul>
     *   <li>{@code PurchaseResult.allow()}: Allow the purchase at normal price</li>
     *   <li>{@code PurchaseResult.allow(newPrice)}: Allow with modified price</li>
     *   <li>{@code PurchaseResult.deny()}: Cancel the purchase silently</li>
     *   <li>{@code PurchaseResult.deny(reason)}: Cancel with a message to player</li>
     *   <li>{@code null}: Defer to next listener or default behavior</li>
     * </ul>
     */
    public static final Event<Before> BEFORE = createArrayBacked(Before.class, listeners -> (player, entry, index) -> {
        for (Before listener : listeners) {
            PurchaseResult result = listener.beforePurchase(player, entry, index);
            if (result != null) {
                return result;
            }
        }
        return null;
    });

    /**
     * Called AFTER a successful purchase.
     * Cannot cancel the purchase - use BEFORE event for that.
     * Useful for tracking, achievements, or side effects.
     */
    public static final Event<After> AFTER = createArrayBacked(After.class, listeners -> (player, entry, index, pricePaid) -> {
        for (After listener : listeners) {
            listener.afterPurchase(player, entry, index, pricePaid);
        }
    });

    @FunctionalInterface
    public interface Before {
        /**
         * Called before a purchase is processed.
         *
         * @param player the player attempting to purchase
         * @param entry  the shop entry being purchased
         * @param index  the index of the entry in the shop
         * @return {@code PurchaseResult} to override, or {@code null} to defer
         */
        @Nullable
        PurchaseResult beforePurchase(ServerPlayerEntity player, ShopEntry entry, int index);
    }

    @FunctionalInterface
    public interface After {
        /**
         * Called after a successful purchase.
         *
         * @param player    the player who made the purchase
         * @param entry     the shop entry that was purchased
         * @param index     the index of the entry in the shop
         * @param pricePaid the actual price paid (may differ from entry.price() if modified)
         */
        void afterPurchase(ServerPlayerEntity player, ShopEntry entry, int index, int pricePaid);
    }

    /**
     * Result of a purchase validation.
     */
    public record PurchaseResult(boolean allowed, int modifiedPrice, @Nullable String denyReason) {

        /**
         * Allow the purchase at the normal price.
         */
        public static PurchaseResult allow() {
            return new PurchaseResult(true, -1, null);
        }

        /**
         * Allow the purchase at a modified price.
         *
         * @param newPrice the new price (0 = free)
         */
        public static PurchaseResult allow(int newPrice) {
            return new PurchaseResult(true, newPrice, null);
        }

        /**
         * Deny the purchase silently.
         */
        public static PurchaseResult deny() {
            return new PurchaseResult(false, -1, null);
        }

        /**
         * Deny the purchase with a reason shown to the player.
         *
         * @param reason the reason to show
         */
        public static PurchaseResult deny(String reason) {
            return new PurchaseResult(false, -1, reason);
        }

        /**
         * @return true if a modified price was specified
         */
        public boolean hasModifiedPrice() {
            return modifiedPrice >= 0;
        }
    }
}
