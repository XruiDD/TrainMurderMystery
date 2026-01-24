package dev.doctor4t.wathe.client.gui.screen.ingame;

import dev.doctor4t.wathe.Wathe;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
import dev.doctor4t.wathe.client.gui.StoreRenderer;
import dev.doctor4t.wathe.util.ShopEntry;
import dev.doctor4t.wathe.util.ShopUtils;
import dev.doctor4t.wathe.util.StoreBuyPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class LimitedInventoryScreen extends LimitedHandledScreen<PlayerScreenHandler> {
    public static final Identifier BACKGROUND_TEXTURE = Wathe.id("textures/gui/container/limited_inventory.png");
    public static final @NotNull Identifier ID = Wathe.id("textures/gui/game.png");
    public final ClientPlayerEntity player;

    public LimitedInventoryScreen(@NotNull ClientPlayerEntity player) {
        super(player.playerScreenHandler, player.getInventory(), Text.empty());
        this.player = player;
    }

    @Override
    protected void init() {
        super.init();

        // Get shop entries for this player
        List<ShopEntry> entries = ShopUtils.getShopEntriesForPlayer(player);

        // Check shop access
        if (entries.isEmpty()) {
            return;
        }

        int apart = 38;
        int x = this.width / 2 - entries.size() * apart / 2 + 9;
        int y = this.y - 46;
        for (int i = 0; i < entries.size(); i++)
            this.addDrawableChild(new StoreItemWidget(this, x + apart * i, y, entries.get(i), i));
    }

    @Override
    protected void drawBackground(@NotNull DrawContext context, float delta, int mouseX, int mouseY) {
        context.drawTexture(BACKGROUND_TEXTURE, this.x, this.y, 0, 0, this.backgroundWidth, this.backgroundHeight);

        context.getMatrices().push();
        context.getMatrices().translate(context.getScaledWindowWidth() / 2f, context.getScaledWindowHeight(), 0);
        float scale = 0.28f;
        context.getMatrices().scale(scale, scale, 1f);
        int height = 254;
        int width = 497;
        context.getMatrices().translate(0, -230, 0);
        int xOffset = 0;
        int yOffset = 0;
        context.drawTexturedQuad(ID, (int) (xOffset - width / 2f), (int) (xOffset + width / 2f), (int) (yOffset - height / 2f), (int) (yOffset + height / 2f), 0, 0, 1f, 0, 1f, 1f, 1f, 1f, 1f);
        context.getMatrices().pop();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(context, mouseX, mouseY);
        StoreRenderer.renderHud(this.textRenderer, this.player, context, delta);
    }

    public static class StoreItemWidget extends ButtonWidget {
        public final LimitedInventoryScreen screen;
        public final ShopEntry entry;

        public StoreItemWidget(LimitedInventoryScreen screen, int x, int y, @NotNull ShopEntry entry, int index) {
            super(x, y, 16, 16, entry.stack().getName(), (a) -> ClientPlayNetworking.send(new StoreBuyPayload(index)), DEFAULT_NARRATION_SUPPLIER);
            this.screen = screen;
            this.entry = entry;
        }

        @Override
        protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
            super.renderWidget(context, mouseX, mouseY, delta);

            // Get shop component for cooldown/stock info
            PlayerShopComponent shopComponent = PlayerShopComponent.KEY.get(screen.player);
            boolean onCooldown = shopComponent.isOnCooldown(entry.id());
            boolean inStock = shopComponent.isInStock(entry.id());
            int remainingStock = shopComponent.getRemainingStock(entry.id());
            int maxStock = shopComponent.getMaxStock(entry.id());
            int remainingCooldown = shopComponent.getRemainingCooldown(entry.id());

            boolean unavailable = onCooldown || !inStock;

            // Draw slot background
            context.drawGuiTexture(entry.type().getTexture(), this.getX() - 7, this.getY() - 7, 30, 30);

            // Draw item
            context.drawItem(this.entry.stack(), this.getX(), this.getY());

            // Draw gray overlay if unavailable
            if (unavailable) {
                int darkColor = 0xAA000000;
                context.fillGradient(RenderLayer.getGuiOverlay(), this.getX(), this.getY(), this.getX() + 16, this.getY() + 16, darkColor, darkColor, 200);
            }

            // Push z for text rendering on top of item
            context.getMatrices().push();
            context.getMatrices().translate(0, 0, 200);

            // Draw cooldown time (centered)
            if (onCooldown) {
                int seconds = remainingCooldown / 20;
                if (seconds > 0) {
                    String cooldownText = seconds + "s";
                    int textX = this.getX() + 8 - screen.textRenderer.getWidth(cooldownText) / 2;
                    int textY = this.getY() + 4;
                    context.drawText(screen.textRenderer, cooldownText, textX, textY, 0xFFFFFF, true);
                }
            }

            // Draw stock count (bottom-right corner) if limited
            if (maxStock > 0) {
                String stockText = String.valueOf(remainingStock);
                int stockColor = remainingStock > 0 ? 0xFFFFFF : 0xFF4444;
                int textX = this.getX() + 16 - screen.textRenderer.getWidth(stockText);
                int textY = this.getY() + 16 - 8;
                context.drawText(screen.textRenderer, stockText, textX, textY, stockColor, true);
            }

            context.getMatrices().pop();

            // Draw hover highlight
            if (this.isHovered()) {
                this.screen.renderLimitedInventoryTooltip(context, this.entry.stack());
                if (!unavailable) {
                    drawShopSlotHighlight(context, this.getX(), this.getY(), 0);
                }
            }

            // Draw price
            MutableText price = Text.literal(this.entry.price() + "\uE781");
            context.drawTooltip(this.screen.textRenderer, price, this.getX() - 4 - this.screen.textRenderer.getWidth(price) / 2, this.getY() - 9);
        }

        private void drawShopSlotHighlight(DrawContext context, int x, int y, int z) {
            int color = 0x90FFBF49;
            context.fillGradient(RenderLayer.getGuiOverlay(), x, y, x + 16, y + 14, color, color, z);
            context.fillGradient(RenderLayer.getGuiOverlay(), x, y + 14, x + 15, y + 15, color, color, z);
            context.fillGradient(RenderLayer.getGuiOverlay(), x, y + 15, x + 14, y + 16, color, color, z);
        }

        @Override
        public void drawMessage(DrawContext context, TextRenderer textRenderer, int color) {
        }
    }
}
