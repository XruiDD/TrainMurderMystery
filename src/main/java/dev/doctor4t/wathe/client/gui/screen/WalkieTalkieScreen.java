package dev.doctor4t.wathe.client.gui.screen;

import dev.doctor4t.wathe.index.WatheDataComponentTypes;
import dev.doctor4t.wathe.item.WalkieTalkieItem;
import dev.doctor4t.wathe.item.component.WalkieTalkieComponent;
import dev.doctor4t.wathe.util.WalkieTalkieChannelPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;

public class WalkieTalkieScreen extends Screen {
    // 列车谋杀案风格配色
    private static final int BRASS_COLOR = 0xFFD4AF37;
    private static final int BRASS_DIM = 0xFF8B735B;
    private static final int PANEL_BG = 0xFF2A1515;
    private static final int TEXT_LIGHT = 0xFFFDF5E6;
    private static final int TEXT_DIM = 0xFF8B7B6B;
    private static final int BUTTON_BG = 0xFF3A2020;
    private static final int BUTTON_HOVER = 0xFF4A3030;

    private static final int PANEL_WIDTH = 180;
    private static final int PANEL_HEIGHT = 180;

    private int channel;
    private final Hand hand;
    private TextFieldWidget channelInput;

    public WalkieTalkieScreen(int channel, Hand hand) {
        super(Text.translatable("screen.wathe.walkie_talkie.title"));
        this.channel = channel;
        this.hand = hand;
    }

    @Override
    protected void init() {
        super.init();

        int centerX = width / 2;
        int centerY = height / 2;
        int panelTop = centerY - PANEL_HEIGHT / 2;

        // 频道输入框
        int inputWidth = 60;
        int inputHeight = 20;
        channelInput = new TextFieldWidget(textRenderer, centerX - inputWidth / 2, panelTop + 88, inputWidth, inputHeight, Text.empty());
        channelInput.setMaxLength(2);
        channelInput.setText(String.valueOf(channel));
        channelInput.setChangedListener(this::onChannelInputChanged);
        addDrawableChild(channelInput);
    }

