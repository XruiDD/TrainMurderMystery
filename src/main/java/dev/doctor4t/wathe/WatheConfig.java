package dev.doctor4t.wathe;

import dev.doctor4t.ratatouille.client.util.OptionLocker;
import dev.doctor4t.wathe.client.WatheClient;
import eu.midnightdust.lib.config.MidnightConfig;
import net.minecraft.client.MinecraftClient;

public class WatheConfig extends MidnightConfig {
    @Entry
    public static boolean disableScreenShake = false;
    
}