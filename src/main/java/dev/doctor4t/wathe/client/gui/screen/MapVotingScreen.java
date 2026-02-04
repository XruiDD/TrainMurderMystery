package dev.doctor4t.wathe.client.gui.screen;

import dev.doctor4t.wathe.cca.MapVotingComponent;
import dev.doctor4t.wathe.cca.MapVotingComponent.UnavailableMapEntry;
import dev.doctor4t.wathe.cca.MapVotingComponent.VotingMapEntry;
import dev.doctor4t.wathe.util.MapVotePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.util.List;
import java.util.Random;

/**
 * 地图投票界面 - 东方快车风格优化版
 */
public class MapVotingScreen extends Screen {

    // --- 风格配色方案 (列车谋杀案风格) ---
    // 背景：深色红木/皮革
    private static final int BG_TOP = 0xEE1A0505;
    private static final int BG_BOTTOM = 0xEE0F0202;

    // 装饰线条：做旧黄铜/金
    private static final int BRASS_COLOR = 0xFFD4AF37;
    private static final int BRASS_DIM = 0xFF8B735B;

    // 卡片/车票颜色
    private static final int TICKET_BG = 0xFFFDF5E6; // 旧纸张色 (Old Lace)
    private static final int TICKET_BG_DARK = 0xFFE0D5C0;
    private static final int TICKET_BG_SELECTED = 0xFFFFF8F0;

    // 文本颜色
    private static final int TEXT_INK = 0xFF2F1B1B; // 墨水色
    private static final int TEXT_DIM = 0xFF6B5A5A;
    private static final int TEXT_RED = 0xFFA00000; // 重要信息红

    // 进度条
    private static final int BAR_BG = 0xFF4A3520;
    private static final int BAR_FILL = 0xFFA00000; // 天鹅绒红

    // 尺寸 (基础值，实际值由 computeLayout 计算)
    private static final int BASE_CARD_WIDTH = 140;
    private static final int BASE_CARD_HEIGHT = 125;
    private static final int BASE_CARD_GAP = 12;
    private static final int TARGET_COLS = 5;
    private static final int TARGET_ROWS = 2;
    private static final int CARDS_PER_PAGE = TARGET_COLS * TARGET_ROWS; // 10

    // 计算后的布局参数
    private int cardWidth = BASE_CARD_WIDTH;
    private int cardHeight = BASE_CARD_HEIGHT;
    private int cardGap = BASE_CARD_GAP;
    private int layoutCols = TARGET_COLS;
    private int layoutRows = TARGET_ROWS;
    private int layoutTopY = 55;
    private int layoutPadding = 30;

    private static final int STRIP_CARD_WIDTH = 120;
    private static final int STRIP_CARD_HEIGHT = 80;
    private static final int STRIP_TOTAL_CARDS = 40;
    private static final int STRIP_LANDING_POS = STRIP_TOTAL_CARDS - 8;
    // 滚动时间 7 秒，展示结果 1 秒（服务端总计 8 秒）
    private static final int ROULETTE_SCROLL_TICKS = 7 * 20;

    // 动画状态
    private float slideProgress = 0f;
    private float prevSlideProgress = 0f;
    private static final float SLIDE_SPEED = 0.08f; // 稍微调慢一点显现速度

    // 滚动/翻页状态
    private int scrollRow = 0; // 当前滚动到的行偏移
    private int maxScrollRow = 0; // 最大可滚动行数

    // 轮盘状态
    private int[] rouletteSequence;
    private int rouletteAnimTick = 0;
    private boolean rouletteStripInitialized = false;
    private boolean rouletteResultSoundPlayed = false;
    private int lastRouletteTickIndex = -1; // 上一次经过指针的卡片索引，用于触发滚动音效
    private float cardHoverScale = 0f; // 用于鼠标悬停动画

    public MapVotingScreen() {
        super(Text.translatable("gui.wathe.map_voting.title"));
    }

    @Override
    protected void init() {
        super.init();
        computeLayout();
    }

