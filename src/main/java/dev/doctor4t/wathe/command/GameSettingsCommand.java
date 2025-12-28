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
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
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
                                                .then(CommandManager.argument("dividend", IntegerArgumentType.integer(3, 6))
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
        GameWorldComponent.KEY.get(source.getWorld()).setKillerDividend(dividend);
        return 1;
    }

    private static int setVigilanteDividend(ServerCommandSource source, int dividend) {
        GameWorldComponent.KEY.get(source.getWorld()).setVigilanteDividend(dividend);
        return 1;
    }

    private static int setNeutralDividend(ServerCommandSource source, int dividend) {
        GameWorldComponent.KEY.get(source.getWorld()).setNeutralDividend(dividend);
        return 1;
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
        GameWorldComponent.KEY.get(source.getWorld()).setShootInnocentPunishment(punishment);

        Text punishmentName = switch (punishment) {
            case PREVENT_GUN_PICKUP -> Text.translatable("commands.wathe.gamesettings.shootinnocentpunishment.preventgunpickup");
            case KILL_SHOOTER -> Text.translatable("commands.wathe.gamesettings.shootinnocentpunishment.killshooter");
        };
        source.sendFeedback(() -> Text.translatable("commands.wathe.gamesettings.shootinnocentpunishment.success", punishmentName), true);
        return 1;
    }

    private static int setEnabledRole(@NotNull ServerCommandSource source, @NotNull String roleName, boolean enabled) throws CommandSyntaxException {
        for (Role role : WatheRoles.ROLES) {
            if (WatheRoles.SPECIAL_ROLES.contains(role)) continue;
            if (role.identifier().getPath().equals(roleName)) {
                GameWorldComponent.KEY.get(source.getWorld()).setRoleEnabled(role, enabled);
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
        MutableText message = Text.translatable("commands.wathe.listroles.header").withColor(Colors.GRAY);

        for (Role role : WatheRoles.ROLES) {
            if (WatheRoles.SPECIAL_ROLES.contains(role)) {
                continue;
            }

            message.append("\n");
            String roleId = role.identifier().getPath();
            Faction faction = role.getFaction();

            // Faction tag
            String factionKey = switch (faction) {
                case NONE -> "faction.wathe.none";
                case CIVILIAN -> "faction.wathe.civilian";
                case KILLER -> "faction.wathe.killer";
                case NEUTRAL -> "faction.wathe.neutral";
            };
            int factionColor = switch (faction) {
                case NONE, CIVILIAN -> CIVILIAN_COLOR;
                case KILLER -> KILLER_COLOR;
                case NEUTRAL -> NEUTRAL_COLOR;
            };
            Text factionTag = Text.literal("[").withColor(factionColor)
                    .append(Text.translatable(factionKey))
                    .append(Text.literal("] "));
            message.append(factionTag);

            String roleKey = "announcement.role." + roleId;
            MutableText roleName = Text.translatable(roleKey).withColor(role.color());

            boolean isEnabled = GameWorldComponent.KEY.get(source.getWorld()).isRoleEnabled(role);
            String command = "/wathe:gameSettings set enableRole " + roleId + " " + !isEnabled;
            String hoverKey = isEnabled ? "commands.wathe.listroles.click_to_disable" : "commands.wathe.listroles.click_to_enable";

            roleName = roleName
                    .styled(style -> style
                            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.translatable(hoverKey)))
                            .withUnderline(true)
                    );

            message.append(roleName);

            if (GameWorldComponent.KEY.get(source.getWorld()).isRoleEnabled(role)) {
                message.append(Text.literal(" [")
                        .append(Text.translatable("commands.wathe.listroles.enabled"))
                        .append(Text.literal("]"))
                        .withColor(ENABLED_COLOR));
            } else {
                message.append(Text.literal(" [")
                        .append(Text.translatable("commands.wathe.listroles.disabled"))
                        .append(Text.literal("]"))
                        .withColor(DISABLED_COLOR));
            }
        }

        source.sendMessage(message);
        return 1;
    }

}
