package dev.doctor4t.wathe.client.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.NotNull;

@Environment(EnvType.CLIENT)
public class CooldownRenderer {

    public static void renderHud(TextRenderer renderer, @NotNull ClientPlayerEntity player, DrawContext context, RenderTickCounter tickCounter) {
        int selectedSlot = player.getInventory().selectedSlot;
        ItemStack heldStack = player.getInventory().main.get(selectedSlot);

        if (heldStack.isEmpty()) return;

        Item heldItem = heldStack.getItem();
        ItemCooldownManager manager = player.getItemCooldownManager();

        if (!manager.isCoolingDown(heldItem)) return;

        // Get the actual remaining ticks directly from the cooldown entry
        ItemCooldownManager.Entry entry = manager.entries.get(heldItem);
        if (entry == null) return;

        float remainingTicks = entry.endTick - (manager.tick + tickCounter.getTickDelta(true));
        if (remainingTicks <= 0f) return;

        float remainingSeconds = remainingTicks / 20f;

        // Format the time string
        String timeText;
        if (remainingSeconds >= 60f) {
            int minutes = (int) (remainingSeconds / 60f);
            int seconds = MathHelper.ceil(remainingSeconds % 60f);
            if (seconds == 60) {
                minutes++;
                seconds = 0;
            }
            timeText = String.format("%d:%02d", minutes, seconds);
        } else if (remainingSeconds >= 10f) {
            int seconds = MathHelper.ceil(remainingSeconds);
            timeText = String.format("%ds", seconds);
        } else {
            timeText = String.format("%.1fs", remainingSeconds);
        }

        int screenWidth = context.getScaledWindowWidth();
        int screenHeight = context.getScaledWindowHeight();

        // Position: centered above the selected hotbar slot
        // Hotbar slot X: screenWidth/2 - 90 + selectedSlot * 20 + 2, item is 16px wide
        int slotCenterX = screenWidth / 2 - 90 + selectedSlot * 20 + 2 + 8;
        int textWidth = renderer.getWidth(timeText);
        int textX = slotCenterX - textWidth / 2;

        // Y: above the hotbar (hotbar top is at screenHeight - 22), leave some gap
        int textY = screenHeight - 22 - 4 - renderer.fontHeight;

        context.getMatrices().push();
        context.getMatrices().translate(0, 0, 200);
        context.drawTextWithShadow(renderer, timeText, textX, textY, 0xFFFFFFFF);
        context.getMatrices().pop();
    }
}
