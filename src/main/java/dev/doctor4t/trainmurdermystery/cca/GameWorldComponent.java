package dev.doctor4t.trainmurdermystery.cca;

import dev.doctor4t.trainmurdermystery.TMM;
import dev.doctor4t.trainmurdermystery.api.GameMode;
import dev.doctor4t.trainmurdermystery.api.Role;
import dev.doctor4t.trainmurdermystery.api.TMMGameModes;
import dev.doctor4t.trainmurdermystery.api.TMMRoles;
import dev.doctor4t.trainmurdermystery.config.TMMServerConfig;
import dev.doctor4t.trainmurdermystery.config.TMMServerConfig.ShootInnocentPunishment;
import dev.doctor4t.trainmurdermystery.game.GameConstants;
import dev.doctor4t.trainmurdermystery.game.GameFunctions;
import com.mojang.authlib.GameProfile;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.ClientTickingComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

public class GameWorldComponent implements AutoSyncedComponent, ServerTickingComponent, ClientTickingComponent {
    public static final ComponentKey<GameWorldComponent> KEY = ComponentRegistry.getOrCreate(TMM.id("game"), GameWorldComponent.class);
    private final World world;




    public enum GameStatus {
        INACTIVE, STARTING, ACTIVE, STOPPING
    }

    private GameMode gameMode = TMMGameModes.MURDER;

    private boolean bound = true;

    private GameStatus gameStatus = GameStatus.INACTIVE;
    private int fade = 0;

    private final HashMap<UUID, Role> roles = new HashMap<>();
    private final HashMap<UUID, GameProfile> gameProfiles = new HashMap<>();
    private final HashSet<UUID> deadPlayers = new HashSet<>();

    private int ticksUntilNextResetAttempt = -1;

    private int psychosActive = 0;

    private UUID looseEndWinner;

    private float backfireChance = 0f;
    private int nextRoundKillerCount = 0; // 0 = use ratio, >0 = exact count for next round
    private int killerPlayerRatio = 6; // 1 killer per X players

    // 射杀无辜惩罚
    private ShootInnocentPunishment shootInnocentPunishment;
    private final HashSet<UUID> preventGunPickup = new HashSet<>();

    public GameWorldComponent(World world) {
        this.world = world;
        // 应用服务器配置默认值
        TMMServerConfig config = TMMServerConfig.HANDLER.instance();
        this.backfireChance = config.backfireChance;
        this.killerPlayerRatio = config.killerRatio;
        this.nextRoundKillerCount = config.killerCount;
        this.bound = config.bound;
        this.shootInnocentPunishment = config.shootInnocentPunishment;
    }

    public int getNextRoundKillerCount() {
        return nextRoundKillerCount;
    }

    public void setNextRoundKillerCount(int nextRoundKillerCount) {
        this.nextRoundKillerCount = Math.max(0, nextRoundKillerCount);
        this.sync();
    }

    public int getKillerPlayerRatio() {
        return killerPlayerRatio;
    }

    public void setKillerPlayerRatio(int killerPlayerRatio) {
        this.killerPlayerRatio = Math.max(1, killerPlayerRatio);
        this.sync();
    }

    public void sync() {
        GameWorldComponent.KEY.sync(this.world);
    }

    public boolean isBound() {
        return bound;
    }

    public void setBound(boolean bound) {
        this.bound = bound;
        this.sync();
    }

    public int getFade() {
        return fade;
    }

