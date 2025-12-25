package dev.doctor4t.wathe.api.event;

import dev.doctor4t.wathe.game.KillerShopBuilder;
import dev.doctor4t.wathe.util.ShopEntry;
import net.fabricmc.fabric.api.event.Event;
import net.minecraft.entity.player.PlayerEntity;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static net.fabricmc.fabric.api.event.EventFactory.createArrayBacked;

/**
 * Event for building the list of shop entries for a specific player.
 * This event is fired on BOTH client and server sides.
 * <p>
 * Listeners can:
 * <ul>
 *   <li>Add new entries to the shop</li>
 *   <li>Remove existing entries</li>
 *   <li>Replace the entire shop contents</li>
 *   <li>Modify prices of existing entries</li>
 * </ul>
 * <p>
 * The final list of entries after all listeners have been called
 * will be used as the shop contents for this player.
 */
public interface BuildShopEntries {

    /**
     * Called when building shop entries for a player.
     * All listeners are called in registration order.
     * Each listener receives the current state of the shop context
     * and can modify it.
     */
    Event<BuildShopEntries> EVENT = createArrayBacked(BuildShopEntries.class, listeners -> (player, context) -> {
        KillerShopBuilder.buildShop(player, context);
        for (BuildShopEntries listener : listeners) {
            listener.buildEntries(player, context);
        }
    });

    /**
     * Modify the shop entries for a player.
     *
     * @param player  the player the shop is being built for
     * @param context the shop context containing entries to modify
     */
    void buildEntries(PlayerEntity player, ShopContext context);

    /**
     * Context object for building shop entries.
     * Provides methods to add, remove, and modify shop entries.
     */
    class ShopContext {
        private final List<ShopEntry> entries;

        public ShopContext(@NotNull List<ShopEntry> defaultEntries) {
            this.entries = new ArrayList<>(defaultEntries);
        }

        /**
         * @return the current list of shop entries (mutable)
         */
        public List<ShopEntry> getEntries() {
            return entries;
        }

        /**
         * Add an entry to the end of the shop.
         *
         * @param entry the entry to add
         */
        public void addEntry(ShopEntry entry) {
            this.entries.add(entry);
        }

        /**
         * Add an entry at a specific index.
         *
         * @param index the index to insert at
         * @param entry the entry to add
         */
        public void addEntry(int index, ShopEntry entry) {
            this.entries.add(index, entry);
        }

        /**
         * Remove an entry at a specific index.
         *
         * @param index the index to remove
         * @return the removed entry
         */
        public ShopEntry removeEntry(int index) {
            return this.entries.remove(index);
        }

        /**
         * Clear all entries from the shop.
         * Useful for denying shop access or completely replacing shop contents.
         */
        public void clearEntries() {
            this.entries.clear();
        }

        /**
         * @return the number of entries in the shop
         */
        public int size() {
            return entries.size();
        }

        /**
         * Get an entry at a specific index.
         *
         * @param index the index
         * @return the entry at that index
         */
        public ShopEntry getEntry(int index) {
            return entries.get(index);
        }

        /**
         * Replace an entry at a specific index.
         *
         * @param index the index to replace
         * @param entry the new entry
         * @return the old entry
         */
        public ShopEntry setEntry(int index, ShopEntry entry) {
            return entries.set(index, entry);
        }
    }
}
