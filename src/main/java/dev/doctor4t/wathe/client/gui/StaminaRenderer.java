package dev.doctor4t.wathe.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.doctor4t.wathe.Wathe;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerStaminaComponent;
import dev.doctor4t.wathe.client.WatheClient;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

@Environment(EnvType.CLIENT)
public class StaminaRenderer {
    private static final Identifier STAMINA_TEXTURE = Wathe.id("textures/gui/stamina_bar.png");

    public static void renderHud(@NotNull PlayerEntity player, DrawContext context, RenderTickCounter tickCounter) {
        if (!WatheClient.isPlayerPlayingAndAlive()) {
            return;
        }

        PlayerStaminaComponent staminaComponent = PlayerStaminaComponent.KEY.get(player);

        int maxSprintTime = staminaComponent.getMaxSprintTime();
        if (maxSprintTime < 0) return;

        float sprintingTicks = staminaComponent.getSprintingTicks();
        boolean exhausted = staminaComponent.isExhausted();
        float staminaScale = sprintingTicks / maxSprintTime;
        staminaScale = Math.max(0, Math.min(1, staminaScale)) * 10f;

        int width = context.getScaledWindowWidth();
        int height = context.getScaledWindowHeight();
        int baseX = width / 2 + 91;
        int baseY = height - 39;

        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        for (int i = 0; i < 10; i++) {
            int x = baseX - i * 8 - 9;
            int textureX;

            // 疲惫状态使用暗色图标 (X=27起始)，正常状态使用正常图标 (X=0起始)
            if (exhausted) {
                textureX = 27; // 疲惫状态基础位置
            } else {
                textureX = 0;  // 正常状态基础位置
            }

            // 根据体力值选择满/半/空图标
            if (staminaScale < i) {
                textureX += 18; // 空图标
            } else if (staminaScale < i + 0.5f) {
                textureX += 9;  // 半图标
            }
            // else: 满图标 (不需要偏移)

            context.drawTexture(STAMINA_TEXTURE, x, baseY, textureX, 119, 9, 9, 128, 128);
        }
    }
}
