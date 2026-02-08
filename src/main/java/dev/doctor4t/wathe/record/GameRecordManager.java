package dev.doctor4t.wathe.record;

import com.mojang.authlib.GameProfile;
import dev.doctor4t.wathe.api.Faction;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.WatheRoles;
import dev.doctor4t.wathe.api.event.RecordEvents;
import dev.doctor4t.wathe.cca.GameRoundEndComponent;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.util.ShopEntry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.Registries;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public final class GameRecordManager {
    private GameRecordManager() {
    }

    public static final class MatchRecord {
        private final UUID matchId;
        private final Identifier dimensionId;
        private final Identifier gameModeId;
        private final Identifier mapEffectId;
        private final long startTick;
        private final long startMs;
        private final List<GameRecordEvent> events = new ArrayList<>();
        private final Set<UUID> roleSnapshotRecorded = new HashSet<>();
        private boolean active = true;
        private int nextSeq = 0;

        private MatchRecord(UUID matchId, Identifier dimensionId, Identifier gameModeId, Identifier mapEffectId, long startTick, long startMs) {
            this.matchId = matchId;
            this.dimensionId = dimensionId;
            this.gameModeId = gameModeId;
            this.mapEffectId = mapEffectId;
            this.startTick = startTick;
            this.startMs = startMs;
        }

        public UUID getMatchId() {
            return matchId;
        }

        public Identifier getDimensionId() {
            return dimensionId;
        }

        public Identifier getGameModeId() {
            return gameModeId;
        }

        public Identifier getMapEffectId() {
            return mapEffectId;
        }

        public long getStartTick() {
            return startTick;
        }

        public long getStartMs() {
            return startMs;
        }

        public List<GameRecordEvent> getEvents() {
            return Collections.unmodifiableList(events);
        }

        private void addEvent(String type, long worldTick, long realTimeMs, NbtCompound data) {
            events.add(new GameRecordEvent(matchId, nextSeq++, type, worldTick, realTimeMs, data));
        }
    }

    private static MatchRecord currentMatch = null;
    private static MatchRecord lastFinishedMatch = null;
    private static final Set<UUID> connectedPlayers = new HashSet<>();

    public static synchronized boolean hasActiveMatch() {
        return currentMatch != null && currentMatch.active;
    }

    public static synchronized @Nullable MatchRecord getCurrentMatch() {
        return currentMatch;
    }

    public static synchronized @Nullable MatchRecord getLastFinishedMatch() {
        return lastFinishedMatch;
    }

    public static synchronized void startMatch(ServerWorld world, GameWorldComponent gameComponent) {
        if (currentMatch != null && currentMatch.active) {
            endMatch(world);
        }

        Identifier dimensionId = world.getRegistryKey().getValue();
        Identifier gameModeId = gameComponent.getGameMode() != null ? gameComponent.getGameMode().identifier : Identifier.of("wathe", "unknown");
        Identifier mapEffectId = gameComponent.getMapEffect() != null ? gameComponent.getMapEffect().identifier : Identifier.of("wathe", "unknown");
        currentMatch = new MatchRecord(UUID.randomUUID(), dimensionId, gameModeId, mapEffectId, world.getTime(), System.currentTimeMillis());
        connectedPlayers.clear();
    }

    public static synchronized void recordMatchStart(ServerWorld world, GameWorldComponent gameComponent) {
        if (!hasActiveMatch()) {
            return;
        }
        MatchRecord match = currentMatch;
        NbtCompound data = new NbtCompound();
        data.putString("game_mode", match.gameModeId.toString());
        data.putString("map_effect", match.mapEffectId.toString());
        data.putInt("player_count", gameComponent.getAllPlayers().size());
        addEvent(world, GameRecordTypes.MATCH_START, null, null, data);

        for (UUID uuid : gameComponent.getAllPlayers()) {
            ServerPlayerEntity player = world.getServer().getPlayerManager().getPlayer(uuid);
            if (player != null) {
                recordPlayerJoinInternal(player);
            }
        }
    }

    public static synchronized void recordRoleSnapshot(ServerWorld world, GameWorldComponent gameComponent) {
        if (!hasActiveMatch()) {
            return;
        }
        MatchRecord match = currentMatch;
        for (Map.Entry<UUID, Role> entry : gameComponent.getRoles().entrySet()) {
            UUID uuid = entry.getKey();
            Role role = entry.getValue();
            if (role == null || role == WatheRoles.NO_ROLE) {
                continue;
            }
            if (!match.roleSnapshotRecorded.add(uuid)) {
                continue;
            }
            GameProfile profile = gameComponent.getGameProfiles().get(uuid);
            NbtCompound data = new NbtCompound();
            data.put("player", buildPlayerSnapshot(uuid, profile, role, gameComponent));
            addEvent(world, GameRecordTypes.ROLE_ASSIGNED, null, null, data);
        }
    }

    public static synchronized void endMatch(ServerWorld world) {
        if (!hasActiveMatch()) {
            return;
        }
        MatchRecord match = currentMatch;
        GameRoundEndComponent roundEnd = GameRoundEndComponent.KEY.get(world.getScoreboard());
        GameFunctions.WinStatus winStatus = roundEnd.getWinStatus();

        NbtCompound endData = new NbtCompound();
        endData.putString("win_status", winStatus.name());
        if (winStatus == GameFunctions.WinStatus.NEUTRAL) {
            NbtList winners = new NbtList();
            for (GameRoundEndComponent.RoundEndData entry : roundEnd.getPlayers()) {
                if (entry.isWinner()) {
                    winners.add(NbtString.of(entry.player().getId().toString()));
                }
            }
            endData.put("winners", winners);
        }
        addEvent(world, GameRecordTypes.MATCH_END, null, null, endData);

        for (GameRoundEndComponent.RoundEndData entry : roundEnd.getPlayers()) {
            Role role = WatheRoles.getRole(entry.role());
            NbtCompound data = new NbtCompound();
            data.putUuid("player", entry.player().getId());
            data.putString("end_status", entry.endStatus().name());
            data.putBoolean("is_winner", entry.isWinner());
            addEvent(world, GameRecordTypes.PLAYER_RESULT, null, null, data);
        }

        match.active = false;
        lastFinishedMatch = match;
        currentMatch = null;
        connectedPlayers.clear();
        RecordEvents.ON_RECORD_END.invoker().onRecordEnd(world, match);
    }

    public static void recordPlayerJoin(ServerPlayerEntity player) {
        if (!hasActiveMatch()) {
            return;
        }
        recordPlayerJoinInternal(player);
    }

    public static void recordPlayerLeave(ServerPlayerEntity player) {
        if (!hasActiveMatch()) {
            return;
        }
        if (!connectedPlayers.remove(player.getUuid())) {
            return;
        }
        NbtCompound data = new NbtCompound();
        putPos(data, "pos", player.getPos());
        addEvent(player.getServerWorld(), GameRecordTypes.PLAYER_LEAVE, player, null, data);
    }

    public static void recordShopPurchase(ServerPlayerEntity player, ShopEntry entry, int index, int pricePaid) {
        if (!hasActiveMatch()) {
            return;
        }
        NbtCompound data = new NbtCompound();
        data.putString("entry_id", entry.id());
        data.putInt("index", index);
        data.putInt("price", entry.price());
        data.putInt("price_paid", pricePaid);
        data.putString("item", Registries.ITEM.getId(entry.stack().getItem()).toString());
        // 存储物品显示名称（Text.translatable），让客户端根据语言设置解析
        data.putString("item_name", Text.Serialization.toJsonString(entry.displayStack().getName(), player.getRegistryManager()));
        data.putInt("balance_after", PlayerShopComponent.KEY.get(player).getBalance());
        addEvent(player.getServerWorld(), GameRecordTypes.SHOP_PURCHASE, player, null, data);
    }

    public static void recordTaskComplete(ServerPlayerEntity player, String taskName) {
        if (!hasActiveMatch()) {
            return;
        }
        NbtCompound data = new NbtCompound();
        data.putString("task", taskName);
        addEvent(player.getServerWorld(), GameRecordTypes.TASK_COMPLETE, player, null, data);
    }

    public static void recordPoisoned(ServerPlayerEntity victim, @Nullable UUID poisonerId, int ticks, Identifier source, @Nullable NbtCompound extra) {
        if (!hasActiveMatch()) {
            return;
        }
        ServerPlayerEntity poisoner = null;
        if (poisonerId != null) {
            poisoner = victim.getServer().getPlayerManager().getPlayer(poisonerId);
        }
        NbtCompound data = extra == null ? new NbtCompound() : extra.copy();
        data.putInt("ticks", ticks);
        data.putString("source", source.toString());
        if (poisonerId != null) {
            data.putUuid("poisoner_uuid", poisonerId);
        }
        addEvent(victim.getServerWorld(), GameRecordTypes.PLAYER_POISONED, poisoner, victim, data);
    }

    public static void recordDeath(ServerPlayerEntity victim, @Nullable ServerPlayerEntity killer, Identifier deathReason) {
        if (!hasActiveMatch()) {
            return;
        }
        NbtCompound data = new NbtCompound();
        data.putString("death_reason", deathReason.toString());
        addEvent(victim.getServerWorld(), GameRecordTypes.DEATH, killer, victim, data);
    }

    public static void recordItemPickup(ServerPlayerEntity player, ItemStack stack, int count) {
        if (!hasActiveMatch()) {
            return;
        }
        NbtCompound data = new NbtCompound();
        data.putString("item", Registries.ITEM.getId(stack.getItem()).toString());
        // 存储物品显示名称（Text.translatable），让客户端根据语言设置解析
        data.putString("item_name", Text.Serialization.toJsonString(stack.getName(), player.getRegistryManager()));
        data.putInt("count", count);
        addEvent(player.getServerWorld(), GameRecordTypes.ITEM_PICKUP, player, null, data);
    }

    public static void recordItemUse(ServerPlayerEntity player, Identifier itemId, @Nullable ServerPlayerEntity target, @Nullable NbtCompound extra) {
        if (!hasActiveMatch()) {
            return;
        }
        NbtCompound data = extra == null ? new NbtCompound() : extra.copy();
        data.putString("item", itemId.toString());
        addEvent(player.getServerWorld(), GameRecordTypes.ITEM_USE, player, target, data);
    }

    public static void recordPlatterTake(ServerPlayerEntity player, Identifier itemId, BlockPos platterPos) {
        if (!hasActiveMatch()) {
            return;
        }
        NbtCompound data = new NbtCompound();
        data.putString("item", itemId.toString());
        putBlockPos(data, "pos", platterPos);
        addEvent(player.getServerWorld(), GameRecordTypes.PLATTER_TAKE, player, null, data);
    }

    public static void recordSkillUse(ServerPlayerEntity player, Identifier skillId, @Nullable ServerPlayerEntity target, @Nullable NbtCompound extra) {
        if (!hasActiveMatch()) {
            return;
        }
        NbtCompound data = extra == null ? new NbtCompound() : extra.copy();
        data.putString("skill", skillId.toString());
        addEvent(player.getServerWorld(), GameRecordTypes.SKILL_USE, player, target, data);
    }

    public static void recordGlobalEvent(ServerWorld world, Identifier eventId, @Nullable ServerPlayerEntity source, @Nullable NbtCompound extra) {
        if (!hasActiveMatch()) {
            return;
        }
        NbtCompound data = extra == null ? new NbtCompound() : extra.copy();
        data.putString("event", eventId.toString());
        addEvent(world, GameRecordTypes.GLOBAL_EVENT, source, null, data);
    }

    public static void recordDoorInteraction(ServerPlayerEntity player, BlockPos doorPos, String interactionType, String doorType, boolean success) {
        if (!hasActiveMatch()) {
            return;
        }
        NbtCompound data = new NbtCompound();
        data.putString("interaction_type", interactionType);
        data.putString("door_type", doorType);
        data.putBoolean("success", success);
        putBlockPos(data, "pos", doorPos);
        addEvent(player.getServerWorld(), GameRecordTypes.DOOR_INTERACTION, player, null, data);
    }

    // ==================== 通用事件 API ====================

    /**
     * 创建通用事件构建器
     * <p>使用示例:</p>
     * <pre>{@code
     * GameRecordManager.event("custom_event")
     *     .actor(player)
     *     .target(targetPlayer)
     *     .put("key", "value")
     *     .putInt("count", 5)
     *     .record();
     * }</pre>
     */
    public static EventBuilder event(String type) {
        return new EventBuilder(type);
    }

    public static final class EventBuilder {
        private final String type;
        private ServerWorld world;
        private ServerPlayerEntity actor;
        private ServerPlayerEntity target;
        private final NbtCompound data = new NbtCompound();

        private EventBuilder(String type) {
            this.type = type;
        }

        public EventBuilder world(ServerWorld world) {
            this.world = world;
            return this;
        }

        public EventBuilder actor(ServerPlayerEntity actor) {
            this.actor = actor;
            if (this.world == null && actor != null) {
                this.world = actor.getServerWorld();
            }
            return this;
        }

        public EventBuilder target(ServerPlayerEntity target) {
            this.target = target;
            return this;
        }

        public EventBuilder put(String key, String value) {
            data.putString(key, value);
            return this;
        }

        public EventBuilder putInt(String key, int value) {
            data.putInt(key, value);
            return this;
        }

        public EventBuilder putLong(String key, long value) {
            data.putLong(key, value);
            return this;
        }

        public EventBuilder putFloat(String key, float value) {
            data.putFloat(key, value);
            return this;
        }

        public EventBuilder putDouble(String key, double value) {
            data.putDouble(key, value);
            return this;
        }

        public EventBuilder putBool(String key, boolean value) {
            data.putBoolean(key, value);
            return this;
        }

        public EventBuilder putUuid(String key, UUID value) {
            data.putUuid(key, value);
            return this;
        }

        public EventBuilder putPos(String key, Vec3d pos) {
            GameRecordManager.putPos(data, key, pos);
            return this;
        }

        public EventBuilder putBlockPos(String key, BlockPos pos) {
            GameRecordManager.putBlockPos(data, key, pos);
            return this;
        }

        public EventBuilder putNbt(String key, NbtCompound nbt) {
            data.put(key, nbt.copy());
            return this;
        }

        /**
         * 提交事件记录
         */
        public void record() {
            if (!hasActiveMatch()) {
                return;
            }
            if (world == null) {
                return;
            }
            addEvent(world, type, actor, target, data);
        }
    }

    public static void putPos(NbtCompound data, String key, Vec3d pos) {
        NbtCompound posTag = new NbtCompound();
        posTag.putDouble("x", pos.x);
        posTag.putDouble("y", pos.y);
        posTag.putDouble("z", pos.z);
        data.put(key, posTag);
    }

    public static void putBlockPos(NbtCompound data, String key, BlockPos pos) {
        NbtCompound posTag = new NbtCompound();
        posTag.putInt("x", pos.getX());
        posTag.putInt("y", pos.getY());
        posTag.putInt("z", pos.getZ());
        data.put(key, posTag);
    }

    private static void recordPlayerJoinInternal(ServerPlayerEntity player) {
        if (!connectedPlayers.add(player.getUuid())) {
            return;
        }
        NbtCompound data = new NbtCompound();
        putPos(data, "pos", player.getPos());
        addEvent(player.getServerWorld(), GameRecordTypes.PLAYER_JOIN, player, null, data);
    }

    private static void addEvent(ServerWorld world, String type, @Nullable ServerPlayerEntity actor, @Nullable ServerPlayerEntity target, @Nullable NbtCompound data) {
        if (!hasActiveMatch()) {
            return;
        }
        MatchRecord match = currentMatch;
        NbtCompound payload = data == null ? new NbtCompound() : data.copy();
        if (actor != null) {
            payload.putUuid("actor", actor.getUuid());
        }
        if (target != null) {
            payload.putUuid("target", target.getUuid());
        }
        match.addEvent(type, world.getTime(), System.currentTimeMillis(), payload);
    }

    /**
     * 构建完整玩家快照信息，仅用于开局 ROLE_ASSIGNED 事件
     */
    private static NbtCompound buildPlayerSnapshot(UUID uuid, @Nullable GameProfile profile, @Nullable Role role, GameWorldComponent gameComponent) {
        NbtCompound info = new NbtCompound();
        info.putUuid("uuid", uuid);
        if (profile != null) {
            info.putString("name", profile.getName());
        }
        if (role != null) {
            info.putString("role", role.identifier().toString());
            Faction faction = role.getFaction();
            info.putString("faction", faction.name());
        }
        GameWorldComponent.RoomData room = gameComponent.getPlayerRoom(uuid);
        if (room != null) {
            info.putInt("room_index", room.getIndex());
            info.putString("room_name", room.getName());
        }
        return info;
    }
}
