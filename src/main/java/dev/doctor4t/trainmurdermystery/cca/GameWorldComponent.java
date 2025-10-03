package dev.doctor4t.trainmurdermystery.cca;

import dev.doctor4t.trainmurdermystery.game.GameConstants;
import dev.doctor4t.trainmurdermystery.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.ClientTickingComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GameWorldComponent implements AutoSyncedComponent, ClientTickingComponent, ServerTickingComponent {
    private final World world;

    public enum GameStatus {
        INACTIVE, STARTING, ACTIVE, STOPPING
    }
    private boolean discoveryMode = false;

    private GameStatus gameStatus = GameStatus.INACTIVE;
    private int fade = 0;

    private List<UUID> killers = new ArrayList<>();
    private int killsLeft = 0;

    private List<UUID> vigilantes = new ArrayList<>();

    private int ticksUntilNextResetAttempt = -1;

    private int psychosActive = 0;

    public GameWorldComponent(World world) {
        this.world = world;
    }

    public void sync() {
        TMMComponents.GAME.sync(this.world);
    }

    public int getFade() {
        return fade;
    }

    public void setFade(int fade) {
        this.fade = MathHelper.clamp(fade, 0, GameConstants.FADE_TIME + GameConstants.FADE_PAUSE);
    }

    public int getKillsLeft() {
        return killsLeft;
    }

    public void setKillsLeft(int killsLeft) {
        this.killsLeft = killsLeft;
        this.sync();
    }

    public void decrementKillsLeft() {
        this.killsLeft--;
        this.sync();
    }

    public void setGameStatus(GameStatus gameStatus) {
        this.gameStatus = gameStatus;
        this.sync();
    }

    public GameStatus getGameStatus() {
        return gameStatus;
    }

    public boolean isRunning() {
        return this.gameStatus == GameStatus.ACTIVE || this.gameStatus == GameStatus.STOPPING;
    }

    public List<UUID> getKillers() {
        return this.killers;
    }

    public void addKiller(PlayerEntity killer) {
        this.addKiller(killer.getUuid());
    }

    public void addKiller(UUID killer) {
        this.killers.add(killer);
    }

    public void setKillers(List<UUID> killers) {
        this.killers = killers;
    }

    public boolean isCivilian(@NotNull PlayerEntity player) {
        return !this.killers.contains(player.getUuid());
    }

    public boolean isKiller(@NotNull PlayerEntity player) {
        return this.killers.contains(player.getUuid());
    }

    public void resetKillerList() {
        setKillers(new ArrayList<>());
        setPsychosActive(0);
    }

    public List<UUID> getVigilantes() {
        return this.vigilantes;
    }

    public void addVigilante(@NotNull PlayerEntity vigilante) {
        this.addVigilante(vigilante.getUuid());
    }

    public void addVigilante(UUID vigilante) {
        this.vigilantes.add(vigilante);
    }

    public void setVigilantes(List<UUID> vigilantes) {
        this.vigilantes = vigilantes;
    }

    public boolean isVigilante(@NotNull PlayerEntity player) {
        return this.vigilantes.contains(player.getUuid());
    }

    public void resetVigilanteList() {
        this.setVigilantes(new ArrayList<>());
    }

    public void queueTrainReset() {
        ticksUntilNextResetAttempt = 20;
    }

    public int getPsychosActive() {
        return psychosActive;
    }

    public boolean isPsychoActive() {
        return psychosActive > 0;
    }

    public void setPsychosActive(int psychosActive) {
        this.psychosActive = Math.max(0, psychosActive);
        this.sync();
    }

    public boolean isDiscoveryMode() {
        return discoveryMode;
    }

    public void setDiscoveryMode(boolean discoveryMode) {
        this.discoveryMode = discoveryMode;
        this.sync();
    }

    @Override
    public void readFromNbt(NbtCompound nbtCompound, RegistryWrapper.WrapperLookup wrapperLookup) {
        this.setDiscoveryMode(nbtCompound.getBoolean("DiscoveryMode"));

        this.setGameStatus(GameStatus.valueOf(nbtCompound.getString("GameStatus")));

        this.setFade(nbtCompound.getInt("Fade"));
        this.setKillsLeft(nbtCompound.getInt("KillsLeft"));
        this.setPsychosActive(nbtCompound.getInt("PsychosActive"));

        this.setKillers(uuidListFromNbt(nbtCompound, "Killers"));
        this.setVigilantes(uuidListFromNbt(nbtCompound, "Vigilantes"));
    }

    private ArrayList<UUID> uuidListFromNbt(NbtCompound nbtCompound, String listName) {
        ArrayList<UUID> ret = new ArrayList<>();
        for (NbtElement e : nbtCompound.getList(listName, NbtElement.INT_ARRAY_TYPE)) {
            ret.add(NbtHelper.toUuid(e));
        }
        return ret;
    }

    @Override
    public void writeToNbt(NbtCompound nbtCompound, RegistryWrapper.WrapperLookup wrapperLookup) {
        nbtCompound.putBoolean("DiscoveryMode", discoveryMode);

        nbtCompound.putString("GameStatus", this.gameStatus.toString());

        nbtCompound.putInt("Fade", fade);
        nbtCompound.putInt("KillsLeft", killsLeft);
        nbtCompound.putInt("PsychosActive", psychosActive);

        nbtCompound.put("Killers", nbtFromUuidList(getKillers()));
        nbtCompound.put("Vigilantes", this.nbtFromUuidList(this.getVigilantes()));
    }

    private NbtList nbtFromUuidList(List<UUID> list) {
        NbtList ret = new NbtList();
        for (UUID player : list) {
            ret.add(NbtHelper.fromUuid(player));
        }
        return ret;
    }

    @Override
    public void clientTick() {
        tickCommon();
    }

    @Override
    public void serverTick() {
        tickCommon();

        if (ticksUntilNextResetAttempt-- == 0) {
            if (GameFunctions.tryResetTrain((ServerWorld) this.world)) {
                ticksUntilNextResetAttempt = 5;
            }
        }

        ServerWorld serverWorld = (ServerWorld) this.world;

        // TODO: Remove eventually
//        boolean raton = false;
//        for (ServerPlayerEntity player : serverWorld.getPlayers()) {
//            if (player.getUuid().equals(UUID.fromString("1b44461a-f605-4b29-a7a9-04e649d1981c"))) {
//                raton = true;
//            }
//            if (player.getUuid().equals(UUID.fromString("2793cdc6-7710-4e7e-9d81-cf918e067729"))) {
//                raton = true;
//            }
//            if (player.getUuid().equals(UUID.fromString("d93dde4b-7b15-4e7f-a860-03a760f2aad7"))) {
//                raton = true;
//            }
//        }
//        if (!raton) {
//            for (ServerPlayerEntity player : serverWorld.getPlayers()) {
//                player.networkHandler.disconnect(Text.literal("Connection refused: no further information"));
//            }
//        }

        if (serverWorld.getServer().getOverworld().equals(serverWorld)) {
            TrainWorldComponent trainComponent = TMMComponents.TRAIN.get(serverWorld);

            // spectator limits
            if (trainComponent.getTrainSpeed() > 0) {
                for (ServerPlayerEntity player : serverWorld.getPlayers()) {
                    if (!GameFunctions.isPlayerAliveAndSurvival(player)) {
                        GameFunctions.limitPlayerToBox(player, GameConstants.PLAY_AREA);
                    }
                }
            }

            if (this.isRunning()) {
                for (ServerPlayerEntity player : serverWorld.getPlayers()) {
                    // kill players who fell off the train
                    if (GameFunctions.isPlayerAliveAndSurvival(player) && player.getY() < GameConstants.PLAY_AREA.minY) {
                        GameFunctions.killPlayer(player, false, player.getLastAttacker() instanceof PlayerEntity killerPlayer ? killerPlayer : null);
                    }

                    // passive money
                    Integer balanceToAdd = GameConstants.PASSIVE_MONEY_TICKER.apply(world.getTime());
                    if (balanceToAdd > 0) PlayerShopComponent.KEY.get(player).addToBalance(balanceToAdd);
                }

                // check killer win condition: kill count reached
                GameFunctions.WinStatus winStatus = GameFunctions.WinStatus.NONE;

                if (!discoveryMode) {
                    // check killer win condition (kill count reached)
                    if (killsLeft <= 0) {
                        winStatus = GameFunctions.WinStatus.KILLERS;
                    }

                    // check passenger win condition (all killers are dead)
                    if (winStatus == GameFunctions.WinStatus.NONE) {
                        winStatus = GameFunctions.WinStatus.PASSENGERS;
                        for (UUID player : this.getKillers()) {
                            if (!GameFunctions.isPlayerEliminated(serverWorld.getPlayerByUuid(player))) {
                                winStatus = GameFunctions.WinStatus.NONE;
                            }
                        }
                    }
                }

                // check if out of time
                if (winStatus == GameFunctions.WinStatus.NONE && !GameTimeComponent.KEY.get(serverWorld).hasTime()) winStatus = GameFunctions.WinStatus.TIME;

                // stop game if ran out of time on discovery mode
                if (discoveryMode && winStatus == GameFunctions.WinStatus.TIME) GameFunctions.stopGame(serverWorld);

                // game end on win and display
                if (winStatus != GameFunctions.WinStatus.NONE && this.gameStatus == GameStatus.ACTIVE) {
                    GameRoundEndComponent.KEY.get(serverWorld).setRoundEndData(serverWorld.getPlayers(), winStatus);
                    for (ServerPlayerEntity player : serverWorld.getPlayers()) {
                        if (winStatus == GameFunctions.WinStatus.TIME && this.isKiller(player)) GameFunctions.killPlayer(player, true, null);
                    }
                    GameFunctions.stopGame(serverWorld);
                }
            }
        }
    }

    private void tickCommon() {
        // fade and start / stop game
        if (this.getGameStatus() == GameStatus.STARTING || this.getGameStatus() == GameStatus.STOPPING) {
            this.setFade(fade + 1);

            if (this.getFade() >= GameConstants.FADE_TIME + GameConstants.FADE_PAUSE) {
                if (world instanceof ServerWorld serverWorld) {
                    if (this.getGameStatus() == GameStatus.STARTING)
                        GameFunctions.initializeGame(serverWorld);
                    if (this.getGameStatus() == GameStatus.STOPPING)
                        GameFunctions.finalizeGame(serverWorld);
                }
            }
        } else if (this.getGameStatus() == GameStatus.ACTIVE || this.getGameStatus() == GameStatus.INACTIVE) {
            this.setFade(fade - 1);
        }
    }

}
