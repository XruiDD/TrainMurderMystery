package dev.doctor4t.wathe.client.gui;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.doctor4t.wathe.api.WatheGameModes;
import dev.doctor4t.wathe.cca.GameRoundEndComponent;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.WatheClient;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.index.WatheSounds;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.option.GameOptions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.ArrayList;
import java.util.List;

public class RoundTextRenderer {
    private static final Map<String, Optional<GameProfile>> failCache = new HashMap<>();
    private static final int WELCOME_DURATION = 200 + GameConstants.FADE_TIME * 2;
    private static final int END_DURATION = 200;
    private static RoleAnnouncementTexts.RoleAnnouncementText role = RoleAnnouncementTexts.CIVILIAN;
    private static int welcomeTime = 0;
    private static int killers = 0;
    private static int targets = 0;
    private static int endTime = 0;
    /**
     * 获取玩家皮肤纹理
     * 优先从模组缓存(PlayerListEntry)获取，如果失效则从客户端皮肤服务(SkinProvider)获取
     */
    private static Identifier getPlayerTexture(GameProfile profile) {
        PlayerListEntry entry = WatheClient.PLAYER_ENTRIES_CACHE.get(profile.getId());
        if (entry != null) {
            return entry.getSkinTextures().texture();
        }

        return MinecraftClient.getInstance().getSkinProvider().getSkinTextures(profile).texture();
    }
    @SuppressWarnings("IntegerDivisionInFloatingPointContext")
    public static void renderHud(TextRenderer renderer, ClientPlayerEntity player, @NotNull DrawContext context) {
        boolean isLooseEnds = GameWorldComponent.KEY.get(player.getWorld()).getGameMode() == WatheGameModes.LOOSE_ENDS;

        if (welcomeTime > 0) {
            context.getMatrices().push();
            context.getMatrices().translate(context.getScaledWindowWidth() / 2f, context.getScaledWindowHeight() / 2f + 3.5, 0);
            context.getMatrices().push();
            context.getMatrices().scale(2.6f, 2.6f, 1f);
            int color = isLooseEnds ? 0x9F0000 : 0xFFFFFF;
            if (welcomeTime <= 180) {
                Text welcomeText = isLooseEnds ? Text.translatable("announcement.loose_ends.welcome") : role.welcomeText;
                context.drawTextWithShadow(renderer, welcomeText, -renderer.getWidth(welcomeText) / 2, -12, color);
            }
            context.getMatrices().pop();
            context.getMatrices().push();
            context.getMatrices().scale(1.2f, 1.2f, 1f);
            if (welcomeTime <= 120) {
                Text premiseText = isLooseEnds ? Text.translatable("announcement.loose_ends.premise") : role.premiseText.apply(killers);
                context.drawTextWithShadow(renderer, premiseText, -renderer.getWidth(premiseText) / 2, 0, color);
            }
            context.getMatrices().pop();
            context.getMatrices().push();
            context.getMatrices().scale(1f, 1f, 1f);
            if (welcomeTime <= 60) {
                Text goalText = isLooseEnds ? Text.translatable("announcement.loose_ends.goal") : role.goalText.apply(targets);
                context.drawTextWithShadow(renderer, goalText, -renderer.getWidth(goalText) / 2, 14, color);
            }
            context.getMatrices().pop();
            context.getMatrices().pop();
        }
        GameWorldComponent game = GameWorldComponent.KEY.get(player.getWorld());
        if (endTime > 0 && endTime < END_DURATION - (GameConstants.FADE_TIME * 2) && !game.isRunning() && game.getGameMode() != WatheGameModes.DISCOVERY) {
            GameRoundEndComponent roundEnd = GameRoundEndComponent.KEY.get(player.getScoreboard());
            if (roundEnd.getWinStatus() == GameFunctions.WinStatus.NONE) return;
            Text endText = null;
            String winRolePath = null;

            if (roundEnd.getWinStatus() == GameFunctions.WinStatus.NEUTRAL)
            {
                for (GameRoundEndComponent.RoundEndData entry : roundEnd.getPlayers()) {
                    if(entry.isWinner()){
                        endText = RoleAnnouncementTexts.getForRole(entry.role()).winText;
                        winRolePath = entry.role().getPath();
                        break;
                    }
                }
            }else {
                PlayerEntity winner = player.getWorld().getPlayerByUuid(game.getLooseEndWinner() == null ? UUID.randomUUID(): game.getLooseEndWinner());
                endText = role.getEndText(roundEnd.getWinStatus(), winner == null ? Text.empty() : winner.getDisplayName());
            }
            if (endText == null) return;
            context.getMatrices().push();
            context.getMatrices().translate(context.getScaledWindowWidth() / 2f, context.getScaledWindowHeight() / 2f - 40, 0);
            context.getMatrices().push();
            context.getMatrices().scale(2.6f, 2.6f, 1f);
            int endTextWidth = renderer.getWidth(endText);
            context.drawTextWithShadow(renderer, endText, -endTextWidth / 2, -12, 0xFFFFFF);
            context.getMatrices().pop();
            context.getMatrices().push();
            context.getMatrices().scale(1.2f, 1.2f, 1f);
            MutableText winMessage = Text.translatable("game.win." + (winRolePath != null ? winRolePath : roundEnd.getWinStatus().name().toLowerCase()));
            int winMessageWidth = renderer.getWidth(winMessage);
            context.drawTextWithShadow(renderer, winMessage, -winMessageWidth / 2, -4, 0xFFFFFF);
            context.getMatrices().pop();
            if (isLooseEnds) {
                context.drawTextWithShadow(renderer, RoleAnnouncementTexts.LOOSE_END.titleText, -renderer.getWidth(RoleAnnouncementTexts.LOOSE_END.titleText) / 2, 14, 0xFFFFFF);
                int looseEnds = 0;
                for (GameRoundEndComponent.RoundEndData entry : roundEnd.getPlayers()) {
                    context.getMatrices().push();
                    context.getMatrices().scale(2f, 2f, 1f);
                    context.getMatrices().translate(((looseEnds % 6) - 3.5) * 12, 14 + (looseEnds / 6) * 12, 0);
                    looseEnds++;
                    PlayerListEntry playerEntry = WatheClient.PLAYER_ENTRIES_CACHE.get(entry.player().getId());
                    if (playerEntry != null && playerEntry.getSkinTextures().texture() != null) {
                        Identifier texture = playerEntry.getSkinTextures().texture();
                        RenderSystem.enableBlend();
                        context.getMatrices().push();
                        context.getMatrices().translate(8, 0, 0);
                        float offColour = entry.wasDead() ? 0.4f : 1f;
                        context.drawTexturedQuad(texture, 0, 8, 0, 8, 0, 8 / 64f, 16 / 64f, 8 / 64f, 16 / 64f, 1f, offColour, offColour, 1f);
                        context.getMatrices().translate(-0.5, -0.5, 0);
                        context.getMatrices().scale(1.125f, 1.125f, 1f);
                        context.drawTexturedQuad(texture, 0, 8, 0, 8, 0, 40 / 64f, 48 / 64f, 8 / 64f, 16 / 64f, 1f, offColour, offColour, 1f);
                        context.getMatrices().pop();
                    }
                    if (entry.wasDead()) {
                        context.getMatrices().translate(13, 0, 0);
                        context.getMatrices().scale(2f, 1f, 1f);
                        context.drawText(renderer, "x", -renderer.getWidth("x") / 2, 0, 0xE10000, false);
                        context.drawText(renderer, "x", -renderer.getWidth("x") / 2, 1, 0x550000, false);
                    }
                    context.getMatrices().pop();
                }
                context.getMatrices().pop();
            } else {
                // Group players by win/lose status
                List<GameRoundEndComponent.RoundEndData> winners = new ArrayList<>();
                List<GameRoundEndComponent.RoundEndData> losers = new ArrayList<>();

                for (GameRoundEndComponent.RoundEndData entry : roundEnd.getPlayers()) {
                    if (entry.isWinner()) {
                        winners.add(entry);
                    } else {
                        losers.add(entry);
                    }
                }

                // Layout constants - more compact
                int maxRows = 4; // 最多渲染4行
                int minPlayersPerRow = 6; // 每行最少6个
                int cardWidth = 36; // Compact width per player card
                int cardHeight = 28; // Height per row (head 16px + role name + small spacing)
                int startY = 16; // Starting Y position below win message
                int titleToCardsGap = 14; // Gap between title and player cards

                // 计算胜利者每行人数：根据总人数动态调整，确保最多4行，每行最少6人
                int winnerPlayersPerRow = Math.max(minPlayersPerRow, (winners.size() + maxRows - 1) / maxRows);
                int winnerRows = winners.isEmpty() ? 0 : Math.min(maxRows, (winners.size() + winnerPlayersPerRow - 1) / winnerPlayersPerRow);

                // Render winner players (no title)
                for (int i = 0; i < winners.size(); i++) {
                    GameRoundEndComponent.RoundEndData entry = winners.get(i);
                    int row = i / winnerPlayersPerRow;
                    int col = i % winnerPlayersPerRow;
                    // Center the row
                    int itemsInThisRow = (row == winnerRows - 1) ? ((winners.size() - 1) % winnerPlayersPerRow + 1) : winnerPlayersPerRow;
                    int rowWidth = itemsInThisRow * cardWidth;
                    int rowStartX = -rowWidth / 2;
                    int x = rowStartX + col * cardWidth + cardWidth / 2 - 8; // Center the head (16px wide) within card
                    int y = startY + row * cardHeight;
                    renderPlayerCard(context, renderer, entry, x, y);
                }

                // Calculate losers section Y position
                int losersStartY = startY + Math.max(1, winnerRows) * cardHeight + 8;

                // Draw losers section title (centered)
                if (!losers.isEmpty()) {
                    Text losersTitle = Text.translatable("announcement.result.losers");
                    int losersTitleX = -renderer.getWidth(losersTitle) / 2;
                    context.drawTextWithShadow(renderer, losersTitle, losersTitleX, losersStartY, 0xFF5555);

                    // 计算失败者每行人数：根据总人数动态调整，确保最多4行，每行最少6人
                    int loserPlayersPerRow = Math.max(minPlayersPerRow, (losers.size() + maxRows - 1) / maxRows);
                    int loserRows = Math.min(maxRows, (losers.size() + loserPlayersPerRow - 1) / loserPlayersPerRow);

                    // Render loser players
                    for (int i = 0; i < losers.size(); i++) {
                        GameRoundEndComponent.RoundEndData entry = losers.get(i);
                        int row = i / loserPlayersPerRow;
                        int col = i % loserPlayersPerRow;
                        // Center the row
                        int itemsInThisRow = (row == loserRows - 1) ? ((losers.size() - 1) % loserPlayersPerRow + 1) : loserPlayersPerRow;
                        int rowWidth = itemsInThisRow * cardWidth;
                        int rowStartX = -rowWidth / 2;
                        int x = rowStartX + col * cardWidth + cardWidth / 2 - 8; // Center the head within card
                        int y = losersStartY + titleToCardsGap + row * cardHeight;
                        renderPlayerCard(context, renderer, entry, x, y);
                    }
                }

                context.getMatrices().pop();
            }
        }
    }

