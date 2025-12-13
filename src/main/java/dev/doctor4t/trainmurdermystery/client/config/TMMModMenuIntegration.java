package dev.doctor4t.trainmurdermystery.client.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.doctor4t.ratatouille.client.util.OptionLocker;
import dev.doctor4t.trainmurdermystery.api.TMMGameModes;
import dev.doctor4t.trainmurdermystery.api.TMMRoles;
import dev.doctor4t.trainmurdermystery.client.TMMClient;
import dev.doctor4t.trainmurdermystery.config.TMMClientConfig;
import dev.doctor4t.trainmurdermystery.config.TMMServerConfig;
import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.util.List;
import java.util.stream.Collectors;

public class TMMModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parentScreen -> YetAnotherConfigLib.createBuilder()
            .title(Text.literal("Train Murder Mystery Config"))

            // 客户端配置类别
            .category(ConfigCategory.createBuilder()
                .name(Text.translatable("config.trainmurdermystery.client"))
                .tooltip(Text.translatable("config.trainmurdermystery.client.tooltip"))

                // 超级性能模式
                .option(Option.<Boolean>createBuilder()
                    .name(Text.translatable("config.trainmurdermystery.ultra_perf_mode"))
                    .description(OptionDescription.of(Text.translatable("config.trainmurdermystery.ultra_perf_mode.desc")))
                    .binding(
                        false,
                        () -> TMMClientConfig.HANDLER.instance().ultraPerfMode,
                        val -> {
                            TMMClientConfig.HANDLER.instance().ultraPerfMode = val;
                            // 应用渲染距离设置
                            int lockedRenderDistance = TMMClient.getLockedRenderDistance(val);
                            OptionLocker.overrideOption("renderDistance", lockedRenderDistance);
                            MinecraftClient.getInstance().options.getViewDistance().setValue(lockedRenderDistance);
                        }
                    )
                    .controller(TickBoxControllerBuilder::create)
                    .build())
                .build())

            // 服务端配置类别
            .category(ConfigCategory.createBuilder()
                .name(Text.translatable("config.trainmurdermystery.server"))
                .tooltip(Text.translatable("config.trainmurdermystery.server.tooltip"))

                // 默认backfire机率
                .option(Option.<Float>createBuilder()
                    .name(Text.translatable("config.trainmurdermystery.backfire_chance"))
                    .description(OptionDescription.of(Text.translatable("config.trainmurdermystery.backfire_chance.desc")))
                    .binding(
                        0f,
                        () -> TMMServerConfig.HANDLER.instance().backfireChance,
                        val -> TMMServerConfig.HANDLER.instance().backfireChance = val
                    )
                    .controller(opt -> FloatSliderControllerBuilder.create(opt)
                        .range(0f, 1f)
                        .step(0.05f))
                    .build())

                // 杀手比例
                .option(Option.<Integer>createBuilder()
                    .name(Text.translatable("config.trainmurdermystery.killer_ratio"))
                    .description(OptionDescription.of(Text.translatable("config.trainmurdermystery.killer_ratio.desc")))
                    .binding(
                        6,
                        () -> TMMServerConfig.HANDLER.instance().killerRatio,
                        val -> TMMServerConfig.HANDLER.instance().killerRatio = val
                    )
                    .controller(opt -> IntegerSliderControllerBuilder.create(opt)
                        .range(1, 20)
                        .step(1))
                    .build())

                // 杀手数量
                .option(Option.<Integer>createBuilder()
                    .name(Text.translatable("config.trainmurdermystery.killer_count"))
                    .description(OptionDescription.of(Text.translatable("config.trainmurdermystery.killer_count.desc")))
                    .binding(
                        0,
                        () -> TMMServerConfig.HANDLER.instance().killerCount,
                        val -> TMMServerConfig.HANDLER.instance().killerCount = val
                    )
                    .controller(opt -> IntegerFieldControllerBuilder.create(opt)
                        .min(0).max(10))
                    .build())

                // 自动开始倒计时
                .option(Option.<Integer>createBuilder()
                    .name(Text.translatable("config.trainmurdermystery.autostart_seconds"))
                    .description(OptionDescription.of(Text.translatable("config.trainmurdermystery.autostart_seconds.desc")))
                    .binding(
                        0,
                        () -> TMMServerConfig.HANDLER.instance().autoStartSeconds,
                        val -> TMMServerConfig.HANDLER.instance().autoStartSeconds = val
                    )
                    .controller(opt -> IntegerFieldControllerBuilder.create(opt)
                        .min(0).max(60))
                    .build())

                // 自动开始游戏模式
                .option(Option.<String>createBuilder()
                    .name(Text.translatable("config.trainmurdermystery.autostart_gamemode"))
                    .description(OptionDescription.of(Text.translatable("config.trainmurdermystery.autostart_gamemode.desc")))
                    .binding(
                        "trainmurdermystery:murder",
                        () -> TMMServerConfig.HANDLER.instance().autoStartGameMode,
                        val -> TMMServerConfig.HANDLER.instance().autoStartGameMode = val
                    )
                    .controller(opt -> DropdownStringControllerBuilder.create(opt)
                        .values(TMMGameModes.GAME_MODES.keySet().stream()
                            .map(id -> id.toString())
                            .collect(Collectors.toList())))
                    .build())

                // 边界限制
                .option(Option.<Boolean>createBuilder()
                    .name(Text.translatable("config.trainmurdermystery.bound"))
                    .description(OptionDescription.of(Text.translatable("config.trainmurdermystery.bound.desc")))
                    .binding(
                        true,
                        () -> TMMServerConfig.HANDLER.instance().bound,
                        val -> TMMServerConfig.HANDLER.instance().bound = val
                    )
                    .controller(TickBoxControllerBuilder::create)
                    .build())

                // 射杀无辜惩罚
                .option(Option.<TMMServerConfig.ShootInnocentPunishment>createBuilder()
                    .name(Text.translatable("config.trainmurdermystery.shoot_innocent_punishment"))
                    .description(OptionDescription.of(Text.translatable("config.trainmurdermystery.shoot_innocent_punishment.desc")))
                    .binding(
                        TMMServerConfig.ShootInnocentPunishment.VANILLA,
                        () -> TMMServerConfig.HANDLER.instance().shootInnocentPunishment,
                        val -> TMMServerConfig.HANDLER.instance().shootInnocentPunishment = val
                    )
                    .controller(opt -> EnumControllerBuilder.create(opt)
                        .enumClass(TMMServerConfig.ShootInnocentPunishment.class))
                    .build())

                // 禁用角色列表
                .group(ListOption.<String>createBuilder()
                    .name(Text.translatable("config.trainmurdermystery.disabled_roles"))
                    .description(OptionDescription.of(Text.translatable("config.trainmurdermystery.disabled_roles.desc")))
                    .binding(
                        List.of(),
                        () -> TMMServerConfig.HANDLER.instance().disabledRoles,
                        val -> TMMServerConfig.HANDLER.instance().disabledRoles = val
                    )
                    .controller(opt -> DropdownStringControllerBuilder.create(opt)
                        .values(TMMRoles.ROLES.stream()
                            .filter(role -> !TMMRoles.SPECIAL_ROLES.contains(role))
                            .map(role -> role.identifier().toString())
                            .collect(Collectors.toList())))
                    .initial("")
                    .build())

                .build())

            .save(() -> {
                // 保存客户端配置
                TMMClientConfig.HANDLER.save();
                // 保存服务端配置
                TMMServerConfig.HANDLER.save();
                // 应用禁用角色配置
                TMMRoles.applyDisabledRolesFromConfig();
            })
            .build()
            .generateScreen(parentScreen);
    }
}
