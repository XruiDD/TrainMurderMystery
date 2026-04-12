package dev.doctor4t.wathe.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import dev.doctor4t.wathe.Wathe;
import dev.doctor4t.wathe.WatheConfig;
import eu.midnightdust.lib.config.MidnightConfig;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

public class ConfigCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                CommandManager.literal("wathe:config")
                        .requires(source -> source.hasPermissionLevel(2))
                        .then(CommandManager.literal("reload")
                                .executes(context -> reloadConfig(context.getSource()))
                        )
                        .then(CommandManager.literal("set")
                                .then(CommandManager.literal("ignoreMapPlayerLimit")
                                        .then(CommandManager.argument("value", BoolArgumentType.bool())
                                                .executes(context -> setIgnoreMapPlayerLimit(
                                                        context.getSource(),
                                                        BoolArgumentType.getBool(context, "value")
                                                ))
                                        )
                                        .executes(context -> queryIgnoreMapPlayerLimit(context.getSource()))
                                )
                        )
        );
    }

    private static int reloadConfig(ServerCommandSource source) {
        WatheConfig.init(Wathe.MOD_ID, WatheConfig.class);
        source.sendFeedback(() -> Text.translatable("commands.wathe.config.reload.success"), true);
        return 1;
    }

    private static int setIgnoreMapPlayerLimit(ServerCommandSource source, boolean value) {
        WatheConfig.ignoreMapPlayerLimit = value;
        MidnightConfig.write(Wathe.MOD_ID);
        Text status = value
                ? Text.translatable("commands.wathe.config.set.enabled")
                : Text.translatable("commands.wathe.config.set.disabled");
        source.sendFeedback(() -> Text.translatable("commands.wathe.config.set.success", "ignoreMapPlayerLimit", status), true);
        return 1;
    }

    private static int queryIgnoreMapPlayerLimit(ServerCommandSource source) {
        Text status = WatheConfig.ignoreMapPlayerLimit
                ? Text.translatable("commands.wathe.config.set.enabled")
                : Text.translatable("commands.wathe.config.set.disabled");
        source.sendFeedback(() -> Text.translatable("commands.wathe.config.query", "ignoreMapPlayerLimit", status), false);
        return 1;
    }
}
