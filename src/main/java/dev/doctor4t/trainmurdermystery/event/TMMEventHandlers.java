package dev.doctor4t.trainmurdermystery.event;

import dev.doctor4t.trainmurdermystery.api.Faction;
import dev.doctor4t.trainmurdermystery.api.TMMRoles;
import dev.doctor4t.trainmurdermystery.cca.GameWorldComponent;
import dev.doctor4t.trainmurdermystery.cca.PlayerShopComponent;
import dev.doctor4t.trainmurdermystery.game.GameConstants;
import dev.doctor4t.trainmurdermystery.index.TMMItems;
import dev.doctor4t.trainmurdermystery.util.ShopEntry;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;

public class TMMEventHandlers {

    public static void register() {
        registerRoleAssignedHandler();
        registerShopHandlers();
    }

    private static void registerRoleAssignedHandler() {
        RoleAssigned.EVENT.register((player, role) -> {
            if (role == null || player.getWorld().isClient()) return;

            // Give vigilante a revolver
            if (role == TMMRoles.VIGILANTE) {
                player.giveItemStack(new ItemStack(TMMItems.REVOLVER));
            }

            // Give killer faction starting money
            if (role.getFaction() == Faction.KILLER && player.getWorld() instanceof ServerWorld serverWorld) {
                GameWorldComponent gameComponent = GameWorldComponent.KEY.get(serverWorld);
                int totalPlayers = serverWorld.getPlayers().size();
                int killerCount = gameComponent.getAllKillerTeamPlayers().size();
                int killerRatio = gameComponent.getKillerPlayerRatio();
                int excessPlayers = Math.max(0, totalPlayers - (killerCount * killerRatio));
                int additionalMoneyPerExcess = 20;
                int dynamicStartingMoney = GameConstants.MONEY_START + (excessPlayers * additionalMoneyPerExcess);
                PlayerShopComponent.KEY.get(player).setBalance(dynamicStartingMoney);
            }
        });
    }

    private static void registerShopHandlers() {
        // Register killer shop - add entries only if player can use killer features
        BuildShopEntries.EVENT.register((player, context) -> {
            if (!GameWorldComponent.KEY.get(player.getWorld()).canUseKillerFeatures(player)) {
                // Not a killer - clear all entries to deny shop access
                context.clearEntries();
                return;
            }

            // Killer player - add all shop items
            context.addEntry(new ShopEntry(TMMItems.KNIFE.getDefaultStack(), 100, ShopEntry.Type.WEAPON));
            context.addEntry(new ShopEntry(TMMItems.REVOLVER.getDefaultStack(), 300, ShopEntry.Type.WEAPON));
            context.addEntry(new ShopEntry(TMMItems.GRENADE.getDefaultStack(), 350, ShopEntry.Type.WEAPON));
            context.addEntry(new ShopEntry(TMMItems.PSYCHO_MODE.getDefaultStack(), 300, ShopEntry.Type.WEAPON) {
                @Override
                public boolean onBuy(net.minecraft.entity.player.PlayerEntity buyPlayer) {
                    return PlayerShopComponent.usePsychoMode(buyPlayer);
                }
            });
            context.addEntry(new ShopEntry(TMMItems.POISON_VIAL.getDefaultStack(), 100, ShopEntry.Type.POISON));
            context.addEntry(new ShopEntry(TMMItems.SCORPION.getDefaultStack(), 50, ShopEntry.Type.POISON));
            context.addEntry(new ShopEntry(TMMItems.FIRECRACKER.getDefaultStack(), 10, ShopEntry.Type.TOOL));
            context.addEntry(new ShopEntry(TMMItems.LOCKPICK.getDefaultStack(), 50, ShopEntry.Type.TOOL));
            context.addEntry(new ShopEntry(TMMItems.CROWBAR.getDefaultStack(), 25, ShopEntry.Type.TOOL));
            context.addEntry(new ShopEntry(TMMItems.BODY_BAG.getDefaultStack(), 100, ShopEntry.Type.TOOL));
            context.addEntry(new ShopEntry(TMMItems.BLACKOUT.getDefaultStack(), 200, ShopEntry.Type.TOOL) {
                @Override
                public boolean onBuy(net.minecraft.entity.player.PlayerEntity buyPlayer) {
                    return PlayerShopComponent.useBlackout(buyPlayer);
                }
            });
            context.addEntry(new ShopEntry(new net.minecraft.item.ItemStack(TMMItems.NOTE, 4), 10, ShopEntry.Type.TOOL));
        });
    }
}
