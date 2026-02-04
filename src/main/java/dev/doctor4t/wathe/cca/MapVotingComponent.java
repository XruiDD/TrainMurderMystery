package dev.doctor4t.wathe.cca;

import dev.doctor4t.wathe.Wathe;
import dev.doctor4t.wathe.compat.TrainVoicePlugin;
import dev.doctor4t.wathe.config.datapack.MapRegistry;
import dev.doctor4t.wathe.config.datapack.MapRegistryEntry;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 地图投票 ScoreboardComponent
 * 绑定到 Scoreboard，全局唯一
 * 管理游戏结束后的地图投票流程：投票 → 加权随机 → 轮盘动画 → 传送
 */
public class MapVotingComponent implements AutoSyncedComponent, ServerTickingComponent {
    public static final ComponentKey<MapVotingComponent> KEY =
        ComponentRegistry.getOrCreate(Wathe.id("map_voting"), MapVotingComponent.class);

    private final Scoreboard scoreboard;

    // === Synced state ===
    private boolean votingActive = false;
    private int votingTicksRemaining = 0;
    private final List<VotingMapEntry> availableMaps = new ArrayList<>();
    private final List<UnavailableMapEntry> unavailableMaps = new ArrayList<>();
    private int[] voteCounts = new int[0];
    private final Map<UUID, Integer> playerVotes = new HashMap<>();
    private int selectedMapIndex = -1;
    private boolean roulettePhase = false;
    private int rouletteTicksRemaining = 0;

    // === Persisted ===
    @Nullable
    private Identifier lastSelectedDimension = null;

    // === Server-only (not synced) ===
    @Nullable
    private MinecraftServer server = null;

    private static final int VOTING_DURATION_TICKS = 30 * 20; // 30 seconds
    private static final int ROULETTE_DURATION_TICKS = 8 * 20; // 8 seconds (5s scroll + 3s stop)
    private static final int ALL_VOTED_REMAINING_TICKS = 5 * 20; // 5 seconds after all voted
    private static final int MIN_PLAYERS_FOR_GAME = 6;

    /**
     * 可用地图条目（同步到客户端）
     */
    public record VotingMapEntry(
        Identifier dimensionId,
        String displayName,
        String description,
        int minPlayers,
        int maxPlayers
    ) {}

    /**
     * 不可用地图条目（同步到客户端，展示不可用原因）
     */
    public record UnavailableMapEntry(
        Identifier dimensionId,
        String displayName,
        String reason
    ) {}

    public MapVotingComponent(Scoreboard scoreboard, @Nullable MinecraftServer server) {
        this.scoreboard = scoreboard;
        this.server = server;
    }

    public void sync() {
        KEY.sync(this.scoreboard);
    }

    // === Getters ===

    public boolean isVotingActive() {
        return votingActive;
    }

    public int getVotingTicksRemaining() {
        return votingTicksRemaining;
    }

    public List<VotingMapEntry> getAvailableMaps() {
        return availableMaps;
    }

    public List<UnavailableMapEntry> getUnavailableMaps() {
        return unavailableMaps;
    }

    public int[] getVoteCounts() {
        return voteCounts;
    }

    public int getVotedMapIndex(UUID playerId) {
        return playerVotes.getOrDefault(playerId, -1);
    }

    public int getPlayerVoteCount() {
        return playerVotes.size();
    }

    public int getSelectedMapIndex() {
        return selectedMapIndex;
    }

    public boolean isRoulettePhase() {
        return roulettePhase;
    }

    public int getRouletteTicksRemaining() {
        return rouletteTicksRemaining;
    }

    @Nullable
    public Identifier getLastSelectedDimension() {
        return lastSelectedDimension;
    }

    /**
     * Directly set lastSelectedDimension (for single-map skip voting case)
     */
    public void setLastSelectedDimensionDirect(@Nullable Identifier dimensionId) {
        this.lastSelectedDimension = dimensionId;
        this.sync();
    }

    // === Voting Logic (server-side) ===

