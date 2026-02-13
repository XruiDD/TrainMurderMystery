package dev.doctor4t.wathe.client.gui;

import dev.doctor4t.ratatouille.util.TextUtils;
import dev.doctor4t.wathe.api.GameMode;
import dev.doctor4t.wathe.cca.AutoStartComponent;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.MapVotingComponent;
import dev.doctor4t.wathe.client.WatheClient;
import dev.doctor4t.wathe.client.gui.screen.MapVotingScreen;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class LobbyPlayersRenderer {
    public static void renderHud(TextRenderer renderer, @NotNull ClientPlayerEntity player, @NotNull DrawContext context) {
        GameWorldComponent game = GameWorldComponent.KEY.get(player.getWorld());
        if (!game.isRunning()) {
            World world = player.getWorld();

            // 检查是否正在投票
            MapVotingComponent votingComp = MapVotingComponent.KEY.get(world.getScoreboard());
            boolean isVotingScreenOpen = MinecraftClient.getInstance().currentScreen instanceof MapVotingScreen;

            if (votingComp.isVotingActive() && !isVotingScreenOpen) {
                // 投票进行中且投票界面未打开 - 在HUD上显示倒计时和按键提示
                context.getMatrices().push();
                context.getMatrices().translate(context.getScaledWindowWidth() / 2f, 6, 0);

                int keybindHintY;
                if (!votingComp.isRoulettePhase()) {
                    // 投票倒计时 - 参考游戏倒计时样式
                    int secondsRemaining = Math.max(0, votingComp.getVotingTicksRemaining() / 20);
                    int minutes = secondsRemaining / 60;
                    int secs = secondsRemaining % 60;
                    String timeStr = String.format("%d:%02d", minutes, secs);

                    context.getMatrices().push();
                    float timerScale = 1.5f;
                    context.getMatrices().scale(timerScale, timerScale, 1f);
                    // 倒计时颜色：时间充裕时金色，最后10秒红色闪烁
                    int timerColor;
                    if (secondsRemaining <= 10) {
                        float flash = (float) (0.5f + 0.5f * Math.sin(System.currentTimeMillis() / 150.0));
                        int r = (int) (255 * (0.6f + 0.4f * flash));
                        timerColor = 0xFF000000 | (r << 16) | (0x30 << 8) | 0x30;
                    } else {
                        timerColor = 0xFFC5A244;
                    }
                    Text timerText = Text.literal(timeStr);
                    context.drawTextWithShadow(renderer, timerText,
                        -renderer.getWidth(timerText) / 2, 0, timerColor);
                    context.getMatrices().pop();

                    // 投票状态文字
                    MutableText votingText = Text.translatable("lobby.voting.active");
                    context.drawTextWithShadow(renderer, votingText,
                        -renderer.getWidth(votingText) / 2, 16, 0xFFC5A244);

                    keybindHintY = 28;
                } else {
                    // 轮盘阶段
                    MutableText selectingText = Text.translatable("gui.wathe.map_voting.selecting");
                    context.drawTextWithShadow(renderer, selectingText,
                        -renderer.getWidth(selectingText) / 2, 0, 0xFFC5A244);
                    keybindHintY = 12;
                }

                // 按键提示
                String keyName = WatheClient.mapVoteKeybind.getBoundKeyLocalizedText().getString();
                MutableText keybindHint = Text.translatable("lobby.voting.keybind_hint", keyName);
                context.drawTextWithShadow(renderer, keybindHint,
                    -renderer.getWidth(keybindHint) / 2, keybindHintY, 0xFFAAAAAA);

                context.getMatrices().pop();
            } else if (!votingComp.isVotingActive()) {
                // 非投票阶段 - 显示正常大厅信息
                context.getMatrices().push();
                context.getMatrices().translate(context.getScaledWindowWidth() / 2f, 6, 0);

                List<? extends PlayerEntity> players = world.getPlayers();
                int count = players.size();
                int readyPlayerCount = GameFunctions.getReadyPlayerCount(world);
                MutableText playerCountText = Text.translatable("lobby.players.count", readyPlayerCount, count);
                context.drawTextWithShadow(renderer, playerCountText, -renderer.getWidth(playerCountText) / 2, 0, 0xFFFFFFFF);

                AutoStartComponent autoStartComponent = AutoStartComponent.KEY.get(world);
                GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(world);
                int nextLineY = 10;
                if (autoStartComponent.isAutoStartActive()) {
                    MutableText autoStartText;
                    int color = 0xFFAAAAAA;
                    int minPlayerCount = gameWorldComponent.getGameMode().minPlayerCount;
                    if (readyPlayerCount >= minPlayerCount) {
                        int seconds = autoStartComponent.getTime() / 20;
                        autoStartText = Text.translatable(seconds <= 0 ? "lobby.autostart.starting" : "lobby.autostart.time", seconds);
                        color = 0xFF00BC16;
                    } else {
                        autoStartText = Text.translatable("lobby.autostart.active", minPlayerCount);
                    }
                    context.drawTextWithShadow(renderer, autoStartText, -renderer.getWidth(autoStartText) / 2, nextLineY, color);
                    nextLineY += 10;
                }

                // 显示当前游戏模式
                GameMode gameMode = gameWorldComponent.getGameMode();
                String gameModeKey = "gamemode." + gameMode.identifier.getNamespace() + "." + gameMode.identifier.getPath();
                MutableText gameModeText = Text.translatable("lobby.autostart.gamemode", Text.translatable(gameModeKey));
                context.drawTextWithShadow(renderer, gameModeText, -renderer.getWidth(gameModeText) / 2, nextLineY, 0xFFC5A244);

                context.getMatrices().pop();
            }
            // 投票界面打开时不渲染HUD顶部文字，避免冲突

            context.getMatrices().push();
            float scale = 0.75f;
            context.getMatrices().translate(0, context.getScaledWindowHeight(), 0);
            context.getMatrices().scale(scale, scale, 1f);
            int i = 0;
            MutableText thanksText = Text.translatable("credits.wathe.thank_you");

            String fallback = "Thank you for playing The Last Voyage of the Harpy Express!\nMe and my team spent a lot of time working\non this mod and we hope you enjoy it.\nIf you do and wish to make a video or stream\nplease make sure to credit my channel,\nvideo and the mod page!\n - RAT / doctor4t";
            if (!thanksText.getString().contains(" - RAT / doctor4t")) {
                thanksText = Text.literal(fallback);
            }

            for (Text text : TextUtils.getWithLineBreaks(thanksText)) {
                i++;
                context.drawTextWithShadow(renderer, text, 10, -90 + 10 * i, 0xFFFFFFFF);
            }
            context.getMatrices().pop();
        }
    }
}