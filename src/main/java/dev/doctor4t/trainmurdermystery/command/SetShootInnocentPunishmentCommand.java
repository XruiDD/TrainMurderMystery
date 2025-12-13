package dev.doctor4t.trainmurdermystery.command;

import com.mojang.brigadier.CommandDispatcher;
import dev.doctor4t.trainmurdermystery.TMM;
import dev.doctor4t.trainmurdermystery.cca.GameWorldComponent;
import dev.doctor4t.trainmurdermystery.config.TMMServerConfig;
import dev.doctor4t.trainmurdermystery.config.TMMServerConfig.ShootInnocentPunishment;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

public class SetShootInnocentPunishmentCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                CommandManager.literal("tmm:setShootInnocentPunishment")
                        .requires(source -> source.hasPermissionLevel(2))
                        .then(CommandManager.literal("default")
                                .executes(context -> execute(context.getSource(), ShootInnocentPunishment.DEFAULT))
                        )
                        .then(CommandManager.literal("preventGunPickup")
                                .executes(context -> execute(context.getSource(), ShootInnocentPunishment.PREVENT_GUN_PICKUP))
                        )
                        .then(CommandManager.literal("killShooter")
                                .executes(context -> execute(context.getSource(), ShootInnocentPunishment.KILL_SHOOTER))
                        )
        );
    }

    private static int execute(ServerCommandSource source, ShootInnocentPunishment punishment) {
        return TMM.executeSupporterCommand(source, () -> {
            GameWorldComponent.KEY.get(source.getWorld()).setShootInnocentPunishment(punishment);
            // 保存到配置文件
            TMMServerConfig.HANDLER.instance().shootInnocentPunishment = punishment;
            TMMServerConfig.HANDLER.save();

            String punishmentName = switch (punishment) {
                case DEFAULT -> "默认 (掉落枪支)";
                case PREVENT_GUN_PICKUP -> "禁止拾取枪支";
                case KILL_SHOOTER -> "击杀射击者";
            };
            source.sendMessage(Text.literal("已设置射杀无辜惩罚为: " + punishmentName));
        });
    }
}