    /**
     * 开始投票（无参数，内部获取人数并过滤地图）
     */
    public void startVoting() {
        if (server == null) {
            Wathe.LOGGER.warn("Cannot start voting: server reference not set");
            return;
        }

        int playerCount = 0;
        for (ServerWorld world : server.getWorlds()) {
            playerCount += world.getPlayers().size();
        }

        Map<Identifier, MapRegistryEntry> allMaps = MapRegistry.getInstance().getMaps();
        if (allMaps.isEmpty()) {
            Wathe.LOGGER.info("No maps registered, skipping voting");
            return;
        }

        this.votingActive = true;
        this.votingTicksRemaining = VOTING_DURATION_TICKS;
        this.selectedMapIndex = -1;
        this.roulettePhase = false;
        this.rouletteTicksRemaining = 0;
        this.playerVotes.clear();
        this.availableMaps.clear();
        this.unavailableMaps.clear();

        for (Map.Entry<Identifier, MapRegistryEntry> entry : allMaps.entrySet()) {
            MapRegistryEntry mapEntry = entry.getValue();
            if (mapEntry.isEligible(playerCount)) {
                this.availableMaps.add(new VotingMapEntry(
                    mapEntry.dimensionId(),
                    mapEntry.displayName(),
                    mapEntry.description().orElse(""),
                    mapEntry.minPlayers(),
                    mapEntry.maxPlayers()
                ));
            } else {
                // Build reason string
                String reason;
                if (playerCount < mapEntry.minPlayers()) {
                    reason = "min_players:" + mapEntry.minPlayers();
                } else {
                    reason = "max_players:" + mapEntry.maxPlayers();
                }
                this.unavailableMaps.add(new UnavailableMapEntry(
                    mapEntry.dimensionId(),
                    mapEntry.displayName(),
                    reason
                ));
            }
        }

        this.voteCounts = new int[this.availableMaps.size()];

        // If only 0 or 1 available maps, handle immediately
        if (this.availableMaps.isEmpty()) {
            Wathe.LOGGER.info("No eligible maps for {} players, voting cancelled", playerCount);
            this.votingActive = false;
            this.sync();
            return;
        }

        if (this.availableMaps.size() == 1) {
            // Single map: skip voting, teleport directly
            Identifier targetDimension = this.availableMaps.get(0).dimensionId();
            this.lastSelectedDimension = targetDimension;
            this.votingActive = false;
            this.sync();
            ServerWorld overworld = server.getOverworld();
            GameFunctions.finalizeVoting(overworld, targetDimension);
            return;
        }

        Wathe.LOGGER.info("Map voting started with {} eligible maps, {} unavailable, for {} players",
            availableMaps.size(), unavailableMaps.size(), playerCount);
        this.sync();
    }

    /**
     * 投票（服务端验证）
     */
    public void castVote(UUID playerId, int mapIndex) {
        if (!votingActive || roulettePhase) return;
        if (mapIndex < 0 || mapIndex >= availableMaps.size()) return;

        // Remove old vote
        Integer oldVote = playerVotes.get(playerId);
        if (oldVote != null && oldVote >= 0 && oldVote < voteCounts.length) {
            voteCounts[oldVote] = Math.max(0, voteCounts[oldVote] - 1);
        }

        // Record new vote
        playerVotes.put(playerId, mapIndex);
        voteCounts[mapIndex]++;

        // 所有在线玩家都投票完成时，缩短倒计时到5秒
        if (server != null && !roulettePhase) {
            int onlinePlayers = 0;
            for (ServerWorld world : server.getWorlds()) {
                onlinePlayers += world.getPlayers().size();
            }
            if (playerVotes.size() >= onlinePlayers && votingTicksRemaining > ALL_VOTED_REMAINING_TICKS) {
                votingTicksRemaining = ALL_VOTED_REMAINING_TICKS;
            }
        }

        this.sync();
    }

