package dev.doctor4t.wathe.client.util;

import dev.doctor4t.ratatouille.util.TextUtils;
import dev.doctor4t.wathe.index.WatheDataComponentTypes;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.item.component.CosmeticComponent;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

public class WatheItemTooltips {
    public static final int COOLDOWN_COLOR = 0xC90000;
    public static final int LETTER_COLOR = 0xC5AE8B;
    public static final int REGULAR_TOOLTIP_COLOR = 0x808080;

    private static final Map<String, Integer> RARITY_COLORS = Map.of(
            "WHITE", 0xA8A29E,
            "GREEN", 0x10B981,
            "BLUE", 0x3B82F6,
            "PURPLE", 0xA855F7,
            "ORANGE", 0xFF5E00
    );

    private static final Map<String, String> RARITY_KEYS = Map.of(
            "WHITE", "tip.skin.rarity.white",
            "GREEN", "tip.skin.rarity.green",
            "BLUE", "tip.skin.rarity.blue",
            "PURPLE", "tip.skin.rarity.purple",
            "ORANGE", "tip.skin.rarity.orange"
    );

    public static void addTooltips() {
        ItemTooltipCallback.EVENT.register((itemStack, tooltipContext, tooltipType, tooltipList) -> {
            addCooldownText(WatheItems.KNIFE, tooltipList, itemStack);
            addCooldownText(WatheItems.REVOLVER, tooltipList, itemStack);
            addCooldownText(WatheItems.DERRINGER, tooltipList, itemStack);
            addCooldownText(WatheItems.GRENADE, tooltipList, itemStack);
            addCooldownText(WatheItems.LOCKPICK, tooltipList, itemStack);
            addCooldownText(WatheItems.CROWBAR, tooltipList, itemStack);
            addCooldownText(WatheItems.BODY_BAG, tooltipList, itemStack);
            addCooldownText(WatheItems.PSYCHO_MODE, tooltipList, itemStack);
            addCooldownText(WatheItems.BLACKOUT, tooltipList, itemStack);

            addTooltipForItem(WatheItems.KNIFE, itemStack, tooltipList);
            addTooltipForItem(WatheItems.REVOLVER, itemStack, tooltipList);
            addTooltipForItem(WatheItems.DERRINGER, itemStack, tooltipList);
            addTooltipForItem(WatheItems.GRENADE, itemStack, tooltipList);
            addTooltipForItem(WatheItems.PSYCHO_MODE, itemStack, tooltipList);
            addTooltipForItem(WatheItems.POISON_VIAL, itemStack, tooltipList);
            addTooltipForItem(WatheItems.SCORPION, itemStack, tooltipList);
            addTooltipForItem(WatheItems.FIRECRACKER, itemStack, tooltipList);
            addTooltipForItem(WatheItems.LOCKPICK, itemStack, tooltipList);
            addTooltipForItem(WatheItems.CROWBAR, itemStack, tooltipList);
            addTooltipForItem(WatheItems.BODY_BAG, itemStack, tooltipList);
            addTooltipForItem(WatheItems.BLACKOUT, itemStack, tooltipList);
            addTooltipForItem(WatheItems.NOTE, itemStack, tooltipList);

            // Universal skin tooltip for all items with a SKIN component
            CosmeticComponent skin = itemStack.get(WatheDataComponentTypes.SKIN);
            if (skin != null && !"default".equals(skin.cosmeticId())) {
                int rarityColor = RARITY_COLORS.getOrDefault(skin.rarity(), 0xA8A29E);
                String rawName = skin.displayName();

                // Only attempt JSON Text parsing if it looks like JSON (object or array);
                // plain text gets auto-colored with rarity color
                Text skinName;
                if (rawName.startsWith("{") || rawName.startsWith("[")) {
                    var player = MinecraftClient.getInstance().player;
                    Text parsed = player != null
                            ? Text.Serialization.fromJson(rawName, player.getRegistryManager())
                            : null;
                    skinName = parsed != null ? parsed : Text.literal(rawName).styled(s -> s.withColor(rarityColor));
                } else {
                    skinName = Text.literal(rawName).styled(s -> s.withColor(rarityColor));
                }

                Text line = Text.translatable("tip.skin")
                        .styled(s -> s.withColor(Colors.GRAY))
                        .append(skinName);

                String rarityKey = RARITY_KEYS.get(skin.rarity());
                if (rarityKey != null) {
                    line = line.copy()
                            .append(Text.literal(" [").styled(s -> s.withColor(rarityColor)))
                            .append(Text.translatable(rarityKey).styled(s -> s.withColor(rarityColor)))
                            .append(Text.literal("]").styled(s -> s.withColor(rarityColor)));
                }

                tooltipList.add(line);
            }
        });
    }

    private static void addTooltipForItem(Item item, @NotNull ItemStack itemStack, List<Text> tooltipList) {
        if (itemStack.isOf(item)) {
            tooltipList.addAll(TextUtils.getTooltipForItem(item, Style.EMPTY.withColor(REGULAR_TOOLTIP_COLOR)));
        }
    }

    private static void addCooldownText(Item item, List<Text> tooltipList, @NotNull ItemStack itemStack) {
        if (!itemStack.isOf(item)) return;
        ItemCooldownManager itemCooldownManager = MinecraftClient.getInstance().player.getItemCooldownManager();
        if (itemCooldownManager.isCoolingDown(item)) {
            ItemCooldownManager.Entry knifeEntry = itemCooldownManager.entries.get(item);
            int timeLeft = knifeEntry.endTick - itemCooldownManager.tick;
            if (timeLeft > 0) {
                int minutes = (int) Math.floor((double) timeLeft / 1200);
                int seconds = (timeLeft - (minutes * 1200)) / 20;
                String countdown = (minutes > 0 ? minutes + "m" : "") + (seconds > 0 ? seconds + "s" : "");
                tooltipList.add(Text.translatable("tip.cooldown", countdown).withColor(COOLDOWN_COLOR));
            }
        }
    }
}
