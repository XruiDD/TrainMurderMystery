package dev.doctor4t.trainmurdermystery.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import dev.doctor4t.trainmurdermystery.TMM;
import dev.doctor4t.trainmurdermystery.api.Role;
import dev.doctor4t.trainmurdermystery.api.TMMRoles;
import dev.doctor4t.trainmurdermystery.cca.ScoreboardRoleSelectorComponent;
import dev.doctor4t.trainmurdermystery.command.argument.RoleSuggestionProvider;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

public class ForceRoleCommand {
    public static final SimpleCommandExceptionType INVALID_ROLE_EXCEPTION = new SimpleCommandExceptionType(Text.translatable("commands.trainmurdermystery.forcerole.invalid"));

    public static void register(@NotNull CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("tmm:forceRole").requires(source -> source.hasPermissionLevel(2))
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
        return TMM.executeSupporterCommand(source, () -> {
            ScoreboardRoleSelectorComponent component = ScoreboardRoleSelectorComponent.KEY.get(source.getServer().getScoreboard());
            boolean hasForced = false;
            for (Role role : TMMRoles.ROLES) {
                if (TMMRoles.SPECIAL_ROLES.contains(role)) continue;
                List<UUID> forcedList = component.getForcedForRole(role);
                if (forcedList.contains(targetPlayer.getUuid())) {
                    Text roleText = Text.literal(role.identifier().getPath()).withColor(role.color());
                    source.sendFeedback(() -> Text.translatable("commands.trainmurdermystery.forcerole.query", targetPlayer.getDisplayName(), roleText), false);
                    hasForced = true;
                }
            }
            if (!hasForced) {
                source.sendFeedback(() -> Text.translatable("commands.trainmurdermystery.forcerole.query.none", targetPlayer.getDisplayName()), false);
            }
        });
    }

    private static int execute(@NotNull ServerCommandSource source, @NotNull ServerPlayerEntity targetPlayer, @NotNull String roleName) throws CommandSyntaxException {
        for (Role role : TMMRoles.ROLES) {
            if (TMMRoles.SPECIAL_ROLES.contains(role)) continue;
            if (role.identifier().getPath().equals(roleName)) {
                final Role finalRole = role;
                return TMM.executeSupporterCommand(source, () -> {
                    ScoreboardRoleSelectorComponent component = ScoreboardRoleSelectorComponent.KEY.get(source.getServer().getScoreboard());
                    List<UUID> forcedList = component.getForcedForRole(finalRole);
                    if (!forcedList.contains(targetPlayer.getUuid())) {
                        forcedList.add(targetPlayer.getUuid());
                    }
                    Text roleText = Text.literal(finalRole.identifier().getPath()).withColor(finalRole.color());
                    source.sendFeedback(() -> Text.translatable("commands.trainmurdermystery.forcerole.success", roleText, targetPlayer.getDisplayName()), true);
                });
            }
        }
        throw INVALID_ROLE_EXCEPTION.create();
    }
}
