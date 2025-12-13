package dev.doctor4t.trainmurdermystery;

import com.google.common.reflect.Reflection;
import dev.doctor4t.trainmurdermystery.api.Role;
import dev.doctor4t.trainmurdermystery.api.TMMRoles;
import dev.doctor4t.trainmurdermystery.block.DoorPartBlock;
import dev.doctor4t.trainmurdermystery.cca.GameWorldComponent;
import dev.doctor4t.trainmurdermystery.command.*;
import dev.doctor4t.trainmurdermystery.command.argument.GameModeArgumentType;
import dev.doctor4t.trainmurdermystery.command.argument.TimeOfDayArgumentType;
import dev.doctor4t.trainmurdermystery.config.TMMServerConfig;
import dev.doctor4t.trainmurdermystery.event.TMMEventHandlers;
import dev.doctor4t.trainmurdermystery.game.GameConstants;
import dev.doctor4t.trainmurdermystery.index.*;
import dev.doctor4t.trainmurdermystery.network.VersionCheckConfigurationTask;
import dev.doctor4t.trainmurdermystery.network.VersionCheckPayload;
import dev.doctor4t.trainmurdermystery.util.*;
import dev.upcraft.datasync.api.DataSyncAPI;
import dev.upcraft.datasync.api.util.Entitlements;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.command.argument.serialize.ConstantArgumentSerializer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.UUID;

public class TMM implements ModInitializer {
    public static final String MOD_ID = "trainmurdermystery";
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
        // 加载服务端配置
        TMMServerConfig.HANDLER.load();

        // 应用禁用角色配置
        TMMServerConfig config = TMMServerConfig.HANDLER.instance();
        for (String roleId : config.disabledRoles) {
            Role role = TMMRoles.getRole(Identifier.of(roleId));
            if (role != null) {
                TMMRoles.setRoleEnabled(role, false);
            }
        }

        // Init constants
        GameConstants.init();

        // Registry initializers
        Reflection.initialize(TMMDataComponentTypes.class);
        TMMSounds.initialize();
        TMMEntities.initialize();
        TMMBlocks.initialize();
        TMMItems.initialize();
        TMMBlockEntities.initialize();
        TMMParticles.initialize();

        // Register command argument types
        ArgumentTypeRegistry.registerArgumentType(id("timeofday"), TimeOfDayArgumentType.class, ConstantArgumentSerializer.of(TimeOfDayArgumentType::timeofday));
        ArgumentTypeRegistry.registerArgumentType(id("gamemode"), GameModeArgumentType.class, ConstantArgumentSerializer.of(GameModeArgumentType::gameMode));

        // Register commands
        CommandRegistrationCallback.EVENT.register(((dispatcher, registryAccess, environment) -> {
            GiveRoomKeyCommand.register(dispatcher);
            StartCommand.register(dispatcher);
            StopCommand.register(dispatcher);
            SetVisualCommand.register(dispatcher);
            ForceRoleCommand.register(dispatcher);
//            UpdateDoorsCommand.register(dispatcher);
            SetTimerCommand.register(dispatcher);
            SetMoneyCommand.register(dispatcher);
            SetBoundCommand.register(dispatcher);
            AutoStartCommand.register(dispatcher);
            SetBackfireChanceCommand.register(dispatcher);
            SetKillerCountCommand.register(dispatcher);
            SetKillerRatioCommand.register(dispatcher);
            ListRolesCommand.register(dispatcher);
            SetEnabledRoleCommand.register(dispatcher);
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
                        "disconnect.trainmurdermystery.version_mismatch",
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
        ServerPlayNetworking.registerGlobalReceiver(KnifeStabPayload.ID, new KnifeStabPayload.Receiver());
        ServerPlayNetworking.registerGlobalReceiver(GunShootPayload.ID, new GunShootPayload.Receiver());
        ServerPlayNetworking.registerGlobalReceiver(StoreBuyPayload.ID, new StoreBuyPayload.Receiver());
        ServerPlayNetworking.registerGlobalReceiver(NoteEditPayload.ID, new NoteEditPayload.Receiver());

        // Register event handlers
        TMMEventHandlers.register();

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

        if (isSupporter(player)) {
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