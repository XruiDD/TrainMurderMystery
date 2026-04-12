package dev.doctor4t.wathe.client.gui;

import dev.doctor4t.wathe.client.WatheClient;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

@Environment(EnvType.CLIENT)
public class AirRenderer {
    private static final Identifier AIR_TEXTURE = Identifier.ofVanilla("hud/air");
    private static final Identifier AIR_BURSTING_TEXTURE = Identifier.ofVanilla("hud/air_bursting");

    public static void renderHud(PlayerEntity player, DrawContext context, RenderTickCounter tickCounter) {
        if (!WatheClient.isPlayerPlayingAndAlive()) {
            return;
        }

        int maxAir = player.getMaxAir();
        int currentAir = Math.min(player.getAir(), maxAir);

        // Only show when underwater or recovering air
        if (!player.isSubmergedIn(FluidTags.WATER) && currentAir >= maxAir) {
            return;
        }

        int width = context.getScaledWindowWidth();
        int height = context.getScaledWindowHeight();
        // Position: right side above hotbar, same as vanilla
        int baseX = width / 2 + 91;
        int baseY = height - 49; // above stamina bar row

        int fullBubbles = MathHelper.ceil((double)(currentAir - 2) * 10.0 / maxAir);
        int burstingBubbles = MathHelper.ceil((double)currentAir * 10.0 / maxAir) - fullBubbles;

        for (int i = 0; i < fullBubbles + burstingBubbles; i++) {
            int x = baseX - i * 8 - 9;
            if (i < fullBubbles) {
                context.drawGuiTexture(AIR_TEXTURE, x, baseY, 9, 9);
            } else {
                context.drawGuiTexture(AIR_BURSTING_TEXTURE, x, baseY, 9, 9);
            }
        }
    }
}
