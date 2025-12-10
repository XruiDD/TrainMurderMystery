package dev.doctor4t.trainmurdermystery.client.gui;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.doctor4t.trainmurdermystery.api.Faction;
import dev.doctor4t.trainmurdermystery.api.TMMGameModes;
import dev.doctor4t.trainmurdermystery.api.TMMRoles;
import dev.doctor4t.trainmurdermystery.cca.GameRoundEndComponent;
import dev.doctor4t.trainmurdermystery.cca.GameWorldComponent;
import dev.doctor4t.trainmurdermystery.client.TMMClient;
import dev.doctor4t.trainmurdermystery.game.GameConstants;
import dev.doctor4t.trainmurdermystery.game.GameFunctions;
import dev.doctor4t.trainmurdermystery.index.TMMSounds;
import net.minecraft.block.entity.SkullBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
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

    @SuppressWarnings("IntegerDivisionInFloatingPointContext")
    public static void renderHud(TextRenderer renderer, ClientPlayerEntity player, @NotNull DrawContext context) {
        boolean isLooseEnds = GameWorldComponent.KEY.get(player.getWorld()).getGameMode() == TMMGameModes.LOOSE_ENDS;

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
        if (endTime > 0 && endTime < END_DURATION - (GameConstants.FADE_TIME * 2) && !game.isRunning() && game.getGameMode() != TMMGameModes.DISCOVERY) {
            GameRoundEndComponent roundEnd = GameRoundEndComponent.KEY.get(player.getWorld());
            if (roundEnd.getWinStatus() == GameFunctions.WinStatus.NONE) return;
            Text endText = null;

            if (roundEnd.getWinStatus() == GameFunctions.WinStatus.NEUTRAL)
            {
                List<GameRoundEndComponent. RoundEndData> players = roundEnd.getPlayers();
                for (GameRoundEndComponent.RoundEndData entry : roundEnd.getPlayers()) {
                    if(entry.isWinner()){
                        endText = RoleAnnouncementTexts.getForRole(entry.role()).winText;
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
            context.drawTextWithShadow(renderer, endText, -renderer.getWidth(endText) / 2, -12, 0xFFFFFF);
            context.getMatrices().pop();
            context.getMatrices().push();
            context.getMatrices().scale(1.2f, 1.2f, 1f);
            MutableText winMessage = Text.translatable("game.win." + roundEnd.getWinStatus().name().toLowerCase().toLowerCase());
            context.drawTextWithShadow(renderer, winMessage, -renderer.getWidth(winMessage) / 2, -4, 0xFFFFFF);
            context.getMatrices().pop();
            if (isLooseEnds) {
                context.drawTextWithShadow(renderer, RoleAnnouncementTexts.LOOSE_END.titleText, -renderer.getWidth(RoleAnnouncementTexts.LOOSE_END.titleText) / 2, 14, 0xFFFFFF);
                int looseEnds = 0;
                for (GameRoundEndComponent.RoundEndData entry : roundEnd.getPlayers()) {
                    context.getMatrices().push();
                    context.getMatrices().scale(2f, 2f, 1f);
                    context.getMatrices().translate(((looseEnds % 6) - 3.5) * 12, 14 + (looseEnds / 6) * 12, 0);
                    looseEnds++;
                    PlayerListEntry playerEntry = TMMClient.PLAYER_ENTRIES_CACHE.get(entry.player().getId());
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
                // Group players by faction
                List<GameRoundEndComponent.RoundEndData> civilianPlayers = new ArrayList<>();
                List<GameRoundEndComponent.RoundEndData> killerPlayers = new ArrayList<>();
                List<GameRoundEndComponent.RoundEndData> neutralPlayers = new ArrayList<>();

                for (GameRoundEndComponent.RoundEndData entry : roundEnd.getPlayers()) {
                    Faction faction = TMMRoles.getRole(entry.role()).getFaction();
                    switch (faction) {
                        case CIVILIAN -> civilianPlayers.add(entry);
                        case KILLER -> killerPlayers.add(entry);
                        case NEUTRAL -> neutralPlayers.add(entry);
                    }
                }

                boolean hasNeutrals = !neutralPlayers.isEmpty();

                // Calculate layout positions based on whether neutrals exist
                int civilianX = hasNeutrals ? -80 : -60;
                int killerX = hasNeutrals ? 0 : 50;
                int neutralX = 80;

                // Calculate vertical offset for killer group based on civilian count
                int civilianRows = (civilianPlayers.size() + 3) / 4; // 4 per row for civilians
                int killerYOffset = hasNeutrals ? 0 : Math.max(0, (civilianRows - 1)) * 12;

                // Draw faction titles
                Text civilianTitle = Text.translatable("announcement.faction.civilian").withColor(RoleAnnouncementTexts.CIVILIAN.colour);
                context.drawTextWithShadow(renderer, civilianTitle, -renderer.getWidth(civilianTitle) / 2 + civilianX, 14, 0xFFFFFF);

                Text killerTitle = Text.translatable("announcement.faction.killer").withColor(RoleAnnouncementTexts.KILLER.colour);
                context.drawTextWithShadow(renderer, killerTitle, -renderer.getWidth(killerTitle) / 2 + killerX, 14 + (hasNeutrals ? 0 : killerYOffset), 0xFFFFFF);

                if (hasNeutrals) {
                    Text neutralTitle = Text.translatable("announcement.faction.neutral").withColor(0xFF9F00);
                    context.drawTextWithShadow(renderer, neutralTitle, -renderer.getWidth(neutralTitle) / 2 + neutralX, 14, 0xFFFFFF);
                }

                // Render civilian players
                int civIndex = 0;
                for (GameRoundEndComponent.RoundEndData entry : civilianPlayers) {
                    context.getMatrices().push();
                    context.getMatrices().scale(2f, 2f, 1f);
                    int columns = hasNeutrals ? 2 : 4;
                    context.getMatrices().translate(civilianX + (civIndex % columns) * 12, 14 + 12 + (civIndex / columns) * 12, 0);
                    renderPlayerHead(context, renderer, entry);
                    context.getMatrices().pop();
                    civIndex++;
                }

                // Render killer players
                int killerIndex = 0;
                for (GameRoundEndComponent.RoundEndData entry : killerPlayers) {
                    context.getMatrices().push();
                    context.getMatrices().scale(2f, 2f, 1f);
                    context.getMatrices().translate(killerX + (killerIndex % 2) * 12, 14 + 12 + (hasNeutrals ? 0 : killerYOffset) + (killerIndex / 2) * 12, 0);
                    renderPlayerHead(context, renderer, entry);
                    context.getMatrices().pop();
                    killerIndex++;
                }

                // Render neutral players (if any)
                if (hasNeutrals) {
                    int neutralIndex = 0;
                    for (GameRoundEndComponent.RoundEndData entry : neutralPlayers) {
                        context.getMatrices().push();
                        context.getMatrices().scale(2f, 2f, 1f);
                        context.getMatrices().translate(neutralX + (neutralIndex % 2) * 12, 14 + 12 + (neutralIndex / 2) * 12, 0);
                        renderPlayerHead(context, renderer, entry);
                        context.getMatrices().pop();
                        neutralIndex++;
                    }
                }

                context.getMatrices().pop();
            }
        }
    }

    public static void tick() {
        if (MinecraftClient.getInstance().world != null && GameWorldComponent.KEY.get(MinecraftClient.getInstance().world).getGameMode() != TMMGameModes.DISCOVERY) {
            ClientPlayerEntity player = MinecraftClient.getInstance().player;
            if (welcomeTime > 0) {
                switch (welcomeTime) {
                    case 200 -> {
                        if (player != null)
                            player.getWorld().playSound(player, player.getX(), player.getY(), player.getZ(), TMMSounds.UI_RISER, SoundCategory.MASTER, 10f, 1f, player.getRandom().nextLong());
                    }
                    case 180 -> {
                        if (player != null)
                            player.getWorld().playSound(player, player.getX(), player.getY(), player.getZ(), TMMSounds.UI_PIANO, SoundCategory.MASTER, 10f, 1.25f, player.getRandom().nextLong());
                    }
                    case 120 -> {
                        if (player != null)
                            player.getWorld().playSound(player, player.getX(), player.getY(), player.getZ(), TMMSounds.UI_PIANO, SoundCategory.MASTER, 10f, 1.5f, player.getRandom().nextLong());
                    }
                    case 60 -> {
                        if (player != null)
                            player.getWorld().playSound(player, player.getX(), player.getY(), player.getZ(), TMMSounds.UI_PIANO, SoundCategory.MASTER, 10f, 1.75f, player.getRandom().nextLong());
                    }
                    case 1 -> {
                        if (player != null)
                            player.getWorld().playSound(player, player.getX(), player.getY(), player.getZ(), TMMSounds.UI_PIANO_STINGER, SoundCategory.MASTER, 10f, 1f, player.getRandom().nextLong());
                    }
                }
                welcomeTime--;
            }
            if (endTime > 0) {
                if (endTime == END_DURATION - (GameConstants.FADE_TIME * 2)) {
                    if (player != null)
                        player.getWorld().playSound(player, player.getX(), player.getY(), player.getZ(), GameRoundEndComponent.KEY.get(player.getWorld()).didWin(player.getUuid()) ? TMMSounds.UI_PIANO_WIN : TMMSounds.UI_PIANO_LOSE, SoundCategory.MASTER, 10f, 1f, player.getRandom().nextLong());
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
    /**
     * Helper method to render a player head with death indicator
     */
    private static void renderPlayerHead(DrawContext context, TextRenderer renderer, GameRoundEndComponent.RoundEndData entry) {
        PlayerListEntry playerEntry = TMMClient.PLAYER_ENTRIES_CACHE.get(entry.player().getId());
        if (playerEntry != null) {
            Identifier texture = playerEntry.getSkinTextures().texture();
            if (texture != null) {
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
        }
    }

}