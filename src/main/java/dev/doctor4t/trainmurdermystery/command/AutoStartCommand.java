package dev.doctor4t.trainmurdermystery.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import dev.doctor4t.trainmurdermystery.TMM;
import dev.doctor4t.trainmurdermystery.api.GameMode;
import dev.doctor4t.trainmurdermystery.api.TMMGameModes;
import dev.doctor4t.trainmurdermystery.cca.AutoStartComponent;
import dev.doctor4t.trainmurdermystery.command.argument.GameModeArgumentType;
import dev.doctor4t.trainmurdermystery.config.TMMServerConfig;
import dev.doctor4t.trainmurdermystery.game.GameConstants;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

public class AutoStartCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                CommandManager.literal("tmm:autoStart")
                        .requires(source -> source.hasPermissionLevel(2))
                        .then(CommandManager.argument("gameMode", GameModeArgumentType.gameMode())
                                .then(CommandManager.argument("seconds", IntegerArgumentType.integer(0, 60))
                                        .executes(context -> setAutoStart(
                                                context.getSource(),
                                                GameModeArgumentType.getGameModeArgument(context, "gameMode"),
                                                IntegerArgumentType.getInteger(context, "seconds")
                                        ))
                                )
                                .executes(context -> setAutoStart(
                                        context.getSource(),
                                        GameModeArgumentType.getGameModeArgument(context, "gameMode"),
                                        30
                                ))
                        )
        );
    }

    private static int setAutoStart(ServerCommandSource source, GameMode gameMode, int seconds) {
        if (gameMode == TMMGameModes.LOOSE_ENDS || gameMode == TMMGameModes.DISCOVERY) {
            return TMM.executeSupporterCommand(source, () -> {
                AutoStartComponent component = AutoStartComponent.KEY.get(source.getWorld());
                component.setGameMode(gameMode);
                component.setStartTime(GameConstants.getInTicks(0, seconds));
                // 保存到配置文件
                TMMServerConfig.HANDLER.instance().autoStartGameMode = gameMode.identifier.toString();
                TMMServerConfig.HANDLER.instance().autoStartSeconds = seconds;
                TMMServerConfig.HANDLER.save();
            });
        } else {
            AutoStartComponent component = AutoStartComponent.KEY.get(source.getWorld());
            component.setGameMode(gameMode);
            component.setStartTime(GameConstants.getInTicks(0, seconds));
            // 保存到配置文件
            TMMServerConfig.HANDLER.instance().autoStartGameMode = gameMode.identifier.toString();
            TMMServerConfig.HANDLER.instance().autoStartSeconds = seconds;
            TMMServerConfig.HANDLER.save();
            return 1;
        }
    }
}
