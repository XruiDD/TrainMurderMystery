package dev.doctor4t.wathe.game;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
import dev.doctor4t.wathe.api.event.BuildShopEntries;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.util.ShopEntry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

import static dev.doctor4t.wathe.game.GameConstants.getInTicks;

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
        // 刀: 100, 库存1
        context.addEntry(new ShopEntry.Builder("knife", WatheItems.KNIFE.getDefaultStack(), 100, ShopEntry.Type.WEAPON)
            .stock(1)
            .build());

        // 枪: 300, 无限制
        context.addEntry(new ShopEntry.Builder("revolver", WatheItems.REVOLVER.getDefaultStack(), 300, ShopEntry.Type.WEAPON)
            .build());

        // 手雷: 350, 无限制
        context.addEntry(new ShopEntry.Builder("grenade", WatheItems.GRENADE.getDefaultStack(), 350, ShopEntry.Type.WEAPON)
            .build());

        // 疯魔模式: 350, 5分钟冷却
        context.addEntry(new ShopEntry.Builder("psycho_mode", WatheItems.PSYCHO_MODE.getDefaultStack(), 350, ShopEntry.Type.WEAPON)
            .cooldown(getInTicks(5, 0))
            .onBuy(PlayerShopComponent::usePsychoMode)
            .build());
    }

    private static void addPoisons(BuildShopEntries.ShopContext context) {
        // 毒药: 75, 无限制
        context.addEntry(new ShopEntry.Builder("poison_vial", WatheItems.POISON_VIAL.getDefaultStack(), 75, ShopEntry.Type.POISON)
            .build());

        // 蝎子: 75, 无限制
        context.addEntry(new ShopEntry.Builder("scorpion", WatheItems.SCORPION.getDefaultStack(), 75, ShopEntry.Type.POISON)
            .build());
    }

    private static void addTools(BuildShopEntries.ShopContext context) {
        // 便签: 10, 无限制 (4个一组)
        context.addEntry(new ShopEntry.Builder("note", new ItemStack(WatheItems.NOTE, 4), 10, ShopEntry.Type.TOOL)
            .build());

        // 开锁器: 50, 库存1
        context.addEntry(new ShopEntry.Builder("lockpick", WatheItems.LOCKPICK.getDefaultStack(), 50, ShopEntry.Type.TOOL)
            .stock(1)
            .build());

        // 撬棍: 25, 库存1
        context.addEntry(new ShopEntry.Builder("crowbar", WatheItems.CROWBAR.getDefaultStack(), 25, ShopEntry.Type.TOOL)
            .stock(1)
            .build());

        // 裹尸袋: 100, 无限制
        context.addEntry(new ShopEntry.Builder("body_bag", WatheItems.BODY_BAG.getDefaultStack(), 100, ShopEntry.Type.TOOL)
            .build());

        // 鞭炮: 25, 无限制
        context.addEntry(new ShopEntry.Builder("firecracker", WatheItems.FIRECRACKER.getDefaultStack(), 25, ShopEntry.Type.TOOL)
            .build());

        // 关灯: 300, 共享冷却在onBuy中处理
        context.addEntry(new ShopEntry.Builder("blackout", WatheItems.BLACKOUT.getDefaultStack(), 400, ShopEntry.Type.TOOL)
            .cooldown(getInTicks(5,0))
            .onBuy(PlayerShopComponent::useBlackout)
            .build());
    }
}
