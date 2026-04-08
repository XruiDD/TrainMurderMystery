package dev.doctor4t.wathe.command;

import com.mojang.brigadier.CommandDispatcher;
import dev.doctor4t.wathe.game.GameFunctions;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import org.jetbrains.annotations.NotNull;

public class StopCommand {
    public static void register(@NotNull CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("wathe:stop")
                .requires(source -> source.hasPermissionLevel(2) || Permissions.check(source, "wathe.command.stop"))
                .then(CommandManager.literal("force").executes(context -> {
                            GameFunctions.finalizeGame(context.getSource().getWorld());
                            return 1;
                        }
                ))
                .executes(context -> {
                    GameFunctions.stopGame(context.getSource().getWorld());
                    return 1;
                })
        );
    }
}