    private void onChannelInputChanged(String text) {
        if (text.isEmpty()) return;
        try {
            int parsed = Integer.parseInt(text);
            int clamped = MathHelper.clamp(parsed, 0, WalkieTalkieChannelPayload.MAX_CHANNEL);
            if (clamped != channel) {
                channel = clamped;
                syncChannel();
            }
        } catch (NumberFormatException ignored) {
            // 非数字输入，恢复到当前频道
            channelInput.setText(String.valueOf(channel));
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        // 不渲染默认的全屏模糊和暗色背景
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        int centerX = width / 2;
        int centerY = height / 2;
        int panelLeft = centerX - PANEL_WIDTH / 2;
        int panelTop = centerY - PANEL_HEIGHT / 2;

        // 背景面板
        context.fill(panelLeft - 2, panelTop - 2, panelLeft + PANEL_WIDTH + 2, panelTop + PANEL_HEIGHT + 2, BRASS_DIM);
        context.fill(panelLeft, panelTop, panelLeft + PANEL_WIDTH, panelTop + PANEL_HEIGHT, PANEL_BG);

        // 顶部装饰线
        context.fill(panelLeft, panelTop, panelLeft + PANEL_WIDTH, panelTop + 2, BRASS_COLOR);

        // 标题
        Text title = Text.translatable("screen.wathe.walkie_talkie.title");
        int titleWidth = textRenderer.getWidth(title);
        context.drawText(textRenderer, title, centerX - titleWidth / 2, panelTop + 10, BRASS_COLOR, false);

        // 频道标签
        Text channelLabel = Text.translatable("screen.wathe.walkie_talkie.channel");
        int labelWidth = textRenderer.getWidth(channelLabel);
        context.drawText(textRenderer, channelLabel, centerX - labelWidth / 2, panelTop + 30, TEXT_DIM, false);

        // 频道号（大号显示）
        String channelStr = String.format("%02d", channel);
        Text channelText = Text.literal(channelStr);

        context.getMatrices().push();
        float scale = 4.0f;
        int scaledWidth = (int) (textRenderer.getWidth(channelText) * scale);
        float textX = centerX - scaledWidth / 2.0f;
        float textY = panelTop + 46;
        context.getMatrices().translate(textX, textY, 0);
        context.getMatrices().scale(scale, scale, 1.0f);
        context.drawText(textRenderer, channelText, 0, 0, TEXT_LIGHT, false);
        context.getMatrices().pop();

        // 按钮区域
        int buttonWidth = 60;
        int buttonHeight = 24;
        int buttonY = panelTop + 120;
        int buttonGap = 20;

        // [-] 按钮
        int minusBtnX = centerX - buttonWidth - buttonGap / 2;
        boolean minusHovered = isInBounds(mouseX, mouseY, minusBtnX, buttonY, buttonWidth, buttonHeight);
        context.fill(minusBtnX, buttonY, minusBtnX + buttonWidth, buttonY + buttonHeight, minusHovered ? BUTTON_HOVER : BUTTON_BG);
        context.fill(minusBtnX, buttonY, minusBtnX + buttonWidth, buttonY + 1, BRASS_DIM);
        Text minusText = Text.literal("- 1");
        int minusTextW = textRenderer.getWidth(minusText);
        context.drawText(textRenderer, minusText, minusBtnX + (buttonWidth - minusTextW) / 2, buttonY + (buttonHeight - 8) / 2, TEXT_LIGHT, false);

        // [+] 按钮
        int plusBtnX = centerX + buttonGap / 2;
        boolean plusHovered = isInBounds(mouseX, mouseY, plusBtnX, buttonY, buttonWidth, buttonHeight);
        context.fill(plusBtnX, buttonY, plusBtnX + buttonWidth, buttonY + buttonHeight, plusHovered ? BUTTON_HOVER : BUTTON_BG);
        context.fill(plusBtnX, buttonY, plusBtnX + buttonWidth, buttonY + 1, BRASS_DIM);
        Text plusText = Text.literal("+ 1");
        int plusTextW = textRenderer.getWidth(plusText);
        context.drawText(textRenderer, plusText, plusBtnX + (buttonWidth - plusTextW) / 2, buttonY + (buttonHeight - 8) / 2, TEXT_LIGHT, false);

        // 底部装饰线
        context.fill(panelLeft, panelTop + PANEL_HEIGHT - 2, panelLeft + PANEL_WIDTH, panelTop + PANEL_HEIGHT, BRASS_COLOR);

        // 渲染子控件（TextFieldWidget 等）
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int centerX = width / 2;
            int centerY = height / 2;
            int panelTop = centerY - PANEL_HEIGHT / 2;
            int buttonWidth = 60;
            int buttonHeight = 24;
            int buttonY = panelTop + 120;
            int buttonGap = 20;

            // [-] 按钮
            int minusBtnX = centerX - buttonWidth - buttonGap / 2;
            if (isInBounds((int) mouseX, (int) mouseY, minusBtnX, buttonY, buttonWidth, buttonHeight)) {
                adjustChannel(-1);
                return true;
            }

            // [+] 按钮
            int plusBtnX = centerX + buttonGap / 2;
            if (isInBounds((int) mouseX, (int) mouseY, plusBtnX, buttonY, buttonWidth, buttonHeight)) {
                adjustChannel(1);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (verticalAmount > 0) {
            adjustChannel(1);
            return true;
        } else if (verticalAmount < 0) {
            adjustChannel(-1);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private void adjustChannel(int delta) {
        int newChannel = MathHelper.clamp(channel + delta, 0, WalkieTalkieChannelPayload.MAX_CHANNEL);
        if (newChannel == channel) return;

        channel = newChannel;
        channelInput.setText(String.valueOf(channel));
        playClickSound();
        syncChannel();
    }

    private void syncChannel() {
        // 发送到服务器
        ClientPlayNetworking.send(new WalkieTalkieChannelPayload(channel, hand == Hand.MAIN_HAND));

        // 即时更新客户端 ItemStack
        if (client != null && client.player != null) {
            ItemStack stack = client.player.getStackInHand(hand);
            if (stack.getItem() instanceof WalkieTalkieItem) {
                stack.set(WatheDataComponentTypes.WALKIE_TALKIE, new WalkieTalkieComponent(channel));
            }
        }
    }

    private void playClickSound() {
        if (client != null) {
            client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0f));
        }
    }

    private static boolean isInBounds(int mouseX, int mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }
}
