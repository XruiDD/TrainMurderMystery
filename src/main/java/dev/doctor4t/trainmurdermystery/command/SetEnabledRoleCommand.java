package dev.doctor4t.trainmurdermystery.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import dev.doctor4t.trainmurdermystery.api.Role;
import dev.doctor4t.trainmurdermystery.api.TMMRoles;
import dev.doctor4t.trainmurdermystery.command.argument.RoleSuggestionProvider;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;

public class SetEnabledRoleCommand {
    public static final SimpleCommandExceptionType INVALID_ROLE_EXCEPTION = new SimpleCommandExceptionType(Text.translatable("commands.trainmurdermystery.setenabledrole.invalid"));

    public static void register(@NotNull CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("tmm:setEnabledRole")
                .requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.argument("role", StringArgumentType.string())
                        .suggests(new RoleSuggestionProvider())
                        .then(CommandManager.argument("enabled", BoolArgumentType.bool())
                                .executes(context -> execute(
                                        context.getSource(),
                                        StringArgumentType.getString(context, "role"),
                                        BoolArgumentType.getBool(context, "enabled")
                                ))
                        )
                )
        );
    }

    private static int execute(@NotNull ServerCommandSource source, @NotNull String roleName, boolean enabled) throws CommandSyntaxException {
        for (Role role : TMMRoles.ROLES) {
            if (TMMRoles.SPECIAL_ROLES.contains(role)) continue;
            if (role.identifier().getPath().equals(roleName)) {
                TMMRoles.setRoleEnabled(role, enabled);
                Text roleText = Text.literal(role.identifier().getPath()).withColor(role.color());
                Text statusText = enabled
                        ? Text.translatable("commands.trainmurdermystery.setenabledrole.enabled")
                        : Text.translatable("commands.trainmurdermystery.setenabledrole.disabled");
                source.sendFeedback(() -> Text.translatable("commands.trainmurdermystery.setenabledrole.success", roleText, statusText), true);
                return 1;
            }
        }
        throw INVALID_ROLE_EXCEPTION.create();
    }
}
