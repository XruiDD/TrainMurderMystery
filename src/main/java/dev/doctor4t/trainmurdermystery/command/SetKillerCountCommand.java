package dev.doctor4t.trainmurdermystery.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import dev.doctor4t.trainmurdermystery.cca.GameWorldComponent;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class SetKillerCountCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                CommandManager.literal("tmm:setKillerCount")
                        .requires(source -> source.hasPermissionLevel(2))
                        .then(
                                CommandManager.argument("count", IntegerArgumentType.integer(0))
                                        .executes(context -> execute(context.getSource(), IntegerArgumentType.getInteger(context, "count")))
                        )
        );
    }

    private static int execute(ServerCommandSource source, int count) {
        GameWorldComponent.KEY.get(source.getWorld()).setNextRoundKillerCount(count);
        source.sendFeedback(() -> Text.literal("Set next round killer count to ").formatted(Formatting.GRAY)
                .append(Text.literal("%d".formatted(count)).withColor(0x808080))
                .append(Text.literal(".").formatted(Formatting.GRAY)), false);
        return 1;
    }
}