    public static void tick() {
        if (MinecraftClient.getInstance().world != null && GameWorldComponent.KEY.get(MinecraftClient.getInstance().world).getGameMode() != WatheGameModes.DISCOVERY) {
            ClientPlayerEntity player = MinecraftClient.getInstance().player;
            if (welcomeTime > 0) {
                switch (welcomeTime) {
                    case 200 -> {
                        if (player != null)
                            player.getWorld().playSound(player, player.getX(), player.getY(), player.getZ(), WatheSounds.UI_RISER, SoundCategory.MASTER, 10f, 1f, player.getRandom().nextLong());
                    }
                    case 180 -> {
                        if (player != null)
                            player.getWorld().playSound(player, player.getX(), player.getY(), player.getZ(), WatheSounds.UI_PIANO, SoundCategory.MASTER, 10f, 1.25f, player.getRandom().nextLong());
                    }
                    case 120 -> {
                        if (player != null)
                            player.getWorld().playSound(player, player.getX(), player.getY(), player.getZ(), WatheSounds.UI_PIANO, SoundCategory.MASTER, 10f, 1.5f, player.getRandom().nextLong());
                    }
                    case 60 -> {
                        if (player != null)
                            player.getWorld().playSound(player, player.getX(), player.getY(), player.getZ(), WatheSounds.UI_PIANO, SoundCategory.MASTER, 10f, 1.75f, player.getRandom().nextLong());
                    }
                    case 1 -> {
                        if (player != null)
                            player.getWorld().playSound(player, player.getX(), player.getY(), player.getZ(), WatheSounds.UI_PIANO_STINGER, SoundCategory.MASTER, 10f, 1f, player.getRandom().nextLong());
                    }
                }
                welcomeTime--;
            }
            if (endTime > 0) {
                if (endTime == END_DURATION - (GameConstants.FADE_TIME * 2)) {
                    if (player != null)
                        player.getWorld().playSound(player, player.getX(), player.getY(), player.getZ(), GameRoundEndComponent.KEY.get(player.getScoreboard()).didWin(player.getUuid()) ? WatheSounds.UI_PIANO_WIN : WatheSounds.UI_PIANO_LOSE, SoundCategory.MASTER, 10f, 1f, player.getRandom().nextLong());
                }
                endTime--;
            }
            GameOptions options = MinecraftClient.getInstance().options;
            if (options != null && options.playerListKey.isPressed()) endTime = Math.max(2, endTime);
        }
    }