    private void computeLayout() {
        layoutPadding = 30;
        layoutTopY = 55; // 标题和顶部装饰条下方

        int bottomY = height - 45; // 底部装饰条上方
        int availableWidth = width - layoutPadding * 2;
        int availableHeight = bottomY - layoutTopY;

        // 先尝试目标列数，逐步减少直到卡片不会太窄
        layoutCols = TARGET_COLS;
        while (layoutCols > 1) {
            int testWidth = (availableWidth - (layoutCols - 1) * BASE_CARD_GAP) / layoutCols;
            if (testWidth >= 100) break; // 最小卡片宽度100
            layoutCols--;
        }

        cardGap = BASE_CARD_GAP;
        cardWidth = (availableWidth - (layoutCols - 1) * cardGap) / layoutCols;
        cardWidth = Math.min(cardWidth, 180); // 上限

        // 计算行数：可以容纳的行数
        layoutRows = TARGET_ROWS;
        while (layoutRows > 1) {
            int testHeight = (availableHeight - (layoutRows - 1) * cardGap) / layoutRows;
            if (testHeight >= 90) break; // 最小卡片高度90
            layoutRows--;
        }

        cardHeight = (availableHeight - (layoutRows - 1) * cardGap) / layoutRows;
        cardHeight = Math.min(cardHeight, 150); // 上限

        scrollRow = 0;
    }

    private void updateMaxScroll(int totalCards) {
        int totalRows = (totalCards + layoutCols - 1) / layoutCols;
        maxScrollRow = Math.max(0, totalRows - layoutRows);
        scrollRow = MathHelper.clamp(scrollRow, 0, maxScrollRow);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        MapVotingComponent voting = getVoting();
        return voting == null || !voting.isRoulettePhase();
    }

    private MapVotingComponent getVoting() {
        if (client != null && client.world != null) {
            return MapVotingComponent.KEY.get(client.world.getScoreboard());
        }
        return null;
    }

    @Override
    public void tick() {
        super.tick();
        // 记录上一帧状态用于插值
        prevSlideProgress = slideProgress;

        if (slideProgress < 1f) {
            slideProgress = Math.min(1f, slideProgress + SLIDE_SPEED);
        }

        MapVotingComponent voting = getVoting();
        if (voting != null && voting.isRoulettePhase()) {
            if (!rouletteStripInitialized) {
                initRouletteStrip(voting.getSelectedMapIndex(), voting.getAvailableMaps().size());
                rouletteStripInitialized = true;
                rouletteResultSoundPlayed = false;
                lastRouletteTickIndex = -1;
                rouletteAnimTick = 0;
            }
            rouletteAnimTick++;
        } else {
            rouletteStripInitialized = false;
            if (voting == null || !voting.isRoulettePhase()) {
                rouletteAnimTick = 0;
            }
        }
    }

    private void initRouletteStrip(int selectedMapIndex, int mapCount) {
        if (mapCount <= 0) return;
        rouletteSequence = new int[STRIP_TOTAL_CARDS];
        Random random = new Random();

        // 生成更有随机感的序列，但保证每张图都出现
        for (int i = 0; i < STRIP_TOTAL_CARDS; i++) {
            if (i == STRIP_LANDING_POS) {
                rouletteSequence[i] = selectedMapIndex;
            } else if (i > STRIP_LANDING_POS && i < STRIP_LANDING_POS + 3) {
                // 结果后面几张随机
                rouletteSequence[i] = random.nextInt(mapCount);
            } else {
                // 确保结果不会在着陆点前立刻出现，增加悬念
                int next = i % mapCount;
                if (i == STRIP_LANDING_POS - 1 && next == selectedMapIndex) {
                    next = (next + 1) % mapCount;
                }
                rouletteSequence[i] = next;
            }
        }
    }
    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {

    }
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        MapVotingComponent voting = getVoting();
        if (voting == null || !voting.isVotingActive()) return;

        // 计算插值后的平滑动画进度
        float smoothedSlide = MathHelper.lerp(delta, prevSlideProgress, slideProgress);
        float ease = easeOutBack(smoothedSlide); // 使用带回弹的缓动效果，更生动
        int yOffset = (int) ((1f - ease) * 100);
        int alpha = (int) (smoothedSlide * 255);

