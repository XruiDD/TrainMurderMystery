package dev.doctor4t.wathe.api.event;

import dev.doctor4t.wathe.api.Faction;
import dev.doctor4t.wathe.api.WatheRoles;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.MapEnhancementsWorldComponent;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
import dev.doctor4t.wathe.cca.PlayerVeteranComponent;
import dev.doctor4t.wathe.client.WatheClient;
import dev.doctor4t.wathe.config.datapack.MapEnhancementsConfiguration.InteractionBlacklistConfig;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.index.WatheItems;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;

public class WatheEventHandlers {

    public static void register() {
        registerRoleAssignedHandler();
        registerBlockInteractionBlacklist();
    }

    /**
     * 注册方块交互黑名单处理器
     * 阻止玩家右键点击黑名单中的方块（仅在游戏进行中生效）
     */
    private static void registerBlockInteractionBlacklist() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            Block block = world.getBlockState(hitResult.getBlockPos()).getBlock();
            MapEnhancementsWorldComponent enhancements = MapEnhancementsWorldComponent.KEY.get(world);
            InteractionBlacklistConfig blacklist = enhancements.getInteractionBlacklistConfig();

            boolean isBlacklisted = blacklist.isBlacklisted(block);

            if (world.isClient && WatheClient.blockBlacklistDebugEnabled) {
                String blockId = net.minecraft.registry.Registries.BLOCK.getId(block).toString();
                player.sendMessage(net.minecraft.text.Text.literal(
                        "§e[Debug] §f点击方块: §b" + blockId + " §f| 黑名单拦截: " + (isBlacklisted ? "§c是" : "§a否")
                ));
            }

            if (isBlacklisted) {
                return ActionResult.FAIL;
            }

            return ActionResult.PASS;
        });
    }

    private static void registerRoleAssignedHandler() {
        RoleAssigned.EVENT.register((player, role) -> {
            if (role == null || player.getWorld().isClient()) return;

            // Give vigilante a revolver
            if (role == WatheRoles.VIGILANTE) {
                player.giveItemStack(new ItemStack(WatheItems.REVOLVER));
            }

            // Give veteran a knife with limited uses (2 stabs)
            if (role == WatheRoles.VETERAN) {
                PlayerVeteranComponent.KEY.get(player).initialize();
                player.giveItemStack(new ItemStack(WatheItems.KNIFE));
            }

            // Give killer faction starting money
            if (role.getFaction() == Faction.KILLER && player.getWorld() instanceof ServerWorld serverWorld) {
                GameWorldComponent gameComponent = GameWorldComponent.KEY.get(serverWorld);
                int totalPlayers = serverWorld.getPlayers().size();
                int killerCount = gameComponent.getAllKillerTeamPlayers().size();
                int killerRatio = gameComponent.getKillerDividend();
                int excessPlayers = Math.max(0, totalPlayers - (killerCount * killerRatio));
                int additionalMoneyPerExcess = 15;
                int dynamicStartingMoney = GameConstants.MONEY_START + (excessPlayers * additionalMoneyPerExcess);
                PlayerShopComponent.KEY.get(player).setBalance(dynamicStartingMoney);
            }
        });
    }
}
