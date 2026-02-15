package dev.doctor4t.wathe.item;

import dev.doctor4t.wathe.client.gui.screen.WalkieTalkieScreen;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Hand;

@Environment(EnvType.CLIENT)
public class WalkieTalkieItemClient {
    public static void openScreen(int channel, Hand hand) {
        MinecraftClient.getInstance().setScreen(new WalkieTalkieScreen(channel, hand));
    }
}
