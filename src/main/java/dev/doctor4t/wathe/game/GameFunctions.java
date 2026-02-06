package dev.doctor4t.wathe.game;

import com.google.common.collect.Lists;
import dev.doctor4t.wathe.Wathe;
import dev.doctor4t.wathe.api.GameMode;
import dev.doctor4t.wathe.api.MapEffect;
import dev.doctor4t.wathe.api.event.GameEvents;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.WatheRoles;
import dev.doctor4t.wathe.api.event.ResetPlayer;
import dev.doctor4t.wathe.cca.*;
import dev.doctor4t.wathe.compat.TrainVoicePlugin;
import dev.doctor4t.wathe.config.datapack.MapRegistry;
import dev.doctor4t.wathe.util.ShopEntry;
import dev.doctor4t.wathe.util.ShopUtils;
import dev.doctor4t.wathe.config.datapack.RoomConfig;
import dev.doctor4t.wathe.entity.FirecrackerEntity;
import dev.doctor4t.wathe.entity.NoteEntity;
import dev.doctor4t.wathe.entity.PlayerBodyEntity;
import dev.doctor4t.wathe.api.event.KillPlayer;
import dev.doctor4t.wathe.api.event.ShouldDropOnDeath;
import dev.doctor4t.wathe.index.WatheDataComponentTypes;
import dev.doctor4t.wathe.index.WatheEntities;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.index.WatheSounds;
import dev.doctor4t.wathe.util.AnnounceEndingPayload;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Clearable;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.util.Language;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.Difficulty;
import net.minecraft.world.GameRules;
import net.minecraft.world.TeleportTarget;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.UnaryOperator;

public class GameFunctions {

    public static void limitPlayerToBox(ServerPlayerEntity player, Box box) {
        Vec3d playerPos = player.getPos();

        if (!box.contains(playerPos)) {
            double x = playerPos.getX();
            double y = playerPos.getY();
            double z = playerPos.getZ();

            if (z < box.minZ) {
                z = box.minZ;
            }
            if (z > box.maxZ) {
                z = box.maxZ;
            }

            if (y < box.minY) {
                y = box.minY;
            }
            if (y > box.maxY) {
                y = box.maxY;
            }

            if (x < box.minX) {
                x = box.minX;
            }
            if (x > box.maxX) {
                x = box.maxX;
            }

            player.requestTeleport(x, y, z);
        }
    }

    public static void startGame(ServerWorld world, GameMode gameMode, MapEffect mapEffect, int time) {
        MapVotingComponent votingComponent = MapVotingComponent.KEY.get(world.getServer().getScoreboard());
        if (votingComponent.isVotingActive()) {
            for (ServerPlayerEntity player : world.getPlayers()) {
                player.sendMessage(Text.translatable("game.start_error.voting_active"), true);
            }
            return;
        }

        GameWorldComponent game = GameWorldComponent.KEY.get(world);
        MapVariablesWorldComponent areas = MapVariablesWorldComponent.KEY.get(world);
        int playerCount = Math.toIntExact(world.getPlayers().stream().filter(serverPlayerEntity -> isPlayerInReadyArea(serverPlayerEntity, areas)).count());
        game.setGameMode(gameMode);
        game.setMapEffect(mapEffect);
        GameTimeComponent.KEY.get(world).setResetTime(time);

        if (playerCount >= gameMode.minPlayerCount) {
            game.setGameStatus(GameWorldComponent.GameStatus.STARTING);
        } else {
            for (ServerPlayerEntity player : world.getPlayers()) {
                player.sendMessage(Text.translatable("game.start_error.not_enough_players", gameMode.minPlayerCount), true);
            }
        }
    }

    public static void stopGame(ServerWorld world) {
        GameWorldComponent component = GameWorldComponent.KEY.get(world);
        component.setGameStatus(GameWorldComponent.GameStatus.STOPPING);
    }

    public static void initializeGame(ServerWorld serverWorld) {
        GameWorldComponent gameComponent = GameWorldComponent.KEY.get(serverWorld);
        List<ServerPlayerEntity> readyPlayerList = getReadyPlayerList(serverWorld);

        GameEvents.ON_GAME_START.invoker().onGameStart(gameComponent.getGameMode());
        // baseInitialize现在返回房间号映射
        Map<UUID, Integer> playerRoomMap = baseInitialize(serverWorld, gameComponent, readyPlayerList);
        gameComponent.getGameMode().initializeGame(serverWorld, gameComponent, readyPlayerList);

        // 角色分配后初始化商店
        initializeShopsForPlayers(readyPlayerList);

        // 角色分配后再生成信件
        giveLettersToPlayers(serverWorld, gameComponent, readyPlayerList, playerRoomMap);

        GameEvents.ON_FINISH_INITIALIZE.invoker().onFinishInitialize(serverWorld, gameComponent);
        gameComponent.setGameStatus(GameWorldComponent.GameStatus.ACTIVE);
        gameComponent.sync();
    }

