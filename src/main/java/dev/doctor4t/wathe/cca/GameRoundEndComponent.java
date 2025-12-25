package dev.doctor4t.wathe.cca;

import com.mojang.authlib.GameProfile;
import dev.doctor4t.wathe.Wathe;
import dev.doctor4t.wathe.api.Faction;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.WatheRoles;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class GameRoundEndComponent implements AutoSyncedComponent {
    public static final ComponentKey<GameRoundEndComponent> KEY = ComponentRegistry.getOrCreate(Wathe.id("roundend"), GameRoundEndComponent.class);
    private final World world;
    private final List<RoundEndData> players = new ArrayList<>();
    private GameFunctions.WinStatus winStatus = GameFunctions.WinStatus.NONE;

    public enum PlayerEndStatus {
        ALIVE,      // 存活且在线
        DEAD,       // 已死亡
        LEFT,       // 活着时退出
        LEFT_DEAD   // 死后退出
    }

    public GameRoundEndComponent(World world) {
        this.world = world;
    }

    public void sync() {
        KEY.sync(this.world);
    }

    // 新方法：从 GameWorldComponent 获取所有玩家数据（包括退出的玩家）
    public void setRoundEndData(ServerWorld serverWorld, GameFunctions.WinStatus winStatus) {
        this.players.clear();
        GameWorldComponent game = GameWorldComponent.KEY.get(this.world);

        for (Map.Entry<UUID, Role> entry : game.getRoles().entrySet()) {
            UUID uuid = entry.getKey();
            Role role = entry.getValue();
            GameProfile profile = game.getGameProfiles().get(uuid);

            if (profile == null || role == WatheRoles.NO_ROLE) continue;

            boolean wasDead = game.isPlayerDead(uuid);
            boolean isOnline = serverWorld.getPlayerByUuid(uuid) != null;

            // 确定玩家最终状态
            PlayerEndStatus endStatus;
            if (wasDead) {
                endStatus = isOnline ? PlayerEndStatus.DEAD : PlayerEndStatus.LEFT_DEAD;
            } else {
                endStatus = isOnline ? PlayerEndStatus.ALIVE : PlayerEndStatus.LEFT;
            }

            // 确定是否获胜
            boolean isWinner;
            if (winStatus == GameFunctions.WinStatus.KILLERS) {
                isWinner = role.getFaction() == Faction.KILLER;
            } else if (winStatus == GameFunctions.WinStatus.PASSENGERS || winStatus == GameFunctions.WinStatus.TIME) {
                isWinner = role.getFaction() == Faction.CIVILIAN;
            } else {
                isWinner = false;
            }

            this.players.add(new RoundEndData(profile, role.identifier(), endStatus, isWinner));
        }
        this.winStatus = winStatus;
        this.sync();
    }

    // 中立胜利重载
    public void setRoundEndData(ServerWorld serverWorld, UUID winnerUuid) {
        this.players.clear();
        GameWorldComponent game = GameWorldComponent.KEY.get(this.world);

        for (Map.Entry<UUID, Role> entry : game.getRoles().entrySet()) {
            UUID uuid = entry.getKey();
            Role role = entry.getValue();
            GameProfile profile = game.getGameProfiles().get(uuid);

            if (profile == null || role == WatheRoles.NO_ROLE) continue;

            boolean wasDead = game.isPlayerDead(uuid);
            boolean isOnline = serverWorld.getPlayerByUuid(uuid) != null;

            PlayerEndStatus endStatus;
            if (wasDead) {
                endStatus = isOnline ? PlayerEndStatus.DEAD : PlayerEndStatus.LEFT_DEAD;
            } else {
                endStatus = isOnline ? PlayerEndStatus.ALIVE : PlayerEndStatus.LEFT;
            }

            boolean isWinner = uuid.equals(winnerUuid);
            this.players.add(new RoundEndData(profile, role.identifier(), endStatus, isWinner));
        }
        this.winStatus = GameFunctions.WinStatus.NEUTRAL;
        this.sync();
    }

    public boolean didWin(UUID uuid) {
        if (GameFunctions.WinStatus.NONE == this.winStatus) return false;
        for (RoundEndData detail : this.players) {
            if (!detail.player.getId().equals(uuid)) continue;

            Faction faction = WatheRoles.getRole(detail.role).getFaction();
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

    public record RoundEndData(GameProfile player, Identifier role, PlayerEndStatus endStatus, boolean isWinner) {
        public RoundEndData(@NotNull NbtCompound tag) {
            this(
                new GameProfile(tag.getUuid("uuid"), tag.getString("name")),
                Identifier.of(tag.getString("role")),
                tag.contains("endStatus") ? PlayerEndStatus.valueOf(tag.getString("endStatus")) :
                    (tag.getBoolean("wasDead") ? PlayerEndStatus.DEAD : PlayerEndStatus.ALIVE),
                tag.getBoolean("isWinner")
            );
        }

        public @NotNull NbtCompound writeToNbt() {
            NbtCompound tag = new NbtCompound();
            tag.putUuid("uuid", this.player.getId());
            tag.putString("name", this.player.getName());
            tag.putString("role", this.role.toString());
            tag.putString("endStatus", this.endStatus.name());
            tag.putBoolean("isWinner", this.isWinner);
            return tag;
        }

        // 便捷方法
        public boolean wasDead() {
            return this.endStatus == PlayerEndStatus.DEAD ||
                   this.endStatus == PlayerEndStatus.LEFT_DEAD;
        }

        public boolean hasLeft() {
            return this.endStatus == PlayerEndStatus.LEFT ||
                   this.endStatus == PlayerEndStatus.LEFT_DEAD;
        }
    }
}