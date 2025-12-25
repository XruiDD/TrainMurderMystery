package dev.doctor4t.wathe.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import dev.doctor4t.wathe.Wathe;
import dev.doctor4t.wathe.api.*;
import dev.doctor4t.wathe.cca.AutoStartComponent;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.command.argument.RoleSuggestionProvider;
import dev.doctor4t.wathe.game.GameConstants;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import org.jetbrains.annotations.NotNull;

public class GameSettingsCommand {
    private static final SimpleCommandExceptionType INVALID_ROLE_EXCEPTION = new SimpleCommandExceptionType(Text.translatable("commands.wathe.setenabledrole.invalid"));
    private static final int CIVILIAN_COLOR = 0x36E51B;
    private static final int KILLER_COLOR = 0xC13838;
    private static final int NEUTRAL_COLOR = 0x9F9F9F;
    private static final int ENABLED_COLOR = 0x55FF55;
    private static final int DISABLED_COLOR = 0xFF5555;

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                CommandManager.literal("wathe:gameSettings")
                        .requires(source -> source.hasPermissionLevel(2))
                        .then(CommandManager.literal("help")
                                .executes(context -> sendHelp(context.getSource()))
                        )
                        .then(CommandManager.literal("listRoles")
                                .executes(context -> listRoles(context.getSource()))
                        )
                        .then(CommandManager.literal("set")
                                .then(CommandManager.literal("autoStart")
                                        .then(CommandManager.argument("seconds", IntegerArgumentType.integer(0, 60))
                                                .executes(context -> setAutoStart(context.getSource(), IntegerArgumentType.getInteger(context, "seconds")))
                                        )
                                )
                                .then(CommandManager.literal("backfire")
                                        .then(CommandManager.argument("chance", FloatArgumentType.floatArg(0f, 1f))
                                                .executes(context -> setBackfire(context.getSource(), FloatArgumentType.getFloat(context, "chance")))
                                        )
                                )
                                .then(CommandManager.literal("roleDividend")
                                        .then(CommandManager.literal("killer")
                                                .then(CommandManager.argument("dividend", IntegerArgumentType.integer(3))
                                                        .executes(context -> setKillerDividend(context.getSource(), IntegerArgumentType.getInteger(context, "dividend")))
                                                )
                                        )
                                        .then(CommandManager.literal("vigilante")
                                                .then(CommandManager.argument("dividend", IntegerArgumentType.integer(3))
                                                        .executes(context -> setVigilanteDividend(context.getSource(), IntegerArgumentType.getInteger(context, "dividend")))
                                                )
                                        ).then(CommandManager.literal("neutral")
                                                .then(CommandManager.argument("dividend", IntegerArgumentType.integer(3))
                                                        .executes(context -> setNeutralDividend(context.getSource(), IntegerArgumentType.getInteger(context, "dividend")))
                                                )
                                        )
                                )
                                .then(CommandManager.literal("bounds")
                                        .then(CommandManager.argument("enabled", BoolArgumentType.bool())
                                                .executes(context -> enableBounds(context.getSource(), BoolArgumentType.getBool(context, "enabled"))))
                                )
                                .then(CommandManager.literal("shootInnocentPunishment")
                                        .then(CommandManager.literal("preventGunPickup")
                                                .executes(context -> setShootInnocentPunishment(context.getSource(), GameWorldComponent.ShootInnocentPunishment.PREVENT_GUN_PICKUP))
                                        )
                                        .then(CommandManager.literal("killShooter")
                                                .executes(context -> setShootInnocentPunishment(context.getSource(), GameWorldComponent.ShootInnocentPunishment.KILL_SHOOTER))
                                        )
                                )
                                .then(CommandManager.literal("enableRole")
                                        .then(CommandManager.argument("role", StringArgumentType.string())
                                                .suggests(new RoleSuggestionProvider())
                                                .then(CommandManager.argument("enabled", BoolArgumentType.bool())
                                                        .executes(context -> setEnabledRole(
                                                                context.getSource(),
                                                                StringArgumentType.getString(context, "role"),
                                                                BoolArgumentType.getBool(context, "enabled")
                                                        ))
                                                )
                                        )
                                )
                        )
        );
    }

    private static int sendHelp(ServerCommandSource source) {
        source.sendMessage(Text.translatable("wathe.game_settings.help"));
        return 1;
    }

    private static int setAutoStart(ServerCommandSource source, int seconds) {
        AutoStartComponent component = AutoStartComponent.KEY.get(source.getWorld());
        component.setStartTime(GameConstants.getInTicks(0, seconds));
        return 1;
    }

    private static int setBackfire(ServerCommandSource source, float chance) {
        return Wathe.executeSupporterCommand(source,
                () -> GameWorldComponent.KEY.get(source.getWorld()).setBackfireChance(chance)
        );
    }

    private static int setKillerDividend(ServerCommandSource source, int dividend) {
        return Wathe.executeSupporterCommand(source,
                () -> GameWorldComponent.KEY.get(source.getWorld()).setKillerDividend(dividend)
        );
    }

    private static int setVigilanteDividend(ServerCommandSource source, int dividend) {
        return Wathe.executeSupporterCommand(source,
                () -> GameWorldComponent.KEY.get(source.getWorld()).setVigilanteDividend(dividend)
        );
    }

    private static int setNeutralDividend(ServerCommandSource source, int dividend) {
        return Wathe.executeSupporterCommand(source,
                () -> GameWorldComponent.KEY.get(source.getWorld()).setNeutralDividend(dividend)
        );
    }

    private static int enableBounds(ServerCommandSource source, boolean enabled) {
        return Wathe.executeSupporterCommand(source,
                () -> {
                    GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(source.getWorld());
                    gameWorldComponent.setBound(enabled);
                }
        );
    }

    private static int setShootInnocentPunishment(ServerCommandSource source, GameWorldComponent.ShootInnocentPunishment punishment) {
        return Wathe.executeSupporterCommand(source, () -> {
            GameWorldComponent.KEY.get(source.getWorld()).setShootInnocentPunishment(punishment);

            String punishmentName = switch (punishment) {
                case PREVENT_GUN_PICKUP -> "prevent gun pickup";
                case KILL_SHOOTER -> "kill shooter";
            };
            source.sendMessage(Text.literal("Set shoot innocent punishment to: " + punishmentName));
        });
    }

    private static int setEnabledRole(@NotNull ServerCommandSource source, @NotNull String roleName, boolean enabled) throws CommandSyntaxException {
        for (Role role : WatheRoles.ROLES) {
            if (WatheRoles.SPECIAL_ROLES.contains(role)) continue;
            if (role.identifier().getPath().equals(roleName)) {
                WatheRoles.setRoleEnabled(role, enabled);
                Text roleText = Text.literal(role.identifier().getPath()).withColor(role.color());
                Text statusText = enabled
                        ? Text.translatable("commands.wathe.setenabledrole.enabled")
                        : Text.translatable("commands.wathe.setenabledrole.disabled");
                source.sendFeedback(() -> Text.translatable("commands.wathe.setenabledrole.success", roleText, statusText), true);
                return 1;
            }
        }
        throw INVALID_ROLE_EXCEPTION.create();
    }

    private static int listRoles(ServerCommandSource source) {
        MutableText message = Text.literal("Roles:").withColor(Colors.GRAY);

        for (Role role : WatheRoles.ROLES) {
            message.append("\n");
            String roleName = role.identifier().getPath();
            Faction faction = role.getFaction();

            // Faction tag
            Text factionTag = switch (faction) {
                case NONE -> Text.literal("[None] ").withColor(CIVILIAN_COLOR);
                case CIVILIAN -> Text.literal("[Civilian] ").withColor(CIVILIAN_COLOR);
                case KILLER -> Text.literal("[Killer] ").withColor(KILLER_COLOR);
                case NEUTRAL -> Text.literal("[Neutral] ").withColor(NEUTRAL_COLOR);
            };
            message.append(factionTag);

            // Role name
            message.append(Text.literal(roleName).withColor(role.color()));

            // Special tag
            if (WatheRoles.SPECIAL_ROLES.contains(role)) {
                message.append(Text.literal(" (special)").withColor(Colors.LIGHT_GRAY));
            }

            // Enabled/Disabled status
            if (!WatheRoles.SPECIAL_ROLES.contains(role)) {
                if (WatheRoles.isRoleEnabled(role)) {
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