    private static Map<UUID, Integer> baseInitialize(ServerWorld serverWorld, GameWorldComponent gameComponent, List<ServerPlayerEntity> players) {
        MapVariablesWorldComponent areas = MapVariablesWorldComponent.KEY.get(serverWorld);
        MapEnhancementsWorldComponent enhancements = MapEnhancementsWorldComponent.KEY.get(serverWorld);

        WorldBlackoutComponent.KEY.get(serverWorld).reset();

        serverWorld.getGameRules().get(GameRules.KEEP_INVENTORY).set(true, serverWorld.getServer());
        serverWorld.getGameRules().get(GameRules.DO_WEATHER_CYCLE).set(false, serverWorld.getServer());
        serverWorld.getGameRules().get(GameRules.DO_DAYLIGHT_CYCLE).set(false, serverWorld.getServer());
        serverWorld.getGameRules().get(GameRules.DO_MOB_GRIEFING).set(false, serverWorld.getServer());
        serverWorld.getGameRules().get(GameRules.DO_MOB_SPAWNING).set(false, serverWorld.getServer());
        serverWorld.getGameRules().get(GameRules.ANNOUNCE_ADVANCEMENTS).set(false, serverWorld.getServer());
        serverWorld.getGameRules().get(GameRules.DO_TRADER_SPAWNING).set(false, serverWorld.getServer());
        serverWorld.getGameRules().get(GameRules.PLAYERS_SLEEPING_PERCENTAGE).set(9999, serverWorld.getServer());
        serverWorld.getServer().setDifficulty(Difficulty.PEACEFUL, true);


        for (ServerPlayerEntity player : serverWorld.getPlayers()) {
            player.dismountVehicle();
        }

        for (ServerPlayerEntity player : players) {
            player.changeGameMode(net.minecraft.world.GameMode.ADVENTURE);
        }


        // kick non playing players
        for (ServerPlayerEntity player : serverWorld.getPlayers(serverPlayerEntity -> !players.contains(serverPlayerEntity))) {
            if(player.hasPermissionLevel(1)){
                player.changeGameMode(net.minecraft.world.GameMode.SPECTATOR);
                TrainVoicePlugin.addPlayer(player.getUuid());
                MapVariablesWorldComponent.PosWithOrientation spectatorSpawnPos = areas.getSpectatorSpawnPos();
                if (spectatorSpawnPos != null) {
                    player.teleport(serverWorld, spectatorSpawnPos.pos.getX(), spectatorSpawnPos.pos.getY(), spectatorSpawnPos.pos.getZ(), spectatorSpawnPos.yaw, spectatorSpawnPos.pitch);
                }
            } else {
                player.networkHandler.disconnect(Text.translatable("disconnect.wathe.not_in_ready_area"));
            }
        }

        // clear items, clear previous game data
        for (ServerPlayerEntity serverPlayerEntity : players) {
            serverPlayerEntity.getInventory().clear();
            PlayerMoodComponent.KEY.get(serverPlayerEntity).reset();
            PlayerShopComponent.KEY.get(serverPlayerEntity).reset();
            PlayerPoisonComponent.KEY.get(serverPlayerEntity).reset();
            PlayerPsychoComponent.KEY.get(serverPlayerEntity).reset();
            PlayerNoteComponent.KEY.get(serverPlayerEntity).reset();
            PlayerShopComponent.KEY.get(serverPlayerEntity).reset();
            PlayerStaminaComponent.KEY.get(serverPlayerEntity).reset();
            PlayerVeteranComponent.KEY.get(serverPlayerEntity).reset();
            TrainVoicePlugin.resetPlayer(serverPlayerEntity.getUuid());
            // remove item cooldowns
            HashSet<Item> copy = new HashSet<>(serverPlayerEntity.getItemCooldownManager().entries.keySet());
            for (Item item : copy) serverPlayerEntity.getItemCooldownManager().remove(item);
            ResetPlayer.EVENT.invoker().onReset(serverPlayerEntity);
        }
        gameComponent.clearRoleMap();
        gameComponent.clearPreventGunPickup(); // 清空射杀无辜惩罚列表
        GameTimeComponent.KEY.get(serverWorld).reset();

        // reset map
        gameComponent.queueMapReset();

        // select rooms and give keys
        Random random = new Random();
        Map<UUID, Integer> playerRoomMap = new HashMap<>();
        int totalRooms = enhancements.getRoomCount();

        // map effect initialize
        gameComponent.getMapEffect().initializeMapEffects(serverWorld, players);

        if (totalRooms > 0) {
            // 有房间配置：使用配置的房间分配和传送
            Map<Integer, Integer> roomPlayerCounts = new HashMap<>(); // 记录每个房间已分配的玩家数

            for (ServerPlayerEntity serverPlayerEntity : players) {
                // 随机选择一个有空位的房间
                int roomNumber = findRandomAvailableRoom(roomPlayerCounts, enhancements, totalRooms, random);
                int playerIndexInRoom = roomPlayerCounts.getOrDefault(roomNumber, 0);

                // 获取房间名（如果配置了自定义名称则使用，否则默认 "Room X"）
                String roomName = enhancements.getRoomConfig(roomNumber)
                    .map(config -> config.getName(roomNumber))
                    .orElse("Room " + roomNumber);

                playerRoomMap.put(serverPlayerEntity.getUuid(), roomNumber);
                gameComponent.addPlayerToRoom(roomNumber, roomName, serverPlayerEntity); // 持久化到组件
                roomPlayerCounts.put(roomNumber, playerIndexInRoom + 1);

                // 给钥匙
                ItemStack itemStack = new ItemStack(WatheItems.KEY);
                itemStack.apply(DataComponentTypes.LORE, LoreComponent.DEFAULT, component -> new LoreComponent(Text.literal(roomName).getWithStyle(Style.EMPTY.withItalic(false).withColor(0xFF8C00))));
                serverPlayerEntity.giveItemStack(itemStack);

                // 传送玩家到对应房间的出生点
                enhancements.getSpawnPointForPlayer(roomNumber, playerIndexInRoom).ifPresent(spawnPoint -> {
                    serverPlayerEntity.requestTeleport(spawnPoint.x(), spawnPoint.y(), spawnPoint.z());
                    serverPlayerEntity.setYaw(spawnPoint.yaw());
                    serverPlayerEntity.setPitch(spawnPoint.pitch());
                });
            }
        } else {
            // 没有房间配置：使用原来的7个房间循环分配
            int roomNumber = 0;
            for (ServerPlayerEntity serverPlayerEntity : players) {
                roomNumber = roomNumber % 7 + 1;
                String roomName = "Room " + roomNumber;

                playerRoomMap.put(serverPlayerEntity.getUuid(), roomNumber);
                gameComponent.addPlayerToRoom(roomNumber, roomName, serverPlayerEntity); // 持久化到组件

                // 给钥匙
                ItemStack itemStack = new ItemStack(WatheItems.KEY);
                int finalRoomNumber = roomNumber;
                itemStack.apply(DataComponentTypes.LORE, LoreComponent.DEFAULT, component -> new LoreComponent(Text.literal("Room " + finalRoomNumber).getWithStyle(Style.EMPTY.withItalic(false).withColor(0xFF8C00))));
                serverPlayerEntity.giveItemStack(itemStack);

                Vec3i offsetVec3i = areas.getPlayAreaOffset();
                if (offsetVec3i != null) {
                    Vec3d offset = Vec3d.of(offsetVec3i);
                    Vec3d pos = serverPlayerEntity.getPos().add(offset);
                    serverPlayerEntity.requestTeleport(pos.getX(), pos.getY() + 1, pos.getZ());
                }
            }
        }



        return playerRoomMap;
    }

