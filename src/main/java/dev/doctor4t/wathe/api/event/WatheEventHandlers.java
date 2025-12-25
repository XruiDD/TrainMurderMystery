package dev.doctor4t.wathe.api.event;

import dev.doctor4t.wathe.api.Faction;
import dev.doctor4t.wathe.api.WatheRoles;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.index.WatheItems;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;

public class WatheEventHandlers {

    public static void register() {
        registerRoleAssignedHandler();
    }

    private static void registerRoleAssignedHandler() {
        RoleAssigned.EVENT.register((player, role) -> {
            if (role == null || player.getWorld().isClient()) return;

            // Give vigilante a revolver
            if (role == WatheRoles.VIGILANTE) {
                player.giveItemStack(new ItemStack(WatheItems.REVOLVER));
            }

            // Give killer faction starting money
            if (role.getFaction() == Faction.KILLER && player.getWorld() instanceof ServerWorld serverWorld) {
                GameWorldComponent gameComponent = GameWorldComponent.KEY.get(serverWorld);
                int totalPlayers = serverWorld.getPlayers().size();
                int killerCount = gameComponent.getAllKillerTeamPlayers().size();
                int killerRatio = gameComponent.getKillerDividend();
                int excessPlayers = Math.max(0, totalPlayers - (killerCount * killerRatio));
                int additionalMoneyPerExcess = 20;
                int dynamicStartingMoney = GameConstants.MONEY_START + (excessPlayers * additionalMoneyPerExcess);
                PlayerShopComponent.KEY.get(player).setBalance(dynamicStartingMoney);
            }
        });
    }
}
