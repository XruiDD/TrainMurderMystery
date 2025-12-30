package dev.doctor4t.wathe;

import eu.midnightdust.lib.config.MidnightConfig;

public class WatheConfig extends MidnightConfig {
    @Entry
    public static boolean disableScreenShake = false;

    @Entry
    public static SnowModeConfig snowOptLevel = SnowModeConfig.NO_OPTIMIZATION;

    public enum SnowModeConfig {
        NO_OPTIMIZATION,  // Standard behavior: checking if the particle hit the block.
        BOX_COLLIDER, // replaces the calculation against the terrain to the calculation against a box that approximates the train
        TURN_OFF, // Client side '/wathe:setVisual snow false'
    }
}