    /**
     * 投票结束，执行加权随机选择
     */
    private void endVoting() {
        if (server == null) return;

        int onlinePlayers = 0;
        for (ServerWorld world : server.getWorlds()) {
            onlinePlayers += world.getPlayers().size();
        }

        if (onlinePlayers < MIN_PLAYERS_FOR_GAME) {
            // Not enough players, reset timer and wait
            this.votingTicksRemaining = VOTING_DURATION_TICKS;
            Wathe.LOGGER.info("Not enough players ({}/{}) for voting result, resetting timer",
                onlinePlayers, MIN_PLAYERS_FOR_GAME);
            this.sync();
            return;
        }

        this.selectedMapIndex = selectMapWeighted();
        this.roulettePhase = true;
        this.rouletteTicksRemaining = ROULETTE_DURATION_TICKS;

        Wathe.LOGGER.info("Voting ended, selected map index {} ({})",
            selectedMapIndex,
            selectedMapIndex >= 0 && selectedMapIndex < availableMaps.size()
                ? availableMaps.get(selectedMapIndex).displayName()
                : "unknown");
        this.sync();
    }

    /**
     * 加权随机选择：0票=权重1，有票按票数
     */
    private int selectMapWeighted() {
        if (availableMaps.isEmpty()) return -1;

        Random random = new Random();
        int totalWeight = 0;
        int[] weights = new int[availableMaps.size()];

        // 检查是否有任何人投票
        boolean hasAnyVotes = false;
        for (int c : voteCounts) {
            if (c > 0) { hasAnyVotes = true; break; }
        }

        for (int i = 0; i < voteCounts.length; i++) {
            // 无人投票：所有地图等概率；有人投票：只有获得票数的地图有概率
            weights[i] = hasAnyVotes ? voteCounts[i] : 1;
            totalWeight += weights[i];
        }

        int roll = random.nextInt(totalWeight);
        int cumulative = 0;
        for (int i = 0; i < weights.length; i++) {
            cumulative += weights[i];
            if (roll < cumulative) {
                return i;
            }
        }

        return 0; // fallback
    }

    /**
     * 轮盘结束，执行传送
     */
    private void finishSelection() {
        if (server == null) return;

        if (selectedMapIndex < 0 || selectedMapIndex >= availableMaps.size()) {
            Wathe.LOGGER.warn("Invalid selected map index {}, aborting", selectedMapIndex);
            reset();
            return;
        }

        Identifier targetDimensionId = availableMaps.get(selectedMapIndex).dimensionId();
        this.lastSelectedDimension = targetDimensionId;

        // Reset voting state before teleport
        this.votingActive = false;
        this.sync();

        // Execute teleport from overworld (or any world with players)
        ServerWorld overworld = server.getOverworld();
        GameFunctions.finalizeVoting(overworld, targetDimensionId);
    }

    /**
     * 清空所有状态
     */
    public void reset() {
        this.votingActive = false;
        this.votingTicksRemaining = 0;
        this.availableMaps.clear();
        this.unavailableMaps.clear();
        this.voteCounts = new int[0];
        this.playerVotes.clear();
        this.selectedMapIndex = -1;
        this.roulettePhase = false;
        this.rouletteTicksRemaining = 0;
        this.sync();
    }

    /**
     * ServerTickingComponent: called every tick
     */
    @Override
    public void serverTick() {
        if (!votingActive) return;

        if (roulettePhase) {
            if (--rouletteTicksRemaining <= 0) {
                finishSelection();
            }
            return;
        }

        if (--votingTicksRemaining <= 0) {
            endVoting();
        }

        // Sync every second for countdown
        if (votingTicksRemaining % 20 == 0) {
            this.sync();
        }
    }

    /**
     * 新玩家加入时检查：如果投票活跃且人数达标，重新启动倒计时
     */
    public void onPlayerJoin() {
        if (server == null) return;
        if (votingActive && !roulettePhase) {
            int onlinePlayers = 0;
            for (ServerWorld world : server.getWorlds()) {
                onlinePlayers += world.getPlayers().size();
            }
            if (onlinePlayers >= MIN_PLAYERS_FOR_GAME && votingTicksRemaining <= 0) {
                votingTicksRemaining = VOTING_DURATION_TICKS;
                this.sync();
            }
        }
    }



    // === NBT ===

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.votingActive = tag.getBoolean("VotingActive");
        this.votingTicksRemaining = tag.getInt("VotingTicksRemaining");
        this.selectedMapIndex = tag.getInt("SelectedMapIndex");
        this.roulettePhase = tag.getBoolean("RoulettePhase");
        this.rouletteTicksRemaining = tag.getInt("RouletteTicksRemaining");

