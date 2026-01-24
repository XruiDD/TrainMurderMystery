package dev.doctor4t.wathe.game.gamemode;

import dev.doctor4t.wathe.api.GameMode;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.WatheRoles;
import dev.doctor4t.wathe.cca.*;
import dev.doctor4t.wathe.api.event.CheckWinCondition;
import dev.doctor4t.wathe.api.event.RoleAssigned;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.util.AnnounceWelcomePayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

public class MurderGameMode extends GameMode {
    public MurderGameMode(Identifier identifier) {
        super(identifier, 15, 6);
    }

    private static int assignRolesAndGetKillerCount(@NotNull ServerWorld world, @NotNull List<ServerPlayerEntity> players, GameWorldComponent gameComponent) {
        // NO_ROLE base role, replaced for selected killers and vigilantes
        for (ServerPlayerEntity player : players) {
            gameComponent.addRole(player, WatheRoles.NO_ROLE);
        }

        // select roles
        ScoreboardRoleSelectorComponent roleSelector = ScoreboardRoleSelectorComponent.KEY.get(world.getScoreboard());
        roleSelector.assignForcedRoles(world, gameComponent, players);
        int total = roleSelector.assignKillers(world, gameComponent, players, (int) Math.floor((double) players.size() / gameComponent.getKillerDividend()));
        roleSelector.assignVigilantes(world, gameComponent, players,  (int) Math.floor((double) players.size() / gameComponent.getVigilanteDividend()));
        roleSelector.assignNeutrals(world, gameComponent, players, (int) Math.floor((double) players.size() / gameComponent.getNeutralDividend()));
        roleSelector.assignCivilians(world, gameComponent, players);
        for (ServerPlayerEntity player : players) {
            RoleAssigned.EVENT.invoker().assignRole(player, gameComponent.getRole(player));
        }
        return total;
    }

    @Override
    public void initializeGame(ServerWorld serverWorld, GameWorldComponent gameWorldComponent, List<ServerPlayerEntity> players) {
        int killerCount = assignRolesAndGetKillerCount(serverWorld, players, gameWorldComponent);

        for (ServerPlayerEntity player : players) {
            Role role = gameWorldComponent.getRole(player);
            String roleId = role != null ? role.identifier().toString() : WatheRoles.CIVILIAN.identifier().toString();
            ServerPlayNetworking.send(player, new AnnounceWelcomePayload(roleId, killerCount, players.size() - killerCount));
            player.getItemCooldownManager().set(WatheItems.REVOLVER,
                    GameConstants.getInTicks(1, 0));
            player.getItemCooldownManager().set(WatheItems.KNIFE,
                    GameConstants.getInTicks(0, 45));
        }
    }

    @Override
    public void tickServerGameLoop(ServerWorld serverWorld, GameWorldComponent gameWorldComponent) {
        GameFunctions.WinStatus winStatus = GameFunctions.WinStatus.NONE;
        ServerPlayerEntity neutralWinner = null;

        // check if out of time
        if (!GameTimeComponent.KEY.get(serverWorld).hasTime())
            winStatus = GameFunctions.WinStatus.TIME;

        boolean civilianAlive = false;
        for (ServerPlayerEntity player : serverWorld.getPlayers()) {
            // passive money
            if (gameWorldComponent.canUseKillerFeatures(player)) {
                Integer balanceToAdd = GameConstants.PASSIVE_MONEY_TICKER.apply(serverWorld.getTime());
                if (balanceToAdd > 0) PlayerShopComponent.KEY.get(player).addToBalance(balanceToAdd);
            }

            // check if some civilians are still alive
            if (gameWorldComponent.isInnocent(player) && !GameFunctions.isPlayerEliminated(player)) {
                civilianAlive = true;
            }
        }

        // check killer win condition (killed all civilians)
        if (winStatus == GameFunctions.WinStatus.NONE && !civilianAlive) {
            winStatus = GameFunctions.WinStatus.KILLERS;
        }

        // check passenger win condition (all killers are dead)
        if (winStatus == GameFunctions.WinStatus.NONE) {
            winStatus = GameFunctions.WinStatus.PASSENGERS;
            for (UUID player : gameWorldComponent.getAllKillerTeamPlayers()) {
                if (!GameFunctions.isPlayerEliminated(serverWorld.getPlayerByUuid(player))) {
                    winStatus = GameFunctions.WinStatus.NONE;
                    break;
                }
            }
        }

        // allow mods to override win conditions (neutral wins, block wins, etc.)
        CheckWinCondition.WinResult eventResult = CheckWinCondition.EVENT.invoker()
                .checkWin(serverWorld, gameWorldComponent, winStatus);
        if (eventResult != null) {
            winStatus = eventResult.status();
            neutralWinner = eventResult.winner();
        }

        // game end on win and display
        if (winStatus != GameFunctions.WinStatus.NONE && gameWorldComponent.getGameStatus() == GameWorldComponent.GameStatus.ACTIVE) {
            if (winStatus == GameFunctions.WinStatus.NEUTRAL && neutralWinner != null) {
                // use single winner method for neutral wins
                GameRoundEndComponent.KEY.get(serverWorld).setRoundEndData(serverWorld, neutralWinner.getUuid());
            } else {
                GameRoundEndComponent.KEY.get(serverWorld).setRoundEndData(serverWorld, winStatus);
            }

            GameFunctions.stopGame(serverWorld);
        }
    }
}
