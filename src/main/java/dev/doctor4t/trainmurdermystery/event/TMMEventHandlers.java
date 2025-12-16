package dev.doctor4t.trainmurdermystery.event;

import dev.doctor4t.trainmurdermystery.api.Faction;
import dev.doctor4t.trainmurdermystery.api.TMMRoles;
import dev.doctor4t.trainmurdermystery.cca.GameWorldComponent;
import dev.doctor4t.trainmurdermystery.cca.PlayerShopComponent;
import dev.doctor4t.trainmurdermystery.game.GameConstants;
import dev.doctor4t.trainmurdermystery.game.KillerShopBuilder;
import dev.doctor4t.trainmurdermystery.index.TMMItems;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;

public class TMMEventHandlers {

    public static void register() {
        registerRoleAssignedHandler();
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
}
