package dev.doctor4t.trainmurdermystery.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import dev.doctor4t.trainmurdermystery.TMM;
import dev.doctor4t.trainmurdermystery.cca.TrainWorldComponent;
import dev.doctor4t.trainmurdermystery.command.argument.TimeOfDayArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

public class SetVisualCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("tmm:setVisual")
                .requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.literal("snow")
                        .then(CommandManager.argument("enabled", BoolArgumentType.bool())
                                .executes(context -> executeSnow(context.getSource(), BoolArgumentType.getBool(context, "enabled")))))
                .then(CommandManager.literal("fog")
                        .then(CommandManager.argument("enabled", BoolArgumentType.bool())
                                .executes(context -> executeFog(context.getSource(), BoolArgumentType.getBool(context, "enabled")))))
                .then(CommandManager.literal("hud")
                        .then(CommandManager.argument("enabled", BoolArgumentType.bool())
                                .executes(context -> executeHud(context.getSource(), BoolArgumentType.getBool(context, "enabled")))))
                .then(CommandManager.literal("trainSpeed")
                        .then(CommandManager.argument("speed", IntegerArgumentType.integer(0))
                                .executes(context -> executeSpeed(context.getSource(), IntegerArgumentType.getInteger(context, "speed")))))
                .then(CommandManager.literal("time")
                        .then(CommandManager.argument("timeOfDay", TimeOfDayArgumentType.timeofday())
                                .executes(context -> executeTime(context.getSource(), TimeOfDayArgumentType.getTimeofday(context, "timeOfDay")))))
                .then(CommandManager.literal("reset")
                        .executes(context -> reset(context.getSource())))
        );
    }

    private static int reset(ServerCommandSource source) {
        TrainWorldComponent trainWorldComponent = TrainWorldComponent.KEY.get(source.getWorld());
        trainWorldComponent.reset();
        return 1;
    }

    private static int executeSnow(ServerCommandSource source, boolean value) {
        return TMM.executeSupporterCommand(source, () -> {
            TrainWorldComponent.KEY.get(source.getWorld()).setSnow(value);
        });
    }

    private static int executeFog(ServerCommandSource source, boolean value) {
        return TMM.executeSupporterCommand(source, () -> {
            TrainWorldComponent.KEY.get(source.getWorld()).setFog(value);
        });
    }

    private static int executeHud(ServerCommandSource source, boolean value) {
        return TMM.executeSupporterCommand(source, () -> {
            TrainWorldComponent.KEY.get(source.getWorld()).setHud(value);
        });
    }

    private static int executeSpeed(ServerCommandSource source, int value) {
        return TMM.executeSupporterCommand(source, () -> {
            TrainWorldComponent.KEY.get(source.getWorld()).setSpeed(value);
        });
    }

    private static int executeTime(ServerCommandSource source, TrainWorldComponent.TimeOfDay value) {
        return TMM.executeSupporterCommand(source, () -> {
            TrainWorldComponent.KEY.get(source.getWorld()).setTimeOfDay(value);
        });
    }
}
