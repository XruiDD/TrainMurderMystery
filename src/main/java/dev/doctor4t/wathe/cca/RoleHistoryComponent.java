package dev.doctor4t.wathe.cca;

import dev.doctor4t.wathe.Wathe;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.WatheRoles;
import dev.doctor4t.wathe.game.rotation.GameEntry;
import dev.doctor4t.wathe.game.rotation.RoleCategory;
import dev.doctor4t.wathe.game.rotation.RoleRotation;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.ladysnake.cca.api.v3.component.Component;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 每后端本地、挂记分板的角色历史。按玩家 UUID 存最近若干局，用于债务式公平轮换。
 * 服务端单线程访问，无需加锁。委托纯算法 {@link RoleRotation} 计算债务。
 */
public class RoleHistoryComponent implements Component {
    public static final ComponentKey<RoleHistoryComponent> KEY =
        ComponentRegistry.getOrCreate(Wathe.id("role_history"), RoleHistoryComponent.class);

    private final Map<UUID, Deque<GameEntry>> history = new HashMap<>();

    public RoleHistoryComponent(Scoreboard scoreboard, @Nullable MinecraftServer server) {
        // 仅需 Map；构造签名匹配 ScoreboardComponentFactory
    }

    private Deque<GameEntry> window(UUID player) {
        return history.getOrDefault(player, new ArrayDeque<>());
    }

    /** 该玩家对某阵营的债务（正=该多分配给他）。 */
    public double debt(UUID player, RoleCategory category) {
        return RoleRotation.debt(window(player), category);
    }

    /** 该玩家窗口内拿过的具体角色 id 集合（用于避重）。 */
    public Set<String> recentRoleIds(UUID player) {
        Set<String> ids = new HashSet<>();
        for (GameEntry e : window(player)) ids.add(e.roleId());
        return ids;
    }

    /** 追加一条历史并裁剪到窗口大小。 */
    public void record(UUID player, GameEntry entry, int windowSize) {
        Deque<GameEntry> dq = history.computeIfAbsent(player, k -> new ArrayDeque<>());
        dq.addLast(entry);
        while (dq.size() > Math.max(1, windowSize)) dq.removeFirst();
    }

    public void clear() {
        history.clear();
    }

    /** 把角色映射到统计桶。顺序：杀手 → 警员/老兵 → 中立 → 平民。 */
    public static RoleCategory categoryOf(@Nullable Role role) {
        if (role == null || role == WatheRoles.NO_ROLE) return RoleCategory.CIVILIAN;
        if (role.canUseKiller()) return RoleCategory.KILLER;
        if (role == WatheRoles.VIGILANTE || role == WatheRoles.VETERAN) return RoleCategory.VIGILANTE;
        if (role.isNeutral()) return RoleCategory.NEUTRAL;
        return RoleCategory.CIVILIAN;
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        history.clear();
        if (!tag.contains("History")) return;
        NbtList players = tag.getList("History", NbtElement.COMPOUND_TYPE);
        for (NbtElement pe : players) {
            NbtCompound pc = (NbtCompound) pe;
            UUID uuid = pc.getUuid("Player");
            Deque<GameEntry> dq = new ArrayDeque<>();
            NbtList entries = pc.getList("Entries", NbtElement.COMPOUND_TYPE);
            for (NbtElement ee : entries) {
                NbtCompound ec = (NbtCompound) ee;
                RoleCategory cat;
                try {
                    cat = RoleCategory.valueOf(ec.getString("Category"));
                } catch (IllegalArgumentException ex) {
                    cat = RoleCategory.CIVILIAN;
                }
                dq.addLast(new GameEntry(
                    ec.getDouble("Ks"), ec.getDouble("Vs"), ec.getDouble("Ns"),
                    cat, ec.getString("RoleId")));
            }
            history.put(uuid, dq);
        }
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        NbtList players = new NbtList();
        for (Map.Entry<UUID, Deque<GameEntry>> en : history.entrySet()) {
            NbtCompound pc = new NbtCompound();
            pc.putUuid("Player", en.getKey());
            NbtList entries = new NbtList();
            for (GameEntry e : en.getValue()) {
                NbtCompound ec = new NbtCompound();
                ec.putDouble("Ks", e.killerShare());
                ec.putDouble("Vs", e.vigilanteShare());
                ec.putDouble("Ns", e.neutralShare());
                ec.putString("Category", e.category().name());
                ec.putString("RoleId", e.roleId());
                entries.add(ec);
            }
            pc.put("Entries", entries);
            players.add(pc);
        }
        tag.put("History", players);
    }
}
