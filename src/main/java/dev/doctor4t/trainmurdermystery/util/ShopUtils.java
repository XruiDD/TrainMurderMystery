package dev.doctor4t.trainmurdermystery.util;

import dev.doctor4t.trainmurdermystery.event.BuildShopEntries;
import dev.doctor4t.trainmurdermystery.game.GameConstants;
import net.minecraft.entity.player.PlayerEntity;

import java.util.List;

/**
 * Utility class for shop-related operations.
 */
public class ShopUtils {

    private ShopUtils() {
        // Utility class - prevent instantiation
    }

    /**
     * Gets the list of shop entries available to a specific player.
     * This triggers the BuildShopEntries event and returns the resulting entries.
     * <p>
     * If the returned list is empty, it means the player has no access to the shop.
     *
     * @param player the player to get shop entries for
     * @return the list of shop entries available to this player (empty if no access)
     */
    public static List<ShopEntry> getShopEntriesForPlayer(PlayerEntity player) {
        BuildShopEntries.ShopContext context = new BuildShopEntries.ShopContext(GameConstants.SHOP_ENTRIES);
        BuildShopEntries.EVENT.invoker().buildEntries(player, context);
        return context.getEntries();
    }

    /**
     * Checks if a player has access to the shop.
     *
     * @param player the player to check
     * @return true if the player can access the shop, false otherwise
     */
    public static boolean canAccessShop(PlayerEntity player) {
        return !getShopEntriesForPlayer(player).isEmpty();
    }
}
