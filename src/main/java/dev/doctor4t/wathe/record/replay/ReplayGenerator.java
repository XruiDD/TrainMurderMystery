package dev.doctor4t.wathe.record.replay;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.WatheRoles;
import dev.doctor4t.wathe.record.GameRecordEvent;
import dev.doctor4t.wathe.record.GameRecordManager;
import dev.doctor4t.wathe.record.GameRecordTypes;
import net.minecraft.item.Item;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.*;

/**
 * 回放生成器
 * 生成并向玩家发送对局回放消息
 */
public final class ReplayGenerator {
    private ReplayGenerator() {
    }

    /**
     * 玩家信息缓存
     */
    public record PlayerInfo(String name, String roleTranslationKey, int roleColor) {
    }

    /**
     * 生成并发送回放给所有在线玩家
     *
     * @param world 服务器世界
     * @param match 对局记录
     */
    public static void generateAndSend(ServerWorld world, GameRecordManager.MatchRecord match) {
        Map<UUID, PlayerInfo> playerInfoCache = buildPlayerInfoCache(match);
        List<Text> replayLines = generateReplayLines(match, world, playerInfoCache);

        // 发送给所有在线玩家
        for (ServerPlayerEntity player : world.getServer().getPlayerManager().getPlayerList()) {
            sendReplayToPlayer(player, replayLines);
        }
    }

    /**
     * 构建玩家信息缓存
     */
    private static Map<UUID, PlayerInfo> buildPlayerInfoCache(GameRecordManager.MatchRecord match) {
        Map<UUID, PlayerInfo> cache = new HashMap<>();

        for (GameRecordEvent event : match.getEvents()) {
            if (GameRecordTypes.ROLE_ASSIGNED.equals(event.type())) {
                NbtCompound data = event.data();
                NbtCompound playerData = data.getCompound("player");
                UUID uuid = playerData.getUuid("uuid");
                String name = playerData.getString("name");
                String roleIdStr = playerData.getString("role");

                Identifier roleId = Identifier.tryParse(roleIdStr);
                Role role = WatheRoles.getRole(roleId);
                int roleColor = role != null ? role.color() : 0xFFFFFF;
                // 使用翻译键，让客户端根据语言设置解析
                String roleTranslationKey = role != null && roleId != null
                        ? "announcement.role." + roleId.getPath()
                        : "unknown";

                cache.put(uuid, new PlayerInfo(name, roleTranslationKey, roleColor));
            }
        }
        return cache;
    }

    /**
     * 生成回放文本行
     */
    private static List<Text> generateReplayLines(GameRecordManager.MatchRecord match, ServerWorld world, Map<UUID, PlayerInfo> playerInfoCache) {
        List<Text> lines = new ArrayList<>();
        long startTick = match.getStartTick();

        // 设置缓存供默认格式化器使用
        DefaultReplayFormatters.setPlayerInfoCache(playerInfoCache);

        // 按时间排序事件
        List<GameRecordEvent> sortedEvents = new ArrayList<>(match.getEvents());
        sortedEvents.sort(Comparator.comparingLong(GameRecordEvent::worldTick));

        for (GameRecordEvent event : sortedEvents) {
            ReplayEventFormatter formatter = ReplayRegistry.getFormatter(event.type());
            if (formatter != null) {
                Text formatted = formatter.format(event, match, world);
                if (formatted != null) {
                    // 添加时间戳前缀
                    String timeStr = formatTime(event.worldTick(), startTick);
                    MutableText timePrefix = Text.literal("[" + timeStr + "] ").formatted(Formatting.GRAY);
                    lines.add(timePrefix.append(formatted));
                }
            }
        }

        return lines;
    }

    /**
     * 发送回放给单个玩家
     */
    private static void sendReplayToPlayer(ServerPlayerEntity player, List<Text> replayLines) {
        // 发送标题
        player.sendMessage(Text.literal("═".repeat(40)).formatted(Formatting.DARK_GRAY), false);
        player.sendMessage(Text.translatable("replay.title").formatted(Formatting.GOLD, Formatting.BOLD), false);
        player.sendMessage(Text.empty(), false);

        // 发送事件行
        for (Text line : replayLines) {
            player.sendMessage(line, false);
        }

        // 发送结尾
        player.sendMessage(Text.empty(), false);
        player.sendMessage(Text.translatable("replay.footer").formatted(Formatting.GRAY), false);
        player.sendMessage(Text.literal("═".repeat(40)).formatted(Formatting.DARK_GRAY), false);
    }

    /**
     * 格式化时间为 MM:SS 格式
     *
     * @param tick      当前世界刻
     * @param startTick 开始世界刻
     * @return 格式化后的时间字符串
     */
    public static String formatTime(long tick, long startTick) {
        long elapsedTicks = tick - startTick;
        long totalSeconds = elapsedTicks / 20;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    /**
     * 格式化玩家名称（带角色颜色）
     *
     * @param uuid            玩家 UUID
     * @param playerInfoCache 玩家信息缓存
     * @return 格式化后的玩家名称文本
     */
    public static Text formatPlayerName(UUID uuid, Map<UUID, PlayerInfo> playerInfoCache) {
        PlayerInfo info = playerInfoCache.get(uuid);
        if (info == null) {
            return Text.literal(uuid.toString().substring(0, 8));
        }

        String playerName = info.name();
        String roleTranslationKey = info.roleTranslationKey();
        int roleColor = info.roleColor();

        // 使用 Text.translatable 让客户端根据语言设置显示角色名
        MutableText text = Text.literal(playerName + "(")
                .append(Text.translatable(roleTranslationKey))
                .append(Text.literal(")"));
        return text.setStyle(Style.EMPTY.withColor(TextColor.fromRgb(roleColor)));
    }

    /**
     * 格式化物品名称，统一为 [物品名] 格式，白色文本
     *
     * @param data  事件 NBT 数据（需包含 item_name 或 item 字段）
     * @param world 服务器世界
     * @return 格式化后的物品名称文本
     */
    public static Text formatItemName(NbtCompound data, ServerWorld world) {
        Text rawName = resolveItemName(data, world);
        return Text.literal("[")
                .append(rawName)
                .append(Text.literal("]"))
                .formatted(Formatting.WHITE);
    }

    /**
     * 解析物品原始名称（优先序列化的 Text，兼容旧记录的 registry ID）
     */
    private static Text resolveItemName(NbtCompound data, ServerWorld world) {
        // 优先使用存储的物品名称（Text.translatable 序列化后的 JSON）
        if (data.contains("item_name")) {
            String nameJson = data.getString("item_name");
            if (nameJson != null && !nameJson.isEmpty()) {
                Text name = Text.Serialization.fromJson(nameJson, world.getRegistryManager());
                if (name != null) {
                    return name;
                }
            }
        }

        // 兼容旧记录：使用物品翻译键
        String itemId = data.getString("item");
        if (itemId != null && !itemId.isEmpty()) {
            Identifier id = Identifier.tryParse(itemId);
            if (id != null) {
                Item item = Registries.ITEM.get(id);
                if (item != null) {
                    return Text.translatable(item.getTranslationKey());
                }
            }
        }
        return Text.literal("unknown");
    }

    /**
     * 获取玩家信息缓存（供外部格式化器使用）
     */
    public static Map<UUID, PlayerInfo> getPlayerInfoCache(GameRecordManager.MatchRecord match) {
        return buildPlayerInfoCache(match);
    }
}
