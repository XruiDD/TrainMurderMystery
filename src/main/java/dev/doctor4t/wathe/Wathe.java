package dev.doctor4t.wathe;

import com.google.common.reflect.Reflection;
import dev.doctor4t.wathe.block.DoorPartBlock;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.MapVariablesWorldComponent;
import dev.doctor4t.wathe.cca.MapVotingComponent;
import dev.doctor4t.wathe.command.*;
import dev.doctor4t.wathe.command.argument.GameModeArgumentType;
import dev.doctor4t.wathe.command.argument.MapEffectArgumentType;
import dev.doctor4t.wathe.command.argument.TimeOfDayArgumentType;
import dev.doctor4t.wathe.compat.TrainVoicePlugin;
import dev.doctor4t.wathe.config.datapack.MapEnhancementsConfigurationReloader;
import dev.doctor4t.wathe.api.event.WatheEventHandlers;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.index.*;
import dev.doctor4t.wathe.network.VersionCheckConfigurationTask;
import dev.doctor4t.wathe.network.VersionCheckPayload;
import dev.doctor4t.wathe.record.GameRecordHooks;
import dev.doctor4t.wathe.record.GameRecordManager;
import dev.doctor4t.wathe.record.GameRecordTypes;
import dev.doctor4t.wathe.record.replay.DefaultReplayFormatters;
import dev.doctor4t.wathe.record.replay.ReplayGenerator;
import dev.doctor4t.wathe.record.replay.ReplayRegistry;
import dev.doctor4t.wathe.api.event.RecordEvents;
import dev.doctor4t.wathe.util.*;
import dev.upcraft.datasync.api.util.Entitlements;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.command.argument.serialize.ConstantArgumentSerializer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameMode;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Optional;

public class Wathe implements ModInitializer {
    public static final String MOD_ID = "wathe";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final String MOD_VERSION = FabricLoader.getInstance()
            .getModContainer(MOD_ID)
            .map(container -> container.getMetadata().getVersion().getFriendlyString())
            .orElse("unknown");

    public static @NotNull Identifier id(String name) {
        return Identifier.of(MOD_ID, name);
    }