        // 背景：深色渐变 + 晕影效果
        context.fillGradient(0, 0, this.width, this.height, BG_TOP, BG_BOTTOM);

        // 绘制顶部和底部的装饰条 (Art Deco 风格)
        renderDecoBars(context, yOffset);

        List<VotingMapEntry> maps = voting.getAvailableMaps();

        if (voting.isRoulettePhase()) {
            // 轮盘阶段
            renderRouletteStrip(context, maps, delta, yOffset);

            // 标题
            Text title = Text.translatable("gui.wathe.map_voting.selecting");
            drawTitle(context, title, 20 - yOffset, 1.5f);
        } else {
            // 投票阶段
            renderVotingCards(context, voting, maps, mouseX, mouseY, yOffset);

            // 标题
            Text title = Text.translatable("gui.wathe.map_voting.title");
            drawTitle(context, title, 16 - yOffset, 1.2f);

            // 倒计时
            int ticksLeft = voting.getVotingTicksRemaining();
            String timeStr = String.format("%d", Math.max(0, ticksLeft / 20));
            int color = ticksLeft < 100 ? 0xFFFF5555 : BRASS_COLOR; // 最后5秒变红
            drawCenteredText(context, timeStr, this.width / 2, 30 - yOffset, color);
        }
        super.render(context, mouseX, mouseY, delta);
    }

    private void renderDecoBars(DrawContext context, int yOffset) {
        int color = BRASS_COLOR;
        // 顶部线条
        context.fill(0, 45 - yOffset, width, 46 - yOffset, color);
        context.fill(0, 48 - yOffset, width, 49 - yOffset, color);

        // 底部线条
        context.fill(0, height - 40 + yOffset, width, height - 39 + yOffset, color);
        context.fill(0, height - 43 + yOffset, width, height - 42 + yOffset, color);
    }

    private void drawTitle(DrawContext context, Text text, int y, float scale) {
        context.getMatrices().push();
        context.getMatrices().translate(width / 2f, y, 0);
        context.getMatrices().scale(scale, scale, 1);
        drawCenteredText(context, text, 0, 0, BRASS_COLOR);
        context.getMatrices().pop();
    }

    private void renderVotingCards(DrawContext context, MapVotingComponent voting,
                                   List<VotingMapEntry> maps, int mouseX, int mouseY, int yOffset) {
        List<UnavailableMapEntry> unavailableMaps = voting.getUnavailableMaps();
        int[] voteCounts = voting.getVoteCounts();

        int totalCards = maps.size() + unavailableMaps.size();
        updateMaxScroll(totalCards);

        // 网格布局居中
        int gridWidth = layoutCols * cardWidth + (layoutCols - 1) * cardGap;
        int startX = (this.width - gridWidth) / 2;
        int baseY = layoutTopY - yOffset;

        int totalVotes = 0;
        for (int c : voteCounts) totalVotes += c;
        int myVote = (client.player != null) ? voting.getVotedMapIndex(client.player.getUuid()) : -1;

        // 计算加权概率（与服务端逻辑一致：无人投票=等概率，有人投票=只算有票的）
        boolean hasAnyVotes = false;
        for (int c : voteCounts) {
            if (c > 0) { hasAnyVotes = true; break; }
        }
        int totalWeight = 0;
        int[] weights = new int[maps.size()];
        for (int i = 0; i < maps.size(); i++) {
            weights[i] = hasAnyVotes ? (i < voteCounts.length ? voteCounts[i] : 0) : 1;
            totalWeight += weights[i];
        }

        // 遍历所有卡片，先可选后不可选
        int firstVisibleIndex = scrollRow * layoutCols;
        int lastVisibleIndex = firstVisibleIndex + layoutRows * layoutCols;

        for (int index = firstVisibleIndex; index < totalCards && index < lastVisibleIndex; index++) {
            int visibleIndex = index - firstVisibleIndex;
            int col = visibleIndex % layoutCols;
            int row = visibleIndex / layoutCols;

            int x = startX + col * (cardWidth + cardGap);
            int y = baseY + row * (cardHeight + cardGap);

            if (index < maps.size()) {
                // 可选地图
                VotingMapEntry map = maps.get(index);
                boolean isHovered = mouseX >= x && mouseX <= x + cardWidth && mouseY >= y && mouseY <= y + cardHeight;
                boolean isMyVote = (index == myVote);

                int hoverOffset = isHovered ? -3 : 0;
                float probability = totalWeight > 0 ? (float) weights[index] / totalWeight : 0f;

                drawTicketCard(context, x, y + hoverOffset, map, true, isMyVote, isHovered,
                        index < voteCounts.length ? voteCounts[index] : 0, totalVotes, probability);
            } else {
                // 不可选地图
                int unavailIdx = index - maps.size();
                UnavailableMapEntry umap = unavailableMaps.get(unavailIdx);
                drawTicketCard(context, x, y, new VotingMapEntry(umap.dimensionId(), umap.displayName(), "", 0, 0),
                        false, false, false, 0, 0, 0f);

                // 不可用原因覆盖层
                context.fill(x + 5, y + 5, x + cardWidth - 5, y + cardHeight - 5, 0xAA000000);
                context.drawBorder(x + 5, y + 5, cardWidth - 10, cardHeight - 10, 0x55FFFFFF);

                Text reasonText = parseUnavailableReason(umap.reason());
                drawCenteredText(context, reasonText, x + cardWidth / 2, y + cardHeight / 2 - 4, 0xFFAAAAAA);
            }
        }

        // 滚动指示器
        if (maxScrollRow > 0) {
            renderScrollIndicator(context, yOffset);
        }
    }

    private void renderScrollIndicator(DrawContext context, int yOffset) {
        int totalPages = maxScrollRow + 1;
        int currentPage = scrollRow + 1;

        // 页码文字
        String pageText = currentPage + " / " + totalPages;
        int indicatorY = height - 35;
        drawCenteredText(context, Text.literal(pageText), width / 2, indicatorY, BRASS_DIM);

        // 上箭头提示
        if (scrollRow > 0) {
            drawTriangle(context, width / 2, layoutTopY - 10 - yOffset, 5, BRASS_COLOR, false);
        }
        // 下箭头提示
        if (scrollRow < maxScrollRow) {
            drawTriangle(context, width / 2, height - 48, 5, BRASS_COLOR, true);
        }
    }

    // 绘制车票风格的卡片
    private void drawTicketCard(DrawContext context, int x, int y, VotingMapEntry map,
                                boolean available, boolean selected, boolean hovered, int votes, int totalVotes, float probability) {
        int cw = cardWidth;
        int ch = cardHeight;

        // 1. 卡片背景 (模仿旧纸张)
        int bgColor = selected ? TICKET_BG_SELECTED : (available ? TICKET_BG : 0xFF504040);
        int shadowColor = 0x66000000;

        // 阴影
        context.fill(x + 4, y + 4, x + cw + 4, y + ch + 4, shadowColor);

        // 主体 - 使用 fillGradient 模拟纸张质感
        context.fillGradient(x, y, x + cw, y + ch, bgColor, available ? TICKET_BG_DARK : 0xFF302020);

        int borderColor = selected ? BRASS_COLOR : (hovered ? 0xFF8B4513 : TEXT_INK);
        if (!available) borderColor = 0xFF2A2A2A;

        // 双重边框 Art Deco 风格
        context.drawBorder(x + 2, y + 2, cw - 4, ch - 4, borderColor);
        context.drawBorder(x + 5, y + 5, cw - 10, ch - 10, borderColor & 0x88FFFFFF);

        // 铆钉/打孔装饰
        int rivetColor = BRASS_DIM;
        context.fill(x + 6, y + 6, x + 8, y + 8, rivetColor);
        context.fill(x + cw - 8, y + 6, x + cw - 6, y + 8, rivetColor);
        context.fill(x + 6, y + ch - 8, x + 8, y + ch - 6, rivetColor);
        context.fill(x + cw - 8, y + ch - 8, x + cw - 6, y + ch - 6, rivetColor);

        // 文字内容
        int titleColor = available ? TEXT_INK : 0xFFAAAAAA;

        // 地图名
        Text name = Text.literal(map.displayName());
        drawCenteredText(context, name, x + cw / 2, y + 15, titleColor);

        if (available) {
            // 人数
            Text players = Text.translatable("gui.wathe.map_voting.player_range", map.minPlayers(), map.maxPlayers());
            drawCenteredText(context, players, x + cw / 2, y + 30, TEXT_DIM);

            // 投票条（固定在底部）
            int barWidth = cw - 24;
            int barX = x + 12;
            int barY = y + ch - 25;

            // 描述 (自动换行多行显示)
            int descStartY = y + 45;
            int lineHeight = 10;
            int descAvailableHeight = barY - descStartY - 16; // 留出概率文字的空间
            int maxLines = Math.max(1, descAvailableHeight / lineHeight);
            List<OrderedText> descLines = textRenderer.wrapLines(StringVisitable.plain(map.description()), cw - 20);
            int linesToRender = Math.min(descLines.size(), maxLines);
            for (int line = 0; line < linesToRender; line++) {
                OrderedText orderedText = descLines.get(line);
                int lineWidth = textRenderer.getWidth(orderedText);
                context.drawText(textRenderer, orderedText, x + (cw - lineWidth) / 2, descStartY + line * lineHeight, TEXT_DIM, false);
            }

            // 概率 (紧跟描述之后)
            int probY = descStartY + linesToRender * lineHeight + 2;
            String probStr = String.format("%.1f%%", probability * 100f);
            int probColor = probability >= 0.5f ? TEXT_RED : BRASS_DIM;
            drawCenteredText(context, Text.literal(probStr), x + cw / 2, probY, probColor);

            // 条底
            context.fill(barX, barY, barX + barWidth, barY + 4, BAR_BG);
            if (totalVotes > 0 && votes > 0) {
                int fillW = (int)(((float)votes / totalVotes) * barWidth);
                context.fill(barX, barY, barX + fillW, barY + 4, BAR_FILL);
            }

            // 票数文字
            Text voteText = Text.literal(votes + " Votes");
            context.getMatrices().push();
            context.getMatrices().translate(x + cw / 2f, barY + 8, 0);
            context.getMatrices().scale(0.8f, 0.8f, 1);
            drawCenteredText(context, voteText, 0, 0, TEXT_DIM);
            context.getMatrices().pop();

            // 如果选中，绘制"印章"
            if (selected) {
                context.drawBorder(x + cw - 30, y + ch - 30, 24, 24, 0xFFA00000);
                context.getMatrices().push();
                context.getMatrices().translate(x + cw - 18, y + ch - 18, 0);
                context.getMatrices().scale(0.6f, 0.6f, 1);
                context.getMatrices().multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotationDegrees(-15));
                drawCenteredText(context, Text.literal("VOTED"), 0, 0, 0xFFA00000);
                context.getMatrices().pop();
            }
        }
    }

    private void renderRouletteStrip(DrawContext context, List<VotingMapEntry> maps, float delta, int yOffset) {
        if (rouletteSequence == null) return;

        int centerY = this.height / 2;
        int centerX = this.width / 2;

        // 使用 delta 插值计算滚动位置，保证帧间平滑
        float smoothTick = rouletteAnimTick + delta;
        float progress = Math.min(1.0f, smoothTick / ROULETTE_SCROLL_TICKS);

        // 两阶段缓动：先加速后减速，模拟真实轮盘
        // 前 15%: 平滑加速 (ease-in)
        // 后 85%: 缓慢减速并锁定 (ease-out)
        float ease;
        if (progress < 0.15f) {
            float t = progress / 0.15f;
            ease = 0.15f * t * t; // 二次加速
        } else {
            float t = (progress - 0.15f) / 0.85f;
            ease = 0.15f + 0.85f * (1f - (1f - t) * (1f - t) * (1f - t)); // 三次减速
        }

        // 计算当前像素偏移
        float totalScrollPixels = STRIP_LANDING_POS * (STRIP_CARD_WIDTH + 8) + STRIP_CARD_WIDTH / 2f;
        float currentScroll = totalScrollPixels * ease;

        // 每当新卡片经过指针时播放 tick 音效
        int currentCardIndex = (int) (currentScroll / (STRIP_CARD_WIDTH + 8));
        if (currentCardIndex != lastRouletteTickIndex && currentCardIndex >= 0 && progress < 0.97f) {
            lastRouletteTickIndex = currentCardIndex;
            // 音调随减速逐渐升高，营造紧张感
            float pitchBase = 0.8f + 0.6f * progress;
            float volume = 0.3f + 0.4f * progress; // 越慢越响
            playSound(SoundEvents.BLOCK_NOTE_BLOCK_HAT.value(), volume, pitchBase);
        }

        // 剪裁区域 (Scissor)
        int stripHeight = STRIP_CARD_HEIGHT + 20;
        context.enableScissor(0, centerY - stripHeight/2, width, centerY + stripHeight/2);

        // 绘制背景槽
        context.fill(0, centerY - stripHeight/2, width, centerY + stripHeight/2, 0xAA000000);
        context.fill(0, centerY - 1, width, centerY + 1, 0x44D4AF37); // 中轴线

        for (int i = 0; i < rouletteSequence.length; i++) {
            int mapIdx = rouletteSequence[i];
            float itemX = i * (STRIP_CARD_WIDTH + 8) + STRIP_CARD_WIDTH / 2f;
            float drawX = centerX + itemX - currentScroll - STRIP_CARD_WIDTH / 2f;

            // 性能优化：只绘制屏幕内的卡片
            if (drawX > width || drawX + STRIP_CARD_WIDTH < 0) continue;

            VotingMapEntry map = maps.get(mapIdx);

            // 越接近中心越大
            float distToCenter = Math.abs((drawX + STRIP_CARD_WIDTH/2f) - centerX);
            float scale = 1.0f - MathHelper.clamp(distToCenter / 300f, 0f, 0.2f);

            // 最终停止时的闪烁特效（滚动基本停止后触发）
            boolean isWinner = (i == STRIP_LANDING_POS) && (progress >= 0.97f);

            // 结果揭晓音效（只播放一次）
            if (isWinner && !rouletteResultSoundPlayed) {
                rouletteResultSoundPlayed = true;
                playSound(SoundEvents.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
            }

            renderRouletteCard(context, (int)drawX, centerY - STRIP_CARD_HEIGHT/2, map, scale, isWinner, smoothTick);
        }

        context.disableScissor();

        // 绘制黄铜指针：上方 ▽ 尖端朝下，下方 △ 尖端朝上
        drawTriangle(context, centerX, centerY - stripHeight / 2, 8, BRASS_COLOR, false);
        drawTriangle(context, centerX, centerY + stripHeight / 2, 8, BRASS_COLOR, true);
    }

    private void renderRouletteCard(DrawContext context, int x, int y, VotingMapEntry map, float scale, boolean isWinner, float time) {
        context.getMatrices().push();
        // 缩放中心
        context.getMatrices().translate(x + STRIP_CARD_WIDTH/2f, y + STRIP_CARD_HEIGHT/2f, 0);
        context.getMatrices().scale(scale, scale, 1f);
        context.getMatrices().translate(-STRIP_CARD_WIDTH/2f, -STRIP_CARD_HEIGHT/2f, 0); // 回到左上角

        int borderColor = isWinner ?
                (Math.sin(time * 0.5) > 0 ? 0xFFFFD700 : BRASS_COLOR) : // 赢家闪烁
                0xFF5A4A4A;

        // 背景
        context.fill(0, 0, STRIP_CARD_WIDTH, STRIP_CARD_HEIGHT, TICKET_BG);
        context.drawBorder(0, 0, STRIP_CARD_WIDTH, STRIP_CARD_HEIGHT, borderColor);

        // 内部装饰线
        context.drawBorder(3, 3, STRIP_CARD_WIDTH - 6, STRIP_CARD_HEIGHT - 6, borderColor & 0x88FFFFFF);

        // 文字
        drawCenteredText(context, Text.literal(map.displayName()),
                STRIP_CARD_WIDTH / 2, 10, TEXT_INK);

        drawCenteredText(context,
                Text.translatable("gui.wathe.map_voting.player_range", map.minPlayers(), map.maxPlayers()),
                STRIP_CARD_WIDTH / 2, 25, TEXT_DIM);

        context.getMatrices().pop();
    }

    // 简单的三角形绘制辅助方法
    private void drawTriangle(DrawContext context, int x, int y, int size, int color, boolean pointDown) {
        int dir = pointDown ? 1 : -1;
        // 使用逐行绘制模拟三角形
        for (int i = 0; i < size; i++) {
            context.fill(x - i, y + i * dir, x + i + 1, y + (i + 1) * dir, color);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        MapVotingComponent voting = getVoting();
        if (voting == null || !voting.isVotingActive() || voting.isRoulettePhase()) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        List<VotingMapEntry> maps = voting.getAvailableMaps();
        int yOffset = (int)((1f - easeOutBack(Math.min(1f, slideProgress))) * 100);

        int gridWidth = layoutCols * cardWidth + (layoutCols - 1) * cardGap;
        int startX = (this.width - gridWidth) / 2;
        int baseY = layoutTopY - yOffset;

        int firstVisibleIndex = scrollRow * layoutCols;
        int lastVisibleIndex = Math.min(maps.size(), firstVisibleIndex + layoutRows * layoutCols);

        for (int index = firstVisibleIndex; index < lastVisibleIndex; index++) {
            int visibleIndex = index - firstVisibleIndex;
            int col = visibleIndex % layoutCols;
            int row = visibleIndex / layoutCols;

            int cardX = startX + col * (cardWidth + cardGap);
            int cardTopY = baseY + row * (cardHeight + cardGap);

            if (mouseX >= cardX && mouseX <= cardX + cardWidth
                    && mouseY >= cardTopY && mouseY <= cardTopY + cardHeight) {
                ClientPlayNetworking.send(new MapVotePayload(index));
                playSound(SoundEvents.UI_BUTTON_CLICK.value(), 1.0f, 1.0f);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (verticalAmount < 0 && scrollRow < maxScrollRow) {
            scrollRow++;
            playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.5f, 1.8f);
            return true;
        } else if (verticalAmount > 0 && scrollRow > 0) {
            scrollRow--;
            playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.5f, 1.8f);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private void playSound(net.minecraft.sound.SoundEvent sound, float volume, float pitch) {
        if (client != null) {
            client.getSoundManager().play(PositionedSoundInstance.master(sound, pitch, volume));
        }
    }

    // 解析不可用原因字符串，格式为 "min_players:6" 或 "max_players:10"
    private Text parseUnavailableReason(String reason) {
        if (reason != null && reason.contains(":")) {
            String[] parts = reason.split(":", 2);
            String key = parts[0];
            String value = parts[1];
            if ("min_players".equals(key)) {
                return Text.translatable("gui.wathe.map_voting.unavailable.min_players", value);
            } else if ("max_players".equals(key)) {
                return Text.translatable("gui.wathe.map_voting.unavailable.max_players", value);
            }
        }
        return Text.translatable("gui.wathe.map_voting.unavailable");
    }

    // 居中绘制文字，无阴影
    private void drawCenteredText(DrawContext context, Text text, int centerX, int y, int color) {
        int w = textRenderer.getWidth(text);
        context.drawText(textRenderer, text, centerX - w / 2, y, color, false);
    }

    private void drawCenteredText(DrawContext context, String text, int centerX, int y, int color) {
        int w = textRenderer.getWidth(text);
        context.drawText(textRenderer, text, centerX - w / 2, y, color, false);
    }

    // 自定义缓动函数：Back Out (冲过头再回弹)
    private float easeOutBack(float x) {
        float c1 = 1.70158f;
        float c3 = c1 + 1;
        return 1 + c3 * (float)Math.pow(x - 1, 3) + c1 * (float)Math.pow(x - 1, 2);
    }
}
