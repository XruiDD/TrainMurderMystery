package dev.doctor4t.trainmurdermystery.game;

import dev.doctor4t.trainmurdermystery.cca.TrainMurderMysteryComponents;
import dev.doctor4t.trainmurdermystery.cca.WorldGameComponent;
import dev.doctor4t.trainmurdermystery.index.TrainMurderMysteryItems;
import net.minecraft.block.Blocks;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.world.GameMode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class GameLoop {
    public static void tick(ServerWorld serverWorld) {
        WorldGameComponent game = TrainMurderMysteryComponents.GAME.get(serverWorld);
        if (game.isRunning()) {
            // check hitman win condition (all targets are dead)
            WinStatus winStatus = WinStatus.HITMEN;
            for (ServerPlayerEntity player : game.getTargets()) {
                if (!isPlayerEliminated(serverWorld, player)) {
                    winStatus = WinStatus.NONE;
                }
            }

            // check passenger win condition (all hitmen are dead)
            if (winStatus == WinStatus.NONE) {
                winStatus = WinStatus.PASSENGERS;
                for (ServerPlayerEntity player : game.getHitmen()) {
                    if (!isPlayerEliminated(serverWorld, player)) {
                        winStatus = WinStatus.NONE;
                    }
                }
            }

            // win display
            if (winStatus != WinStatus.NONE) {
                for (ServerPlayerEntity player : serverWorld.getPlayers()) {
                    player.sendMessage(Text.translatable("game.win." + winStatus.name().toLowerCase(Locale.ROOT)), true);
                    System.out.println("game.win." + winStatus.name().toLowerCase(Locale.ROOT));
                }
                game.setRunning(false);
            }
        }
    }

    public static void startGame(ServerWorld world) {
        TrainMurderMysteryComponents.TRAIN.get(world).setTrainSpeed(130);
        WorldGameComponent gameComponent = TrainMurderMysteryComponents.GAME.get(world);

        List<ServerPlayerEntity> playerPool = new ArrayList<>(world.getPlayers().stream().filter(serverPlayerEntity -> !serverPlayerEntity.isInCreativeMode() && !serverPlayerEntity.isSpectator()).toList());

        // limit the game to 14 players, put players 15 to n in spectator mode
        Collections.shuffle(playerPool);
        while (playerPool.size() > 14) {
            playerPool.getFirst().changeGameMode(GameMode.SPECTATOR);
            playerPool.removeFirst();
        }

        List<ServerPlayerEntity> rolePlayerPool = new ArrayList<>(playerPool);

        // clear items, clear previous game data
        for (ServerPlayerEntity serverPlayerEntity : rolePlayerPool) {
            serverPlayerEntity.getInventory().clear();
        }
        gameComponent.resetLists();

        // select hitmen
        int hitmanCount = (int) Math.floor(rolePlayerPool.size() * .2f);
        Collections.shuffle(rolePlayerPool);
        for (int i = 0; i < hitmanCount; i++) {
            ServerPlayerEntity player = rolePlayerPool.getFirst();
            rolePlayerPool.removeFirst();
            player.giveItemStack(new ItemStack(TrainMurderMysteryItems.KNIFE));
            gameComponent.addHitman(player);
        }

        // select detectives
        int detectiveCount = hitmanCount;
        Collections.shuffle(rolePlayerPool);
        for (int i = 0; i < detectiveCount; i++) {
            ServerPlayerEntity player = rolePlayerPool.getFirst();
            rolePlayerPool.removeFirst();
            player.giveItemStack(new ItemStack(TrainMurderMysteryItems.REVOLVER));
            gameComponent.addDetective(player);
        }

        // select targets
        int targetCount = rolePlayerPool.size() / 2;
        Collections.shuffle(rolePlayerPool);
        for (int i = 0; i < targetCount; i++) {
            ServerPlayerEntity player = rolePlayerPool.getFirst();
            rolePlayerPool.removeFirst();
            player.giveItemStack(new ItemStack(Blocks.TARGET.asItem()));
            gameComponent.addTarget(player);
        }

        // select rooms
        Collections.shuffle(playerPool);
        for (int i = 0; i < playerPool.size(); i++) {
            ItemStack itemStack = new ItemStack(TrainMurderMysteryItems.ROOM_KEY);
            int roomNumber = (int) Math.floor((double) (i + 2) / 2);
            itemStack.apply(DataComponentTypes.LORE, LoreComponent.DEFAULT, component -> new LoreComponent(Text.literal("Room "+ roomNumber).getWithStyle(Style.EMPTY.withItalic(false).withColor(0xFF8C00))));
            playerPool.get(i).giveItemStack(itemStack);
        }

        gameComponent.setRunning(true);
    }

    private static boolean isPlayerEliminated(ServerWorld world, ServerPlayerEntity player) {
        return player == null || !player.isAlive() || player.isCreative() || player.isSpectator();
    }

    public enum WinStatus {
        NONE, HITMEN, PASSENGERS
    }
}
