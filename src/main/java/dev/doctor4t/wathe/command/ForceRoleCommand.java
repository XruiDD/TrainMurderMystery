package dev.doctor4t.wathe.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import dev.doctor4t.wathe.Wathe;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.WatheRoles;
import dev.doctor4t.wathe.cca.ScoreboardRoleSelectorComponent;
import dev.doctor4t.wathe.command.argument.RoleSuggestionProvider;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;

public class ForceRoleCommand {
    public static final SimpleCommandExceptionType INVALID_ROLE_EXCEPTION = new SimpleCommandExceptionType(Text.translatable("commands.wathe.forcerole.invalid"));

    public static void register(@NotNull CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("wathe:forceRole").requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.argument("player", EntityArgumentType.player())
                        .executes(context -> query(context.getSource(), EntityArgumentType.getPlayer(context, "player")))
                        .then(CommandManager.argument("role", StringArgumentType.string())
                                .suggests(new RoleSuggestionProvider())
                                .executes(context -> execute(context.getSource(), EntityArgumentType.getPlayer(context, "player"), StringArgumentType.getString(context, "role")))
                        )
                )
        );
    }

    private static int query(@NotNull ServerCommandSource source, @NotNull ServerPlayerEntity targetPlayer) {
        return Wathe.executeSupporterCommand(source, () -> {
            ScoreboardRoleSelectorComponent component = ScoreboardRoleSelectorComponent.KEY.get(source.getServer().getScoreboard());
            Role forcedRole = component.getForcedRoleForPlayer(targetPlayer.getUuid());
            if (forcedRole != null) {
                Text roleText = Text.literal(forcedRole.identifier().getPath()).withColor(forcedRole.color());
                source.sendFeedback(() -> Text.translatable("commands.wathe.forcerole.query", targetPlayer.getDisplayName(), roleText), false);
            } else {
                source.sendFeedback(() -> Text.translatable("commands.wathe.forcerole.query.none", targetPlayer.getDisplayName()), false);
            }
        });
    }

    private static int execute(@NotNull ServerCommandSource source, @NotNull ServerPlayerEntity targetPlayer, @NotNull String roleName) throws CommandSyntaxException {
        for (Role role : WatheRoles.ROLES) {
            if (WatheRoles.SPECIAL_ROLES.contains(role)) continue;
            if (role.identifier().getPath().equals(roleName)) {
                final Role finalRole = role;
                return Wathe.executeSupporterCommand(source, () -> {
                    ScoreboardRoleSelectorComponent component = ScoreboardRoleSelectorComponent.KEY.get(source.getServer().getScoreboard());
                    // Use addForcedRole to safely add the player, removing them from other forced roles first
                    component.addForcedRole(finalRole, targetPlayer.getUuid());
                    Text roleText = Text.literal(finalRole.identifier().getPath()).withColor(finalRole.color());
                    source.sendFeedback(() -> Text.translatable("commands.wathe.forcerole.success", roleText, targetPlayer.getDisplayName()), true);
                });
            }
        }
        throw INVALID_ROLE_EXCEPTION.create();
    }
}