    /**
     * Initializes shop state for all players after role assignment.
     * This sets up initial cooldowns and stock limits based on each player's shop entries.
     */
    private static void initializeShopsForPlayers(List<ServerPlayerEntity> players) {
        for (ServerPlayerEntity player : players) {
            List<ShopEntry> entries = ShopUtils.getShopEntriesForPlayer(player);
            PlayerShopComponent.KEY.get(player).initializeShop(entries);
        }
    }

    private static void giveLettersToPlayers(ServerWorld serverWorld, GameWorldComponent gameComponent,
                                              List<ServerPlayerEntity> players, Map<UUID, Integer> playerRoomMap) {
        int letterColor = 0xC5AE8B; // 固定棕色，与旧版一致

        for (ServerPlayerEntity player : players) {
            ItemStack letter = new ItemStack(WatheItems.LETTER);
            letter.set(DataComponentTypes.ITEM_NAME, Text.translatable(letter.getTranslationKey()));

            Role role = gameComponent.getRole(player);
            String roleName = (role != null && role != WatheRoles.NO_ROLE) ? role.identifier().getPath() : null;
            String factionName = (role != null && role != WatheRoles.NO_ROLE) ? role.getFaction().name().toLowerCase() : null;
            int roleColor = (role != null && role != WatheRoles.NO_ROLE) ? role.color() : letterColor;

            applyLetterLore(letter, player, role, roleName, factionName, letterColor, roleColor);
            player.giveItemStack(letter);
        }
    }