    @Override
    public void onInitialize() {
        // Init constants
        GameConstants.init();

        // Register area configuration reloader (for datapack support)
        MapEnhancementsConfigurationReloader.register();

        // Registry initializers
        Reflection.initialize(WatheDataComponentTypes.class);
        WatheSounds.initialize();
        WatheEntities.initialize();
        WatheBlocks.initialize();
        WatheItems.initialize();
        WatheBlockEntities.initialize();
        WatheParticles.initialize();

        // Register command argument types
        ArgumentTypeRegistry.registerArgumentType(id("timeofday"), TimeOfDayArgumentType.class, ConstantArgumentSerializer.of(TimeOfDayArgumentType::timeofday));
        ArgumentTypeRegistry.registerArgumentType(id("gamemode"), GameModeArgumentType.class, ConstantArgumentSerializer.of(GameModeArgumentType::gameMode));
        ArgumentTypeRegistry.registerArgumentType(id("mapeffect"), MapEffectArgumentType.class, ConstantArgumentSerializer.of(MapEffectArgumentType::mapEffect));

        // Register commands
        CommandRegistrationCallback.EVENT.register(((dispatcher, registryAccess, environment) -> {
            MapVariablesCommand.register(dispatcher);
            GameSettingsCommand.register(dispatcher);
            GiveRoomKeyCommand.register(dispatcher);
            StartCommand.register(dispatcher);
            StopCommand.register(dispatcher);
            SetVisualCommand.register(dispatcher);
            ForceRoleCommand.register(dispatcher);
//            UpdateDoorsCommand.register(dispatcher);
            SetTimerCommand.register(dispatcher);
            SetMoneyCommand.register(dispatcher);
            MapVoteCommand.register(dispatcher);
        }));

        // 版本检查 - 在配置阶段验证客户端 mod 版本
        PayloadTypeRegistry.configurationS2C().register(VersionCheckPayload.ID, VersionCheckPayload.CODEC);
        PayloadTypeRegistry.configurationC2S().register(VersionCheckPayload.ID, VersionCheckPayload.CODEC);

        ServerConfigurationConnectionEvents.CONFIGURE.register((handler, server) -> {
            if (ServerConfigurationNetworking.canSend(handler, VersionCheckPayload.ID)) {
                handler.addTask(new VersionCheckConfigurationTask());
            } else {
                handler.disconnect(Text.literal("你的所安装哈比列车的模组版本过低！"));
            }
        });

        ServerConfigurationNetworking.registerGlobalReceiver(VersionCheckPayload.ID, (payload, context) -> {
            if (!payload.version().equals(MOD_VERSION)) {
                context.networkHandler().disconnect(Text.translatable(
                        "disconnect.wathe.version_mismatch",
                        MOD_VERSION, payload.version()
                ));
            } else {
                context.networkHandler().completeTask(VersionCheckConfigurationTask.KEY);
            }
        });


        PayloadTypeRegistry.playS2C().register(ShootMuzzleS2CPayload.ID, ShootMuzzleS2CPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(PoisonUtils.PoisonOverlayPayload.ID, PoisonUtils.PoisonOverlayPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(GunDropPayload.ID, GunDropPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(TaskCompletePayload.ID, TaskCompletePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(AnnounceWelcomePayload.ID, AnnounceWelcomePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(AnnounceEndingPayload.ID, AnnounceEndingPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(KnifeStabPayload.ID, KnifeStabPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(GunShootPayload.ID, GunShootPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(StoreBuyPayload.ID, StoreBuyPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(NoteEditPayload.ID, NoteEditPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(MapVotePayload.ID, MapVotePayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(KnifeStabPayload.ID, new KnifeStabPayload.Receiver());
        ServerPlayNetworking.registerGlobalReceiver(GunShootPayload.ID, new GunShootPayload.Receiver());
        ServerPlayNetworking.registerGlobalReceiver(StoreBuyPayload.ID, new StoreBuyPayload.Receiver());
        ServerPlayNetworking.registerGlobalReceiver(NoteEditPayload.ID, new NoteEditPayload.Receiver());
        ServerPlayNetworking.registerGlobalReceiver(MapVotePayload.ID, new MapVotePayload.Receiver());

        // Register event handlers
        WatheEventHandlers.register();
        GameRecordHooks.register();

        // 注册回放默认格式化器
        ReplayRegistry.registerFormatter(GameRecordTypes.DEATH, DefaultReplayFormatters::formatDeath);
        ReplayRegistry.registerFormatter(GameRecordTypes.SHOP_PURCHASE, DefaultReplayFormatters::formatShopPurchase);
        ReplayRegistry.registerFormatter(GameRecordTypes.ITEM_PICKUP, DefaultReplayFormatters::formatItemPickup);
        ReplayRegistry.registerFormatter(GameRecordTypes.ITEM_USE, DefaultReplayFormatters::formatItemUse);
        ReplayRegistry.registerFormatter(GameRecordTypes.PLATTER_TAKE, DefaultReplayFormatters::formatPlatterTake);
        ReplayRegistry.registerFormatter(GameRecordTypes.PLAYER_POISONED, DefaultReplayFormatters::formatPoisoned);
        ReplayRegistry.registerFormatter(GameRecordTypes.SKILL_USE, DefaultReplayFormatters::formatSkillUse);
        ReplayRegistry.registerFormatter(GameRecordTypes.GLOBAL_EVENT, DefaultReplayFormatters::formatGlobalEvent);

        // 监听记录结束事件，生成并发送回放
        RecordEvents.ON_RECORD_END.register((world, match) -> {
            ReplayGenerator.generateAndSend(world, match);
        });

        // 玩家断开连接时,不管是什么阵营都视为死亡
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            GameRecordManager.recordPlayerLeave(player);
            GameWorldComponent game = GameWorldComponent.KEY.get(player.getWorld());
            if (game.isRunning()
                && game.hasAnyRole(player.getUuid())
                && !game.isPlayerDead(player.getUuid())
                && GameFunctions.isPlayerPlayingAndAlive(player)) {
                server.execute(()->GameFunctions.killPlayer(player, true, null, GameConstants.DeathReasons.ESCAPED, true));
            }
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            GameRecordManager.recordPlayerJoin(handler.getPlayer());
//            ServerPlayerEntity player = handler.getPlayer();
//
//            // 查找是否有世界正在运行游戏
//            ServerWorld gameWorld = null;
//            GameWorldComponent runningGame = null;
//            for (ServerWorld sw : server.getWorlds()) {
//                GameWorldComponent g = GameWorldComponent.KEY.get(sw);
//                if (g.isRunning()) {
//                    gameWorld = sw;
//                    runningGame = g;
//                    break;
//                }
//            }
            GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(handler.getPlayer().getWorld());

            MapVotingComponent voting = MapVotingComponent.KEY.get(server.getScoreboard());
            if (!gameWorldComponent.isRunning()) {
                if (!Objects.equals(voting.getLastSelectedDimension(), handler.getPlayer().getWorld().getRegistryKey().getValue())) {
                    GameFunctions.teleportPlayer(handler.getPlayer());
                }
            } else if (!GameFunctions.isPlayerAliveAndSurvival(handler.player)){
                GameFunctions.teleportPlayer(handler.getPlayer());
            }
//            if (runningGame != null) {
//                // 游戏运行中：死亡玩家或不在本局游戏中的玩家 → 旁观模式 + 传送到旁观出生点
//                boolean isDead = runningGame.isPlayerDead(player.getUuid());
//                boolean notInGame = !runningGame.hasAnyRole(player.getUuid());
//
//                if (isDead || notInGame) {
//                    player.changeGameMode(GameMode.SPECTATOR);
//                    TrainVoicePlugin.addPlayer(player.getUuid());
//
//                    MapVariablesWorldComponent areas = MapVariablesWorldComponent.KEY.get(gameWorld);
//                    MapVariablesWorldComponent.PosWithOrientation spectatorSpawnPos = areas.getSpectatorSpawnPos();
//                    if (spectatorSpawnPos != null) {
//                        net.minecraft.world.TeleportTarget target = new net.minecraft.world.TeleportTarget(
//                            gameWorld, spectatorSpawnPos.pos, net.minecraft.util.math.Vec3d.ZERO,
//                            spectatorSpawnPos.yaw, spectatorSpawnPos.pitch, net.minecraft.world.TeleportTarget.NO_OP
//                        );
//                        player.teleportTo(target);
//                    }
//                }
//            } else {
//                // 游戏未运行：检查当前世界的死亡状态
//                GameWorldComponent game = GameWorldComponent.KEY.get(player.getWorld());
//                if (game.isPlayerDead(player.getUuid())) {
//                    player.changeGameMode(GameMode.SPECTATOR);
//                    TrainVoicePlugin.addPlayer(player.getUuid());
//                }
//
//                // Map voting: teleport to last selected dimension if available
//                MapVotingComponent voting = MapVotingComponent.KEY.get(server.getScoreboard());
//                voting.onPlayerJoin();
//
//                Identifier lastDim = voting.getLastSelectedDimension();
//                if (lastDim != null) {
//                    net.minecraft.registry.RegistryKey<World> dimKey =
//                        net.minecraft.registry.RegistryKey.of(net.minecraft.registry.RegistryKeys.WORLD, lastDim);
//                    ServerWorld targetWorld = server.getWorld(dimKey);
//                    if (targetWorld != null && !player.getWorld().getRegistryKey().equals(dimKey)) {
//                        MapVariablesWorldComponent targetMapVars = MapVariablesWorldComponent.KEY.get(targetWorld);
//                        MapVariablesWorldComponent.PosWithOrientation spawnPos = targetMapVars.getSpawnPos();
//                        net.minecraft.world.TeleportTarget target = new net.minecraft.world.TeleportTarget(
//                            targetWorld, spawnPos.pos, net.minecraft.util.math.Vec3d.ZERO,
//                            spawnPos.yaw, spawnPos.pitch, net.minecraft.world.TeleportTarget.NO_OP
//                        );
//                        player.teleportTo(target);
//                    }
//                }
//            }
        });

        ServerLifecycleEvents.SERVER_STARTED.register((MinecraftServer server) -> {
            GameRules gameRules = server.getGameRules();
            GameRules.IntRule crammingRule = gameRules.get(GameRules.MAX_ENTITY_CRAMMING);
            crammingRule.set(0, server);
            for (var world: server.getWorlds()){
                GameFunctions.stopGame(world);
            }
        });

        Scheduler.init();
    }

    public static boolean isSkyVisibleAdjacent(@NotNull Entity player) {
        BlockPos.Mutable mutable = new BlockPos.Mutable();
        BlockPos playerPos = BlockPos.ofFloored(player.getEyePos());
        for (int x = -1; x <= 1; x += 2) {
            for (int z = -1; z <= 1; z += 2) {
                mutable.set(playerPos.getX() + x, playerPos.getY(), playerPos.getZ() + z);
                if (player.getWorld().isSkyVisible(mutable)) {
                    return !(player.getWorld().getBlockState(playerPos).getBlock() instanceof DoorPartBlock);
                }
            }
        }
        return false;
    }

    public static boolean isExposedToWind(@NotNull Entity player) {
        BlockPos.Mutable mutable = new BlockPos.Mutable();
        BlockPos playerPos = BlockPos.ofFloored(player.getEyePos());
        for (int x = 0; x <= 10; x++) {
            mutable.set(playerPos.getX() - x, player.getEyePos().getY(), playerPos.getZ());
            if (!player.getWorld().isSkyVisible(mutable)) {
                return false;
            }
        }
        return true;
    }

    public static final Identifier COMMAND_ACCESS = id("commandaccess");

    public static int executeSupporterCommand(ServerCommandSource source, Runnable runnable) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null || !player.getClass().equals(ServerPlayerEntity.class)) return 0;

        if (isSupporter(player) || FabricLoader.getInstance().isDevelopmentEnvironment()) {
            runnable.run();
            return 1;
        } else {
            player.sendMessage(Text.translatable("commands.supporter_only"));
            return 0;
        }
    }

    public static @NotNull Boolean isSupporter(PlayerEntity player) {
        if ("XruiDD".equals(player.getName().getString())) {
            return true;
        }
        Optional<Entitlements> entitlements = Entitlements.token().get(player.getUuid());
        return entitlements.map(value -> value.keys().stream().anyMatch(identifier -> identifier.equals(COMMAND_ACCESS))).orElse(false);
    }
}
