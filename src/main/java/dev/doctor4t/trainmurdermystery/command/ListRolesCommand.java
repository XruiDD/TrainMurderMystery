package dev.doctor4t.trainmurdermystery.command;

import com.mojang.brigadier.CommandDispatcher;
import dev.doctor4t.trainmurdermystery.api.Faction;
import dev.doctor4t.trainmurdermystery.api.Role;
import dev.doctor4t.trainmurdermystery.api.TMMRoles;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;

public class ListRolesCommand {
    private static final int CIVILIAN_COLOR = 0x36E51B;
    private static final int KILLER_COLOR = 0xC13838;
    private static final int NEUTRAL_COLOR = 0x9F9F9F;
    private static final int ENABLED_COLOR = 0x55FF55;
    private static final int DISABLED_COLOR = 0xFF5555;

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("tmm:listRoles")
                .executes(context -> execute(context.getSource())));
    }

    private static int execute(ServerCommandSource source) {
        MutableText message = Text.literal("Roles:").withColor(Colors.GRAY);

        for (Role role : TMMRoles.ROLES) {
            message.append("\n");
            String roleName = role.identifier().getPath();
            Faction faction = role.getFaction();

            // Faction tag
            Text factionTag = switch (faction) {
                case CIVILIAN -> Text.literal("[Civilian] ").withColor(CIVILIAN_COLOR);
                case KILLER -> Text.literal("[Killer] ").withColor(KILLER_COLOR);
                case NEUTRAL -> Text.literal("[Neutral] ").withColor(NEUTRAL_COLOR);
            };
            message.append(factionTag);

            // Role name
            message.append(Text.literal(roleName).withColor(role.color()));

            // Special tag
            if (TMMRoles.SPECIAL_ROLES.contains(role)) {
                message.append(Text.literal(" (special)").withColor(Colors.LIGHT_GRAY));
            }

            // Enabled/Disabled status
            if (!TMMRoles.SPECIAL_ROLES.contains(role)) {
                if (TMMRoles.isRoleEnabled(role)) {
                    message.append(Text.literal(" [ON]").withColor(ENABLED_COLOR));
                } else {
                    message.append(Text.literal(" [OFF]").withColor(DISABLED_COLOR));
                }
            }
        }

        source.sendMessage(message);
        return 1;
    }
}