    /**
     * 应用信件Lore，支持动态fallback机制
     * 结构：name, room, tooltip1, tooltip2, ... tooltipX（直到检测不到为止）
     * Fallback顺序：tip.letter.{roleName}.tooltipX -> tip.letter.{faction}.tooltipX -> tip.letter.tooltipX
     */
    private static void applyLetterLore(ItemStack letter, ServerPlayerEntity player, Role role,
                                         String roleName, String factionName, int letterColor, int roleColor) {
        letter.apply(DataComponentTypes.LORE, LoreComponent.DEFAULT, component -> {
            List<Text> text = new ArrayList<>();
            UnaryOperator<Style> stylizer = style -> style.withItalic(false).withColor(letterColor);
            String playerName = getPlayerDisplayName(player);

            // 获取角色显示名称
            String roleDisplayName = Text.translatable("announcement.role" + "." + roleName).getString();

            // name - 使用fallback机制，包含玩家名和角色名（角色名使用角色颜色）
            String nameKey = resolveTranslationKey("name", roleName, factionName);
            Text roleText = Text.literal(roleDisplayName).styled(style -> style.withItalic(false).withColor(roleColor));
            text.add(Text.translatable(nameKey, playerName, roleText)
                .styled(style -> style.withItalic(false).withColor(0xFFFFFF)));

            // room - 使用fallback机制
            String roomKey = resolveTranslationKey("room", roleName, factionName);
            text.add(Text.translatable(roomKey).styled(stylizer));

            // tooltipX - 循环直到检测不到为止
            int tooltipIndex = 1;
            while (true) {
                String tooltipKey = resolveTranslationKey("tooltip" + tooltipIndex, roleName, factionName);
                if (tooltipKey == null) {
                    break; // 没有找到任何翻译键，停止循环
                }

                // 检查是否需要添加空行（在角色/阵营特定tooltip之前）
                if (tooltipIndex == 1) {
                    // 检查是否使用了角色或阵营特定的tooltip
                    String roleSpecific = "tip.letter." + roleName + ".tooltip1";
                    String factionSpecific = "tip.letter." + factionName + ".tooltip1";
                    if ((roleName != null && hasTranslation(roleSpecific)) ||
                        (factionName != null && hasTranslation(factionSpecific))) {
                        text.add(Text.empty()); // 添加空行分隔
                    }
                }

                text.add(Text.translatable(tooltipKey).styled(stylizer));
                tooltipIndex++;
            }

            return new LoreComponent(text);
        });
    }

    /**
     * 解析翻译键，按fallback顺序查找
     * @return 找到的翻译键，如果都不存在则返回null
     */
    private static String resolveTranslationKey(String suffix, String roleName, String factionName) {
        // 1. 优先使用角色特定的
        if (roleName != null) {
            String roleKey = "tip.letter." + roleName + "." + suffix;
            if (hasTranslation(roleKey)) {
                return roleKey;
            }
        }

        // 2. 其次使用阵营特定的
        if (factionName != null) {
            String factionKey = "tip.letter." + factionName + "." + suffix;
            if (hasTranslation(factionKey)) {
                return factionKey;
            }
        }

        // 3. 最后使用通用的
        String genericKey = "tip.letter." + suffix;
        if (hasTranslation(genericKey)) {
            return genericKey;
        }

        return null; // 没有找到任何翻译键
    }

    /**
     * 检查翻译键是否存在
     */
    private static boolean hasTranslation(String key) {
        return Language.getInstance().hasTranslation(key);
    }

    private static String getPlayerDisplayName(ServerPlayerEntity player) {
        Text displayName = player.getDisplayName();
        String name = displayName != null ? displayName.getString() : player.getName().getString();
        if (!name.isEmpty() && name.charAt(name.length() - 1) == '\uE780') {
            name = name.substring(0, name.length() - 1);
        }
        return name;
    }

    /**
     * 判断玩家是否在准备区域内
     * @param player 要检查的玩家
     * @param areas 地图区域组件
     * @return 如果玩家在准备区域内返回true
     */
    private static boolean isPlayerInReadyArea(PlayerEntity player, MapVariablesWorldComponent areas) {
        return areas.getReadyArea().contains(player.getPos()) && !player.isSpectator() && !player.isCreative();
    }

    private static List<ServerPlayerEntity> getReadyPlayerList(ServerWorld serverWorld) {
        MapVariablesWorldComponent areas = MapVariablesWorldComponent.KEY.get(serverWorld);
        List<ServerPlayerEntity> players = serverWorld.getPlayers(serverPlayerEntity -> isPlayerInReadyArea(serverPlayerEntity, areas) && !serverPlayerEntity.isSpectator());
        return players;
    }

