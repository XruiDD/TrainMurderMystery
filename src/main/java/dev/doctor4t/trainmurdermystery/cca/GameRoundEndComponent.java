package dev.doctor4t.trainmurdermystery.cca;

import com.mojang.authlib.GameProfile;
import dev.doctor4t.trainmurdermystery.TMM;
import dev.doctor4t.trainmurdermystery.api.Faction;
import dev.doctor4t.trainmurdermystery.api.Role;
import dev.doctor4t.trainmurdermystery.api.TMMRoles;
import dev.doctor4t.trainmurdermystery.game.GameFunctions;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GameRoundEndComponent implements AutoSyncedComponent {
    public static final ComponentKey<GameRoundEndComponent> KEY = ComponentRegistry.getOrCreate(TMM.id("roundend"), GameRoundEndComponent.class);
    private final World world;
    private final List<RoundEndData> players = new ArrayList<>();
    private GameFunctions.WinStatus winStatus = GameFunctions.WinStatus.NONE;

    public GameRoundEndComponent(World world) {
        this.world = world;
    }

    public void sync() {
        KEY.sync(this.world);
    }

    public void setRoundEndData(@NotNull List<ServerPlayerEntity> players, GameFunctions.WinStatus winStatus) {
        this.players.clear();
        GameWorldComponent game = GameWorldComponent.KEY.get(this.world);
        for (ServerPlayerEntity player : players) {
            Role playerRole = game.getRole(player);
            switch (winStatus){
                case NONE:
                case PASSENGERS:
                case TIME:
                    this.players.add(new RoundEndData(player.getGameProfile(), playerRole.identifier(), !GameFunctions.isPlayerAliveAndSurvival(player),playerRole.getFaction() == Faction.CIVILIAN));
                    break;
                case KILLERS:
                    this.players.add(new RoundEndData(player.getGameProfile(), playerRole.identifier(), !GameFunctions.isPlayerAliveAndSurvival(player),playerRole.getFaction() == Faction.KILLER));
                    break;
            }
        }
        this.winStatus = winStatus;
        this.sync();
    }

    public void setRoundEndData(@NotNull List<ServerPlayerEntity> players, ServerPlayerEntity winner) {
        this.players.clear();
        GameWorldComponent game = GameWorldComponent.KEY.get(this.world);
        for (ServerPlayerEntity player : players) {
            Role playerRole = game.getRole(player);
            if(player.equals(winner)){
                this.players.add(new RoundEndData(player.getGameProfile(), playerRole.identifier(), !GameFunctions.isPlayerAliveAndSurvival(player),true));
            }else{
                this.players.add(new RoundEndData(player.getGameProfile(), playerRole.identifier(), !GameFunctions.isPlayerAliveAndSurvival(player),false));
            }
        }
        this.winStatus = GameFunctions.WinStatus.NEUTRAL;
        this.sync();
    }

    public boolean didWin(UUID uuid) {
        if (GameFunctions.WinStatus.NONE == this.winStatus) return false;
        for (RoundEndData detail : this.players) {
            if (!detail.player.getId().equals(uuid)) continue;

            Faction faction = TMMRoles.getRole(detail.role).getFaction();
            return switch (this.winStatus) {
                case KILLERS -> faction == Faction.KILLER;
                case PASSENGERS, TIME -> faction == Faction.CIVILIAN;
                case NEUTRAL -> detail.isWinner;
                default -> false;
            };
        }
        return false;
    }

    public List<RoundEndData> getPlayers() {
        return this.players;
    }

    public GameFunctions.WinStatus getWinStatus() {
        return this.winStatus;
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        NbtList list = new NbtList();
        for (RoundEndData detail : this.players) list.add(detail.writeToNbt());
        tag.put("players", list);
        tag.putInt("winstatus", this.winStatus.ordinal());
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.players.clear();
        for (NbtElement element : tag.getList("players", 10)) this.players.add(new RoundEndData((NbtCompound) element));
        this.winStatus = GameFunctions.WinStatus.values()[tag.getInt("winstatus")];
    }

    public record RoundEndData(GameProfile player, Identifier role, boolean wasDead, boolean isWinner) {
        public RoundEndData(@NotNull NbtCompound tag) {
            this(new GameProfile(tag.getUuid("uuid"), tag.getString("name")), Identifier.of(tag.getString("role")), tag.getBoolean("wasDead"),tag.getBoolean("isWinner"));
        }
        public @NotNull NbtCompound writeToNbt() {
            NbtCompound tag = new NbtCompound();
            tag.putUuid("uuid", this.player.getId());
            tag.putString("name", this.player.getName());
            tag.putString("role", this.role.toString());
            tag.putBoolean("wasDead", this.wasDead);
            tag.putBoolean("isWinner", this.isWinner);
            return tag;
        }
    }
}