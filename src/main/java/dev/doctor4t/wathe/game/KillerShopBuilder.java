package dev.doctor4t.wathe.game;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
import dev.doctor4t.wathe.api.event.BuildShopEntries;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.util.ShopEntry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

/**
 * Builds the shop entries for killer faction players.
 * This class encapsulates all the logic for what items are available
 * in the killer shop and their prices.
 */
public class KillerShopBuilder {

    private KillerShopBuilder() {
        // Utility class - prevent instantiation
    }

    /**
     * Builds shop entries for a player.
     * If the player is not a killer, clears all entries to deny shop access.
     * If the player is a killer, adds all available shop items.
     *
     * @param player  the player to build shop for
     * @param context the shop context to populate
     */
    public static void buildShop(PlayerEntity player, BuildShopEntries.ShopContext context) {
        if (!GameWorldComponent.KEY.get(player.getWorld()).canUseKillerFeatures(player)) {
            return;
        }
        addWeapons(context);
        addPoisons(context);
        addTools(context);
    }

    private static void addWeapons(BuildShopEntries.ShopContext context) {
        context.addEntry(new ShopEntry(WatheItems.KNIFE.getDefaultStack(), 100, ShopEntry.Type.WEAPON));
        context.addEntry(new ShopEntry(WatheItems.REVOLVER.getDefaultStack(), 300, ShopEntry.Type.WEAPON));
        context.addEntry(new ShopEntry(WatheItems.GRENADE.getDefaultStack(), 350, ShopEntry.Type.WEAPON));

        // Psycho mode - special handling via custom onBuy
        context.addEntry(new ShopEntry(WatheItems.PSYCHO_MODE.getDefaultStack(), 300, ShopEntry.Type.WEAPON) {
            @Override
            public boolean onBuy(PlayerEntity buyPlayer) {
                return PlayerShopComponent.usePsychoMode(buyPlayer);
            }
        });
    }

    private static void addPoisons(BuildShopEntries.ShopContext context) {
        context.addEntry(new ShopEntry(WatheItems.POISON_VIAL.getDefaultStack(), 100, ShopEntry.Type.POISON));
        context.addEntry(new ShopEntry(WatheItems.SCORPION.getDefaultStack(), 50, ShopEntry.Type.POISON));
    }

    private static void addTools(BuildShopEntries.ShopContext context) {
        context.addEntry(new ShopEntry(WatheItems.FIRECRACKER.getDefaultStack(), 10, ShopEntry.Type.TOOL));
        context.addEntry(new ShopEntry(WatheItems.LOCKPICK.getDefaultStack(), 50, ShopEntry.Type.TOOL));
        context.addEntry(new ShopEntry(WatheItems.CROWBAR.getDefaultStack(), 25, ShopEntry.Type.TOOL));
        context.addEntry(new ShopEntry(WatheItems.BODY_BAG.getDefaultStack(), 100, ShopEntry.Type.TOOL));

        // Blackout - special handling via custom onBuy
        context.addEntry(new ShopEntry(WatheItems.BLACKOUT.getDefaultStack(), 200, ShopEntry.Type.TOOL) {
            @Override
            public boolean onBuy(PlayerEntity buyPlayer) {
                return PlayerShopComponent.useBlackout(buyPlayer);
            }
        });

        // Notes - sold in packs of 4
        context.addEntry(new ShopEntry(new ItemStack(WatheItems.NOTE, 4), 10, ShopEntry.Type.TOOL));
    }
}