    public static void startWelcome(RoleAnnouncementTexts.RoleAnnouncementText role, int killers, int targets) {
        RoundTextRenderer.role = role;
        welcomeTime = WELCOME_DURATION;
        RoundTextRenderer.killers = killers;
        RoundTextRenderer.targets = targets;
    }

    public static void startEnd() {
        welcomeTime = 0;
        endTime = END_DURATION;
    }

    public static boolean isEndAnimationPlaying() {
        return endTime > 0;
    }

    public static void clearEndAnimation() {
        endTime = 0;
    }

    /**
     * Helper method to render a player card with head and role name
     */
    private static void renderPlayerCard(DrawContext context, TextRenderer renderer, GameRoundEndComponent.RoundEndData entry, int x, int y) {
        Identifier texture = getPlayerTexture(entry.player());

        RoleAnnouncementTexts.RoleAnnouncementText role = RoleAnnouncementTexts.getForRole(entry.role());
        Text roleName = role.roleText;

        float r = 1.0f;
        float g = 1.0f;
        float b = 1.0f;
        if (entry.hasLeft()) {
            r = 0.2f;
            g = 0.2f;
            b = 0.2f;
        }else if (entry.wasDead()) {
            r = 1.0f;
            g = 0.4f;
            b = 0.4f;
        }

        if (texture != null) {
            RenderSystem.enableBlend();
            context.getMatrices().push();
            context.getMatrices().scale(2f, 2f, 1f);
            context.getMatrices().translate(x / 2f, y / 2f, 0);

            // Draw base head (8x8) - 使用 r, g, b
            context.drawTexturedQuad(texture, 0, 8, 0, 8, 0, 8 / 64f, 16 / 64f, 8 / 64f, 16 / 64f, r, g, b, 1f);

            // Draw hat layer - 使用 r, g, b
            context.getMatrices().translate(-0.5, -0.5, 0);
            context.getMatrices().scale(1.125f, 1.125f, 1f);
            context.drawTexturedQuad(texture, 0, 8, 0, 8, 0, 40 / 64f, 48 / 64f, 8 / 64f, 16 / 64f, r, g, b, 1f);

            context.getMatrices().pop();
        }
        // 渲染状态标记（符号）
        switch (entry.endStatus()) {
            case DEAD,LEFT_DEAD -> {
                // 死亡标记 - 红色 X
                context.getMatrices().push();
                context.getMatrices().scale(2f, 2f, 1f);
                context.getMatrices().translate(x / 2f + 5, y / 2f, 0);
                context.getMatrices().scale(2f, 1f, 1f);
                context.drawText(renderer, "x", -renderer.getWidth("x") / 2, 0, 0xE10000, false);
                context.drawText(renderer, "x", -renderer.getWidth("x") / 2, 1, 0x550000, false);
                context.getMatrices().pop();
            }
            case ALIVE -> {
            }
        }

        // Render role name below head
        context.drawTextWithShadow(renderer, roleName, x + 8 - renderer.getWidth(roleName) / 2, y + 18, role.colour);
    }

}