package dev.doctor4t.trainmurdermystery.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import dev.doctor4t.trainmurdermystery.cca.GameWorldComponent;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class SetKillerRatioCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                CommandManager.literal("tmm:setKillerRatio")
                        .requires(source -> source.hasPermissionLevel(2))
                        .then(
                                CommandManager.argument("ratio", IntegerArgumentType.integer(1))
                                        .executes(context -> execute(context.getSource(), IntegerArgumentType.getInteger(context, "ratio")))
                        )
        );
    }

    private static int execute(ServerCommandSource source, int ratio) {
        GameWorldComponent.KEY.get(source.getWorld()).setKillerPlayerRatio(ratio);
        source.sendFeedback(() -> Text.literal("Set killer-player ratio to ").formatted(Formatting.GRAY)
                .append(Text.literal("1:" + ratio).withColor(0x808080))
                .append(Text.literal(".").formatted(Formatting.GRAY)), false);
        return 1;
    }

}