    public static void finalizeGame(ServerWorld world) {
        GameWorldComponent gameComponent = GameWorldComponent.KEY.get(world);
        GameEvents.ON_GAME_STOP.invoker().onGameStop(gameComponent.getGameMode());
        GameRecordManager.endMatch(world);
        gameComponent.getGameMode().finalizeGame(world, gameComponent);

        WorldBlackoutComponent.KEY.get(world).reset();
        TrainWorldComponent trainComponent = TrainWorldComponent.KEY.get(world);

        // discard all player bodies
        for (PlayerBodyEntity body : world.getEntitiesByType(WatheEntities.PLAYER_BODY, playerBodyEntity -> true))
            body.discard();
        for (FirecrackerEntity entity : world.getEntitiesByType(WatheEntities.FIRECRACKER, entity -> true))
            entity.discard();
        for (NoteEntity entity : world.getEntitiesByType(WatheEntities.NOTE, entity -> true)) entity.discard();
        for (ItemEntity item : world.getEntitiesByType(EntityType.ITEM, playerBodyEntity -> true)) item.discard();
        // reset all players
        for (ServerPlayerEntity player : world.getPlayers()) {
            ServerPlayNetworking.send(player, new AnnounceEndingPayload());
            resetPlayer(player);
        }

        // reset game component
        GameTimeComponent.KEY.get(world).reset();
        gameComponent.clearRoleMap();
        gameComponent.setGameStatus(GameWorldComponent.GameStatus.INACTIVE);
        trainComponent.setTime(0);
        gameComponent.sync();

        GameEvents.ON_FINISH_FINALIZE.invoker().onFinishFinalize(world, gameComponent);

        // Check if map voting should start
        if (MapRegistry.getInstance().getMapCount() > 0) {
            MapVotingComponent voting = MapVotingComponent.KEY.get(
                world.getServer().getScoreboard());
            voting.startVoting();
        }
    }

    public static void resetPlayer(ServerPlayerEntity player) {
        player.dismountVehicle();
        player.getInventory().clear();
        PlayerMoodComponent.KEY.get(player).reset();
        PlayerShopComponent.KEY.get(player).reset();
        PlayerPoisonComponent.KEY.get(player).reset();
        PlayerPsychoComponent.KEY.get(player).reset();
        PlayerNoteComponent.KEY.get(player).reset();
        PlayerStaminaComponent.KEY.get(player).reset();
        PlayerVeteranComponent.KEY.get(player).reset();
        TrainVoicePlugin.resetPlayer(player.getUuid());
        player.changeGameMode(net.minecraft.world.GameMode.ADVENTURE);
        player.wakeUp();
        MapVariablesWorldComponent.PosWithOrientation spawnPos = MapVariablesWorldComponent.KEY.get(player.getWorld()).getSpawnPos();
        TeleportTarget teleportTarget = new TeleportTarget(player.getServerWorld(), spawnPos.pos, Vec3d.ZERO, spawnPos.yaw, spawnPos.pitch, TeleportTarget.NO_OP);
        player.teleportTo(teleportTarget);
        ResetPlayer.EVENT.invoker().onReset(player);
    }

    @SuppressWarnings("unused")
    public static void killPlayer(ServerPlayerEntity victim, boolean spawnBody, @Nullable ServerPlayerEntity killer) {
        killPlayer(victim, spawnBody, killer, GameConstants.DeathReasons.GENERIC);
    }

    public static void killPlayer(ServerPlayerEntity victim, boolean spawnBody, @Nullable ServerPlayerEntity killer, Identifier deathReason){
        killPlayer(victim, spawnBody, killer, deathReason, false);
    }