        // Last selected dimension (persisted)
        if (tag.contains("LastSelectedDimension")) {
            this.lastSelectedDimension = Identifier.tryParse(tag.getString("LastSelectedDimension"));
        } else {
            this.lastSelectedDimension = null;
        }

        // Available maps
        this.availableMaps.clear();
        if (tag.contains("AvailableMaps")) {
            NbtList mapsList = tag.getList("AvailableMaps", NbtElement.COMPOUND_TYPE);
            for (NbtElement e : mapsList) {
                NbtCompound mapNbt = (NbtCompound) e;
                this.availableMaps.add(new VotingMapEntry(
                    Identifier.tryParse(mapNbt.getString("DimensionId")),
                    mapNbt.getString("DisplayName"),
                    mapNbt.getString("Description"),
                    mapNbt.getInt("MinPlayers"),
                    mapNbt.getInt("MaxPlayers")
                ));
            }
        }

        // Unavailable maps
        this.unavailableMaps.clear();
        if (tag.contains("UnavailableMaps")) {
            NbtList unavList = tag.getList("UnavailableMaps", NbtElement.COMPOUND_TYPE);
            for (NbtElement e : unavList) {
                NbtCompound mapNbt = (NbtCompound) e;
                this.unavailableMaps.add(new UnavailableMapEntry(
                    Identifier.tryParse(mapNbt.getString("DimensionId")),
                    mapNbt.getString("DisplayName"),
                    mapNbt.getString("Reason")
                ));
            }
        }

        // Vote counts
        if (tag.contains("VoteCounts")) {
            this.voteCounts = tag.getIntArray("VoteCounts");
        } else {
            this.voteCounts = new int[this.availableMaps.size()];
        }

        // Player votes
        this.playerVotes.clear();
        if (tag.contains("PlayerVotes")) {
            NbtList votesList = tag.getList("PlayerVotes", NbtElement.COMPOUND_TYPE);
            for (NbtElement e : votesList) {
                NbtCompound voteNbt = (NbtCompound) e;
                UUID playerId = voteNbt.getUuid("PlayerId");
                int mapIndex = voteNbt.getInt("MapIndex");
                this.playerVotes.put(playerId, mapIndex);
            }
        }
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putBoolean("VotingActive", votingActive);
        tag.putInt("VotingTicksRemaining", votingTicksRemaining);
        tag.putInt("SelectedMapIndex", selectedMapIndex);
        tag.putBoolean("RoulettePhase", roulettePhase);
        tag.putInt("RouletteTicksRemaining", rouletteTicksRemaining);

        // Last selected dimension
        if (lastSelectedDimension != null) {
            tag.putString("LastSelectedDimension", lastSelectedDimension.toString());
        }

        // Available maps
        NbtList mapsList = new NbtList();
        for (VotingMapEntry entry : availableMaps) {
            NbtCompound mapNbt = new NbtCompound();
            mapNbt.putString("DimensionId", entry.dimensionId().toString());
            mapNbt.putString("DisplayName", entry.displayName());
            mapNbt.putString("Description", entry.description());
            mapNbt.putInt("MinPlayers", entry.minPlayers());
            mapNbt.putInt("MaxPlayers", entry.maxPlayers());
            mapsList.add(mapNbt);
        }
        tag.put("AvailableMaps", mapsList);

        // Unavailable maps
        NbtList unavList = new NbtList();
        for (UnavailableMapEntry entry : unavailableMaps) {
            NbtCompound mapNbt = new NbtCompound();
            mapNbt.putString("DimensionId", entry.dimensionId().toString());
            mapNbt.putString("DisplayName", entry.displayName());
            mapNbt.putString("Reason", entry.reason());
            unavList.add(mapNbt);
        }
        tag.put("UnavailableMaps", unavList);

        // Vote counts
        tag.putIntArray("VoteCounts", voteCounts);

        // Player votes
        NbtList votesList = new NbtList();
        for (Map.Entry<UUID, Integer> entry : playerVotes.entrySet()) {
            NbtCompound voteNbt = new NbtCompound();
            voteNbt.putUuid("PlayerId", entry.getKey());
            voteNbt.putInt("MapIndex", entry.getValue());
            votesList.add(voteNbt);
        }
        tag.put("PlayerVotes", votesList);
    }
}
