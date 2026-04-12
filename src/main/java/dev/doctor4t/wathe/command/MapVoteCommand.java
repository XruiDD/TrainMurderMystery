package dev.doctor4t.wathe.command;

import com.mojang.brigadier.CommandDispatcher;
import dev.doctor4t.wathe.cca.MapVotingComponent;
import dev.doctor4t.wathe.util.WathePermissions;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import org.jetbrains.annotations.NotNull;

public class MapVoteCommand {
    public static void register(@NotNull CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("wathe:mapvote")
                .requires(Permissions.require(WathePermissions.COMMAND_MAP_VOTE, WathePermissions.DEFAULT_COMMAND_LEVEL))
                .executes(context -> {
                    MapVotingComponent voting = MapVotingComponent.KEY.get(
                        context.getSource().getServer().getScoreboard());
                    if (voting.isVotingActive()) {
                        context.getSource().sendError(net.minecraft.text.Text.literal("Map voting is already active"));
                        return 0;
                    }
                    voting.startVoting();
                    return 1;
                })
        );
    }
}