    public static void killPlayer(ServerPlayerEntity victim, boolean spawnBody, @Nullable ServerPlayerEntity killer, Identifier deathReason, boolean force) {
        PlayerPsychoComponent component = PlayerPsychoComponent.KEY.get(victim);

        // Fire BEFORE event
        KillPlayer.KillResult beforeResult = KillPlayer.BEFORE.invoker().beforeKillPlayer(victim, killer, deathReason);
        if (beforeResult != null && !force && beforeResult.cancelled()) return;

        // Override spawnBody if the event result specifies it
        if (beforeResult != null && beforeResult.spawnBody() != null) {
            spawnBody = beforeResult.spawnBody();
        }

        if (!force && component.getPsychoTicks() > 0) {
            if (component.getArmour() > 0) {
                component.setArmour(component.getArmour() - 1);
                component.sync();
                victim.playSoundToPlayer(WatheSounds.ITEM_PSYCHO_ARMOUR, SoundCategory.MASTER, 5F, 1F);
                return;
            } else {
                component.stopPsycho();
            }
        }
        if(force && component.getPsychoTicks() > 0){
            component.stopPsycho();
        }

        if (victim instanceof ServerPlayerEntity serverPlayerEntity && isPlayerPlayingAndAlive(serverPlayerEntity)) {
            serverPlayerEntity.changeGameMode(net.minecraft.world.GameMode.SPECTATOR);
            GameWorldComponent.KEY.get(victim.getWorld()).markPlayerDead(victim.getUuid());
        } else {
            return;
        }

        if (killer != null) {
            if (GameWorldComponent.KEY.get(killer.getWorld()).canUseKillerFeatures(killer)) {
                PlayerShopComponent.KEY.get(killer).addToBalance(GameConstants.MONEY_PER_KILL);
            }

            // replenish derringer
            for (List<ItemStack> list : killer.getInventory().combinedInventory) {
                for (ItemStack stack : list) {
                    Boolean used = stack.get(WatheDataComponentTypes.USED);
                    if (stack.isOf(WatheItems.DERRINGER) && used != null && used) {
                        stack.set(WatheDataComponentTypes.USED, false);
                        killer.playSoundToPlayer(WatheSounds.ITEM_DERRINGER_RELOAD, SoundCategory.PLAYERS, 1.0f, 1.0f);
                    }
                }
            }
        }

        PlayerMoodComponent.KEY.get(victim).reset();

        if (spawnBody) {
            PlayerBodyEntity body = WatheEntities.PLAYER_BODY.create(victim.getWorld());
            if (body != null) {
                body.setPlayerUuid(victim.getUuid());
                body.setDeathReason(deathReason);
                Vec3d spawnPos = victim.getPos().add(victim.getRotationVector().normalize().multiply(1));
                body.refreshPositionAndAngles(spawnPos.getX(), victim.getY(), spawnPos.getZ(), victim.getHeadYaw(), 0f);
                body.setYaw(victim.getHeadYaw());
                body.setHeadYaw(victim.getHeadYaw());
                victim.getWorld().spawnEntity(body);
            }
        }

        for (List<ItemStack> list : victim.getInventory().combinedInventory) {
            for (int i = 0; i < list.size(); i++) {
                ItemStack stack = list.get(i);
                if (shouldDropOnDeath(stack, victim)) {
                    victim.dropItem(stack, true, false);
                    list.set(i, ItemStack.EMPTY);
                }
            }
        }

        GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(victim.getWorld());
        if (gameWorldComponent.isInnocent(victim)) {
            GameTimeComponent.KEY.get(victim.getWorld()).addTime(GameConstants.TIME_ON_CIVILIAN_KILL);
        }

        TrainVoicePlugin.addPlayer(victim.getUuid());

        GameRecordManager.recordDeath(serverPlayerEntity, killer, deathReason);

        // Fire AFTER event
        KillPlayer.AFTER.invoker().afterKillPlayer(victim, killer, deathReason);
    }

    public static boolean shouldDropOnDeath(@NotNull ItemStack stack, PlayerEntity victim) {
        return !stack.isEmpty() && (stack.isOf(WatheItems.REVOLVER) || ShouldDropOnDeath.EVENT.invoker().shouldDrop(stack, victim));
    }

    public static boolean isPlayerAliveAndSurvival(PlayerEntity player) {
        return player != null && !player.isSpectator() && !player.isCreative();
    }

    public static boolean isPlayerPlayingAndAlive(PlayerEntity player) {
        return player != null && GameWorldComponent.KEY.get(player.getWorld()).isRunning() && GameWorldComponent.KEY.get(player.getWorld()).hasAnyRole(player.getUuid()) && !GameWorldComponent.KEY.get(player.getWorld()).isPlayerDead(player.getUuid());
    }

    public static boolean isPlayerSpectatingOrCreative(PlayerEntity player) {
        return player != null && (player.isSpectator() || player.isCreative());
    }

    record BlockEntityInfo(NbtCompound nbt, ComponentMap components) {
    }

    record BlockInfo(BlockPos pos, BlockState state, @Nullable BlockEntityInfo blockEntityInfo) {
    }

    enum Mode {
        FORCE(true),
        MOVE(true),
        NORMAL(false);

        private final boolean allowsOverlap;

        Mode(final boolean allowsOverlap) {
            this.allowsOverlap = allowsOverlap;
        }

        public boolean allowsOverlap() {
            return this.allowsOverlap;
        }
    }

