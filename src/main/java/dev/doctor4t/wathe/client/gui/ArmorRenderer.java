package dev.doctor4t.wathe.client.gui;

import dev.doctor4t.wathe.cca.PlayerPsychoComponent;
import dev.doctor4t.wathe.game.GameConstants;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

@Environment(EnvType.CLIENT)
public class ArmorRenderer {
    private static final Identifier ARMOR_FULL = Identifier.ofVanilla("hud/armor_full");
    private static final Identifier ARMOR_HALF = Identifier.ofVanilla("hud/armor_half");
    private static final Identifier ARMOR_EMPTY = Identifier.ofVanilla("hud/armor_empty");
    public static void renderHud(@NotNull PlayerEntity player, DrawContext context, RenderTickCounter tickCounter)
    {
        PlayerPsychoComponent psychoComponent = PlayerPsychoComponent.KEY.get(player);
        if (psychoComponent.getPsychoTicks() > 0) {
            int width = context.getScaledWindowWidth();
            int height = context.getScaledWindowHeight();
            int baseX = width / 2 - 91;
            int baseY = height - 39;
            int currentArmour = psychoComponent.getArmour();
            for (int i = 0; i < currentArmour; i++) {
                int x = baseX + (i * 8);
                context.drawGuiTexture(ARMOR_FULL, x, baseY, 9, 9);
            }
        }
    }

}
