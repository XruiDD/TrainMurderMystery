package dev.doctor4t.wathe.command;

import com.mojang.brigadier.CommandDispatcher;
import dev.doctor4t.wathe.cca.MapVotingComponent;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import org.jetbrains.annotations.NotNull;

public class MapVoteCommand {
    public static void register(@NotNull CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("wathe:mapvote")
                .requires(source -> source.hasPermissionLevel(2))
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