    // returns whether another reset should be attempted
    public static boolean tryResetTrain(ServerWorld serverWorld) {
        Identifier dimensionId = serverWorld.getRegistryKey().getValue();
        MapVariablesWorldComponent areas = MapVariablesWorldComponent.KEY.get(serverWorld);
        BlockPos backupMinPos = BlockPos.ofFloored(areas.getResetTemplateArea().getMinPos());
        BlockPos backupMaxPos = BlockPos.ofFloored(areas.getResetTemplateArea().getMaxPos());
        BlockBox backupTrainBox = BlockBox.create(backupMinPos, backupMaxPos);
        BlockPos trainMinPos = BlockPos.ofFloored(areas.getResetTemplateArea().offset(Vec3d.of(areas.getResetPasteOffset())).getMinPos());
        BlockPos trainMaxPos = trainMinPos.add(backupTrainBox.getDimensions());
        BlockBox trainBox = BlockBox.create(trainMinPos, trainMaxPos);

        if (serverWorld.isRegionLoaded(backupMinPos, backupMaxPos) && serverWorld.isRegionLoaded(trainMinPos, trainMaxPos)) {
            List<BlockInfo> list = Lists.newArrayList();
            List<BlockInfo> list2 = Lists.newArrayList();
            List<BlockInfo> list3 = Lists.newArrayList();
            Deque<BlockPos> deque = Lists.newLinkedList();
            BlockPos blockPos5 = new BlockPos(
                    trainBox.getMinX() - backupTrainBox.getMinX(), trainBox.getMinY() - backupTrainBox.getMinY(), trainBox.getMinZ() - backupTrainBox.getMinZ()
            );

            for (int k = backupTrainBox.getMinZ(); k <= backupTrainBox.getMaxZ(); k++) {
                for (int l = backupTrainBox.getMinY(); l <= backupTrainBox.getMaxY(); l++) {
                    for (int m = backupTrainBox.getMinX(); m <= backupTrainBox.getMaxX(); m++) {
                        BlockPos blockPos6 = new BlockPos(m, l, k);
                        BlockPos blockPos7 = blockPos6.add(blockPos5);
                        CachedBlockPosition cachedBlockPosition = new CachedBlockPosition(serverWorld, blockPos6, false);
                        BlockState blockState = cachedBlockPosition.getBlockState();

                        BlockEntity blockEntity = serverWorld.getBlockEntity(blockPos6);
                        if (blockEntity != null) {
                            BlockEntityInfo blockEntityInfo = new BlockEntityInfo(
                                    blockEntity.createComponentlessNbt(serverWorld.getRegistryManager()), blockEntity.getComponents()
                            );
                            list2.add(new BlockInfo(blockPos7, blockState, blockEntityInfo));
                            deque.addLast(blockPos6);
                        } else if (!blockState.isOpaqueFullCube(serverWorld, blockPos6) && !blockState.isFullCube(serverWorld, blockPos6)) {
                            list3.add(new BlockInfo(blockPos7, blockState, null));
                            deque.addFirst(blockPos6);
                        } else {
                            list.add(new BlockInfo(blockPos7, blockState, null));
                            deque.addLast(blockPos6);
                        }
                    }
                }
            }

            List<BlockInfo> list4 = Lists.newArrayList();
            list4.addAll(list);
            list4.addAll(list2);
            list4.addAll(list3);
            List<BlockInfo> list5 = Lists.reverse(list4);

            for (BlockInfo blockInfo : list5) {
                BlockEntity blockEntity3 = serverWorld.getBlockEntity(blockInfo.pos);
                Clearable.clear(blockEntity3);
                serverWorld.setBlockState(blockInfo.pos, Blocks.BARRIER.getDefaultState(), Block.NOTIFY_LISTENERS);
            }

            int mx = 0;

            for (BlockInfo blockInfo2 : list4) {
                if (serverWorld.setBlockState(blockInfo2.pos, blockInfo2.state, Block.NOTIFY_LISTENERS)) {
                    mx++;
                }
            }

            for (BlockInfo blockInfo2x : list2) {
                BlockEntity blockEntity4 = serverWorld.getBlockEntity(blockInfo2x.pos);
                if (blockInfo2x.blockEntityInfo != null && blockEntity4 != null) {
                    blockEntity4.readComponentlessNbt(blockInfo2x.blockEntityInfo.nbt, serverWorld.getRegistryManager());
                    blockEntity4.setComponents(blockInfo2x.blockEntityInfo.components);
                    blockEntity4.markDirty();
                }

                serverWorld.setBlockState(blockInfo2x.pos, blockInfo2x.state, Block.NOTIFY_LISTENERS);
            }

            for (BlockInfo blockInfo2x : list5) {
                serverWorld.updateNeighbors(blockInfo2x.pos, blockInfo2x.state.getBlock());
            }

            serverWorld.getBlockTickScheduler().scheduleTicks(serverWorld.getBlockTickScheduler(), backupTrainBox, blockPos5);
            if (mx == 0) {
                Wathe.LOGGER.info("Train reset failed: No blocks copied. Queueing another attempt. Dimension: {}", dimensionId);
                return true;
            }
        } else {
            Wathe.LOGGER.info("Train reset failed: Clone positions not loaded. Queueing another attempt. Dimension: {}", dimensionId);
            return true;
        }

        // discard all player bodies and items
        for (PlayerBodyEntity body : serverWorld.getEntitiesByType(WatheEntities.PLAYER_BODY, playerBodyEntity -> true)) {
            body.discard();
        }
        for (ItemEntity item : serverWorld.getEntitiesByType(EntityType.ITEM, playerBodyEntity -> true)) {
            item.discard();
        }
        for (FirecrackerEntity entity : serverWorld.getEntitiesByType(WatheEntities.FIRECRACKER, entity -> true))
            entity.discard();
        for (NoteEntity entity : serverWorld.getEntitiesByType(WatheEntities.NOTE, entity -> true))
            entity.discard();

        Wathe.LOGGER.info("Train reset successful. Dimension: {}", dimensionId);
        return false;
    }

