package dev.doctor4t.wathe.util;

/**
 * Permission node constants for the Fabric Permissions API.
 * Each node falls back to the specified default op level when no permission provider is installed.
 */
public final class WathePermissions {
    // Command permissions (default: op level 2)
    public static final String COMMAND_CONFIG = "wathe.command.config";
    public static final String COMMAND_GIVE_ROOM_KEY = "wathe.command.giveroomkey";
    public static final String COMMAND_FORCE_ROLE = "wathe.command.forcerole";
    public static final String COMMAND_MAP_VOTE = "wathe.command.mapvote";
    public static final String COMMAND_MAP_VARIABLES = "wathe.command.mapvariables";
    public static final String COMMAND_GAME_SETTINGS = "wathe.command.gamesettings";
    public static final String COMMAND_SET_MONEY = "wathe.command.setmoney";
    public static final String COMMAND_SET_TIMER = "wathe.command.settimer";
    public static final String COMMAND_SET_VISUAL = "wathe.command.setvisual";
    public static final String COMMAND_START = "wathe.command.start";
    public static final String COMMAND_STOP = "wathe.command.stop";
    public static final String COMMAND_UPDATE_DOORS = "wathe.command.updatedoors";

    // Gameplay permissions (wathe.admin.* — default: op level 2)
    public static final String ADMIN_HORN_COOLDOWN = "wathe.admin.horn_cooldown";       // 按喇叭跳过冷却
    public static final String ADMIN_HORN_START = "wathe.admin.horn_start";             // 按喇叭直接开始游戏
    public static final String ADMIN_SPECTATOR_BYPASS = "wathe.admin.spectator_bypass"; // 旁观模式不被自动重置
    public static final String SPECTATE = "wathe.spectate";                             // default: op level 1

    public static final int DEFAULT_COMMAND_LEVEL = 2;

    private WathePermissions() {}
}
