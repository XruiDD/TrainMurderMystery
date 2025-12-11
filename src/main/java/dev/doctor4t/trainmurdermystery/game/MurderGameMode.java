package dev.doctor4t.trainmurdermystery.game;

import dev.doctor4t.trainmurdermystery.api.GameMode;
import dev.doctor4t.trainmurdermystery.api.Role;
import dev.doctor4t.trainmurdermystery.api.TMMRoles;
import dev.doctor4t.trainmurdermystery.cca.*;
import dev.doctor4t.trainmurdermystery.event.CheckWinCondition;
import dev.doctor4t.trainmurdermystery.event.RoleAssigned;
import dev.doctor4t.trainmurdermystery.util.AnnounceWelcomePayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

public class MurderGameMode extends GameMode {
    public MurderGameMode(Identifier identifier) {
        super(identifier, 10, 6);
    }

    private static int assignRolesAndGetKillerCount(@NotNull ServerWorld world, @NotNull List<ServerPlayerEntity> players, GameWorldComponent gameComponent) {
        // NO_ROLE base role, replaced for selected killers and vigilantes
        for (ServerPlayerEntity player : players) {
            gameComponent.addRole(player, TMMRoles.NO_ROLE);
        }

        // select roles
        ScoreboardRoleSelectorComponent roleSelector = ScoreboardRoleSelectorComponent.KEY.get(world.getScoreboard());
        
        // Calculate killer count based on configuration
        int killerCount;
        int configuredKillerCount = gameComponent.getNextRoundKillerCount();
        
        if (configuredKillerCount > 0) {
            // Use exact configured count for next round
            killerCount = configuredKillerCount;
            // Reset the configured count for future rounds
            gameComponent.setNextRoundKillerCount(0);
        } else {
            // Use ratio-based calculation
            int ratio = gameComponent.getKillerPlayerRatio();
            killerCount = (int) Math.floor(players.size() / (float) ratio);
        }
        
        // Ensure at least 1 killer if there are enough players
        killerCount = Math.max(1, killerCount);
        roleSelector.assignForcedRoles(world, gameComponent, players);
        int total = roleSelector.assignKillers(world, gameComponent, players, killerCount);
        roleSelector.assignVigilantes(world, gameComponent, players, killerCount);
        roleSelector.assignNeutrals(world, gameComponent, players, killerCount);
        roleSelector.assignCivilians(world, gameComponent, players);
        for (ServerPlayerEntity player : players) {
            RoleAssigned.EVENT.invoker().assignModdedRole(player,gameComponent.getRole(player));
        }
        return total;
    }

    @Override
    public void initializeGame(ServerWorld serverWorld, GameWorldComponent gameWorldComponent, List<ServerPlayerEntity> players) {
        TrainWorldComponent.KEY.get(serverWorld).setTimeOfDay(TrainWorldComponent.TimeOfDay.NIGHT);

        int killerCount = assignRolesAndGetKillerCount(serverWorld, players, gameWorldComponent);

        for (ServerPlayerEntity player : players) {
            Role role = gameWorldComponent.getRole(player);
            String roleId = role != null ? role.identifier().toString() : TMMRoles.CIVILIAN.identifier().toString();
            ServerPlayNetworking.send(player, new AnnounceWelcomePayload(roleId, killerCount, players.size() - killerCount));
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
                GameRoundEndComponent.KEY.get(serverWorld).setRoundEndData(serverWorld.getPlayers(), neutralWinner);
            } else {
                GameRoundEndComponent.KEY.get(serverWorld).setRoundEndData(serverWorld.getPlayers(), winStatus);
            }

            GameFunctions.stopGame(serverWorld);
        }
    }
}