    public static int getReadyPlayerCount(World world) {
        List<? extends PlayerEntity> players = world.getPlayers();
        MapVariablesWorldComponent areas = MapVariablesWorldComponent.KEY.get(world);
        return Math.toIntExact(players.stream().filter(p -> isPlayerInReadyArea(p, areas)).count());
    }

    /**
     * 随机选择一个有空位的房间
     * 从所有未满的房间中随机选择一个
     * 如果所有房间都满了，则按顺序从第一个房间开始强制塞人
     */
    private static int findRandomAvailableRoom(Map<Integer, Integer> roomPlayerCounts, MapEnhancementsWorldComponent enhancements, int totalRooms, Random random) {
        // 收集所有有空位的房间
        List<Integer> availableRooms = new ArrayList<>();
        for (int i = 1; i <= totalRooms; i++) {
            int currentCount = roomPlayerCounts.getOrDefault(i, 0);
            int maxPlayers = enhancements.getRoomConfig(i).map(RoomConfig::getMaxPlayers).orElse(1);
            if (currentCount < maxPlayers) {
                availableRooms.add(i);
            }
        }

        // 如果有空位的房间，随机选择一个
        if (!availableRooms.isEmpty()) {
            return availableRooms.get(random.nextInt(availableRooms.size()));
        }

        // 所有房间都满了，按顺序强制塞人
        int totalPlayers = roomPlayerCounts.values().stream().mapToInt(Integer::intValue).sum();
        return (totalPlayers % totalRooms) + 1;
    }
    public static RegistryKey<World> getWorldByPath(MinecraftServer server, String path) {
        for (RegistryKey<World> key : server.getWorldRegistryKeys()) {
            Identifier id = key.getValue();
            if (id.getPath().equals(path)) {
                return key;
            }
        }
        return null;
    }
    public static BlockPos Vec3dToBlockPos(Vec3d vec3d) {
        return new BlockPos(
                (int) Math.round(vec3d.x),
                (int) Math.round(vec3d.y),
                (int) Math.round(vec3d.z)
        );

    }
    public static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor();

    public static void teleportPlayer(ServerPlayerEntity player) {
        MinecraftServer server = player.getServer();
        SCHEDULER.schedule(() -> {
            if (server == null) return;
            server.execute(() -> {
                RegistryKey<World> worldKey = RegistryKey.of(RegistryKeys.WORLD, MapVotingComponent.KEY.get(player.getServer().getScoreboard()).getLastSelectedDimension());
                if (worldKey == null) return;
                ServerWorld world = server.getWorld(worldKey);
                if (world == null) return;
                MapVariablesWorldComponent spawn = MapVariablesWorldComponent.KEY.get(world);
                MapVariablesWorldComponent.PosWithOrientation spawnPos = spawn.getSpawnPos();
                if(player.isSpectator()){
                    spawnPos = spawn.getSpectatorSpawnPos();
                }
                world.getChunk(Vec3dToBlockPos(spawnPos.pos));

                player.teleport(
                        world,
                        spawnPos.pos.getX() + 0.5,
                        spawnPos.pos.getY() + 1,
                        spawnPos.pos.getZ() + 0.5,
                        spawnPos.yaw,
                        spawnPos.pitch
                );
                player.setSpawnPoint(worldKey, new BlockPos((int) spawnPos.pos.getX(), (int) spawnPos.pos.getY(), (int) spawnPos.pos.getZ()), spawnPos.yaw, true, false);
                TrainVoicePlugin.resetPlayer(player.getUuid());
                player.getInventory().clear();
            });
        }, 100, TimeUnit.MILLISECONDS);
    }
    /**
     * 投票结束后传送所有玩家到目标维度
     */
    public static void finalizeVoting(ServerWorld currentWorld, Identifier targetDimensionId) {
        RegistryKey<World> dimKey = RegistryKey.of(RegistryKeys.WORLD, targetDimensionId);
        ServerWorld targetWorld = currentWorld.getServer().getWorld(dimKey);

        if (targetWorld == null) {
            Wathe.LOGGER.warn("Target dimension {} not found, staying in current world", targetDimensionId);
            return;
        }

        // Teleport all players from all worlds to the target dimension
        for (ServerWorld world : currentWorld.getServer().getWorlds()) {
            if (world.getRegistryKey().equals(dimKey)) continue; // Already in target
            for (ServerPlayerEntity player : new ArrayList<>(world.getPlayers())) {
                teleportPlayer(player);
            }
        }

        Wathe.LOGGER.info("Teleported all players to dimension {}", targetDimensionId);
    }

    public enum WinStatus {
        NONE, KILLERS, PASSENGERS, TIME, LOOSE_END, NEUTRAL
    }
}
