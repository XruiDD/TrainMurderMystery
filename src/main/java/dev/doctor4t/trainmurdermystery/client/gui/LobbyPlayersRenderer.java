package dev.doctor4t.trainmurdermystery.client.gui;

import dev.doctor4t.trainmurdermystery.cca.TMMComponents;
import dev.doctor4t.trainmurdermystery.game.GameConstants;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;

public class LobbyPlayersRenderer {
    public static void renderHud(TextRenderer renderer, @NotNull ClientPlayerEntity player, @NotNull DrawContext context) {
        var game = TMMComponents.GAME.get(player.getWorld());
        if (!game.isRunning()) {
            context.getMatrices().push();
            context.getMatrices().translate(context.getScaledWindowWidth() / 2f, 6, 0);
            var world = player.getWorld();
            var players = world.getPlayers();
            var count = players.size();
            var ready = players.stream().filter(p -> GameConstants.READY_AREA.contains(p.getPos())).count();
            var text = Text.translatable("lobby.players.count", ready, count);
            context.drawTextWithShadow(renderer, text, -renderer.getWidth(text) / 2, 0, 0xFFFFFFFF);
            context.getMatrices().pop();
        }
    }
}