    public void setFade(int fade) {
        this.fade = MathHelper.clamp(fade, 0, GameConstants.FADE_TIME + GameConstants.FADE_PAUSE);
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

    public void addRole(PlayerEntity player, Role role) {
        this.roles.put(player.getUuid(), role);
        this.gameProfiles.put(player.getUuid(), player.getGameProfile());
    }

    public void addRole(UUID player, Role role) {
        this.roles.put(player, role);
    }

    public void resetRole(Role role) {
        roles.entrySet().removeIf(entry -> entry.getValue() == role);
    }

    public void setRoles(List<UUID> players, Role role) {
        resetRole(role);

        for (UUID player : players) {
            addRole(player, role);
        }
    }

    public HashMap<UUID, Role> getRoles() {
        return roles;
    }

    public @Nullable Role getRole(PlayerEntity player) {
        return getRole(player.getUuid());
    }

    public @Nullable Role getRole(UUID uuid) {
        return roles.get(uuid);
    }

    public List<UUID> getAllKillerTeamPlayers() {
        List<UUID> ret = new ArrayList<>();
        roles.forEach((uuid, playerRole) -> {
            if (playerRole.canUseKiller()) {
                ret.add(uuid);
            }
        });

        return ret;
    }
    public List<UUID> getAllWithRole(Role role) {
        List<UUID> ret = new ArrayList<>();
        roles.forEach((uuid, playerRole) -> {
            if (playerRole == role) {
                ret.add(uuid);
            }
        });

        return ret;
    }

    public boolean isRole(@NotNull PlayerEntity player, Role role) {
        return isRole(player.getUuid(), role);
    }

    public boolean isRole(@NotNull UUID uuid, Role role) {
        return this.roles.get(uuid) == role;
    }

    public boolean hasAnyRole(@NotNull PlayerEntity player) {
        return hasAnyRole(player.getUuid());
    }

    public boolean hasAnyRole(@NotNull UUID uuid) {
        return this.roles.containsKey(uuid) && this.roles.get(uuid) != TMMRoles.NO_ROLE;
    }

    public boolean canUseKillerFeatures(@NotNull PlayerEntity player) {
        return getRole(player) != null && getRole(player).canUseKiller();
    }
    public boolean isInnocent(@NotNull PlayerEntity player) {
        return getRole(player) != null && getRole(player).isInnocent();
    }

    public void clearRoleMap() {
        this.roles.clear();
        this.gameProfiles.clear();
        this.deadPlayers.clear();
        setPsychosActive(0);
    }

    public void markPlayerDead(UUID uuid) {
        this.deadPlayers.add(uuid);
    }

    public boolean isPlayerDead(UUID uuid) {
        return this.deadPlayers.contains(uuid);
    }

    public HashMap<UUID, GameProfile> getGameProfiles() {
        return this.gameProfiles;
    }

    public void queueTrainReset() {
        ticksUntilNextResetAttempt = 10;
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

    public GameMode getGameMode() {
        return gameMode;
    }

    public void setGameMode(GameMode gameMode) {
        this.gameMode = gameMode;
        this.sync();
    }

    public UUID getLooseEndWinner() {
        return this.looseEndWinner;
    }

    public void setLooseEndWinner(UUID looseEndWinner) {
        this.looseEndWinner = looseEndWinner;
        this.sync();
    }


    public float getBackfireChance() {
        return backfireChance;
    }

    public void setBackfireChance(float backfireChance) {
        this.backfireChance = backfireChance;
        this.sync();
    }

    public ShootInnocentPunishment getShootInnocentPunishment() {
        return shootInnocentPunishment;
    }

    public void setShootInnocentPunishment(ShootInnocentPunishment punishment) {
        this.shootInnocentPunishment = punishment;
        this.sync();
    }

    public void addToPreventGunPickup(PlayerEntity player) {
        this.preventGunPickup.add(player.getUuid());
    }

    public boolean isPreventedFromGunPickup(PlayerEntity player) {
        return this.preventGunPickup.contains(player.getUuid());
    }

    public void clearPreventGunPickup() {
        this.preventGunPickup.clear();
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound nbtCompound, RegistryWrapper.WrapperLookup wrapperLookup) {

        this.gameMode = TMMGameModes.GAME_MODES.get(Identifier.of(nbtCompound.getString("GameMode")));
        this.gameStatus = GameStatus.valueOf(nbtCompound.getString("GameStatus"));

        this.fade = nbtCompound.getInt("Fade");
        this.psychosActive = nbtCompound.getInt("PsychosActive");

        this.backfireChance = nbtCompound.getFloat("BackfireChance");
        this.nextRoundKillerCount = nbtCompound.getInt("NextRoundKillerCount");
        this.killerPlayerRatio = nbtCompound.getInt("KillerPlayerRatio") > 0 ? nbtCompound.getInt("KillerPlayerRatio") : 6;

        // Read shoot innocent punishment
        if (nbtCompound.contains("ShootInnocentPunishment")) {
            try {
                this.shootInnocentPunishment = ShootInnocentPunishment.valueOf(nbtCompound.getString("ShootInnocentPunishment"));
            } catch (IllegalArgumentException e) {
                this.shootInnocentPunishment = ShootInnocentPunishment.VANILLA;
            }
        }

        for (Role role : TMMRoles.ROLES) {
            this.setRoles(uuidListFromNbt(nbtCompound, role.identifier().toString()), role);
        }

        if (nbtCompound.contains("LooseEndWinner")) {
            this.looseEndWinner = nbtCompound.getUuid("LooseEndWinner");
        } else {
            this.looseEndWinner = null;
        }

        // Read game profiles
        this.gameProfiles.clear();
        if (nbtCompound.contains("GameProfiles")) {
            NbtList profileList = nbtCompound.getList("GameProfiles", NbtElement.COMPOUND_TYPE);
            for (NbtElement e : profileList) {
                NbtCompound profileNbt = (NbtCompound) e;
                UUID uuid = profileNbt.getUuid("uuid");
                String name = profileNbt.getString("name");
                this.gameProfiles.put(uuid, new GameProfile(uuid, name));
            }
        }

        // Read dead players
        this.deadPlayers.clear();
        if (nbtCompound.contains("DeadPlayers")) {
            for (NbtElement e : nbtCompound.getList("DeadPlayers", NbtElement.INT_ARRAY_TYPE)) {
                this.deadPlayers.add(NbtHelper.toUuid(e));
            }
        }

        // Read prevent gun pickup list
        this.preventGunPickup.clear();
        if (nbtCompound.contains("PreventGunPickup")) {
            for (NbtElement e : nbtCompound.getList("PreventGunPickup", NbtElement.INT_ARRAY_TYPE)) {
                this.preventGunPickup.add(NbtHelper.toUuid(e));
            }
        }
    }

    private ArrayList<UUID> uuidListFromNbt(NbtCompound nbtCompound, String listName) {
        ArrayList<UUID> ret = new ArrayList<>();
        for (NbtElement e : nbtCompound.getList(listName, NbtElement.INT_ARRAY_TYPE)) {
            ret.add(NbtHelper.toUuid(e));
        }
        return ret;
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound nbtCompound, RegistryWrapper.WrapperLookup wrapperLookup) {

        nbtCompound.putString("GameMode", this.gameMode != null ? this.gameMode.identifier.toString() : "");
        nbtCompound.putString("GameStatus", this.gameStatus.toString());

        nbtCompound.putInt("Fade", fade);
        nbtCompound.putInt("PsychosActive", psychosActive);

        nbtCompound.putFloat("BackfireChance", backfireChance);
        nbtCompound.putInt("NextRoundKillerCount", nextRoundKillerCount);
        nbtCompound.putInt("KillerPlayerRatio", killerPlayerRatio);

        // Write shoot innocent punishment
        nbtCompound.putString("ShootInnocentPunishment", shootInnocentPunishment.name());

        for (Role role : TMMRoles.ROLES) {
            nbtCompound.put(role.identifier().toString(), nbtFromUuidList(getAllWithRole(role)));
        }

        if (this.looseEndWinner != null) nbtCompound.putUuid("LooseEndWinner", this.looseEndWinner);

        // Write game profiles
        NbtList profileList = new NbtList();
        for (var entry : this.gameProfiles.entrySet()) {
            NbtCompound profileNbt = new NbtCompound();
            profileNbt.putUuid("uuid", entry.getKey());
            profileNbt.putString("name", entry.getValue().getName());
            profileList.add(profileNbt);
        }
        nbtCompound.put("GameProfiles", profileList);

        // Write dead players
        NbtList deadList = new NbtList();
        for (UUID uuid : this.deadPlayers) {
            deadList.add(NbtHelper.fromUuid(uuid));
        }
        nbtCompound.put("DeadPlayers", deadList);

        // Write prevent gun pickup list
        NbtList preventGunPickupList = new NbtList();
        for (UUID uuid : this.preventGunPickup) {
            preventGunPickupList.add(NbtHelper.fromUuid(uuid));
        }
        nbtCompound.put("PreventGunPickup", preventGunPickupList);
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

        if (this.isRunning()) {
            gameMode.tickClientGameLoop();
        }
    }


    @Override
    public void serverTick() {
        tickCommon();

        if (!(this.world instanceof ServerWorld serverWorld)) {
            return;
        }

        AreasWorldComponent areas = AreasWorldComponent.KEY.get(serverWorld);
        Box playArea = areas.getPlayArea();

        // attempt to reset the play area
        if (--ticksUntilNextResetAttempt == 0) {
            if (GameFunctions.tryResetTrain(serverWorld)) {
                queueTrainReset();
            } else {
                ticksUntilNextResetAttempt = -1;
            }
        }

        // if not running and spectators or not in lobby reset them
        if (serverWorld.getTime() % 20 == 0) {
            for (ServerPlayerEntity player : serverWorld.getPlayers()) {
                if (!isRunning() && (player.isSpectator() && serverWorld.getServer().getPermissionLevel(player.getGameProfile()) < 2 || (GameFunctions.isPlayerAliveAndSurvival(player) && playArea != null && playArea.contains(player.getPos())))) {
                    GameFunctions.resetPlayer(player);
                }
            }
        }

        if (serverWorld.getServer().getOverworld().equals(serverWorld)) {
            TrainWorldComponent trainComponent = TrainWorldComponent.KEY.get(serverWorld);

            // spectator limits
            if (trainComponent.getSpeed() > 0) {
                for (ServerPlayerEntity player : serverWorld.getPlayers()) {
                    if (!GameFunctions.isPlayerAliveAndSurvival(player) && isBound() && playArea != null) {
                        GameFunctions.limitPlayerToBox(player, playArea);
                    }
                }
            }

            if (this.isRunning()) {
                for (ServerPlayerEntity player : serverWorld.getPlayers()) {
                    if (GameFunctions.isPlayerAliveAndSurvival(player)) {
                        // kill players who fell off the train
                        if (playArea != null && player.getY() < playArea.minY) {
                            GameFunctions.killPlayer(player, false, player.getLastAttacker() instanceof PlayerEntity killerPlayer ? killerPlayer : null, GameConstants.DeathReasons.FELL_OUT_OF_TRAIN);
                        }

                        // put players with no role in spectator mode
                        if (GameWorldComponent.KEY.get(world).getRole(player) == null) {
                            player.changeGameMode(net.minecraft.world.GameMode.SPECTATOR);
                        }
                    }

                }


                // run game loop logic
                gameMode.tickServerGameLoop(serverWorld, this);
            }
        }

        if (serverWorld.getTime() % 20 == 0) {
            this.sync();
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

        if (this.isRunning()) {
            gameMode.tickCommonGameLoop();
        }
    }

}
