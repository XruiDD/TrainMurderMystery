package dev.doctor4t.trainmurdermystery.client.util;

import dev.doctor4t.trainmurdermystery.index.TrainMurderMysteryItems;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.util.List;

public class TMMItemTooltips {
    public static void addTooltips() {
        ItemTooltipCallback.EVENT.register((itemStack, tooltipContext, tooltipType, list) -> {
            addTooltipsForItem(TrainMurderMysteryItems.KNIFE, 3, itemStack, list);
            addTooltipsForItem(TrainMurderMysteryItems.LOCKPICK, 2, itemStack, list);
        });
    }

    private static void addTooltipsForItem(Item item, int tooltipLineCount, ItemStack itemStack, List<Text> tooltipList) {
        if (itemStack.isOf(item)) {
            addCooldownText(item, tooltipList);

            for (int i = 1; i <= tooltipLineCount; i++) {
                tooltipList.add(Text.translatable("tip." + item.getTranslationKey().substring(24) + ".tooltip" + i).withColor(0x808080));
            }
        }
    }

    private static void addCooldownText(Item item, List<Text> tooltipList) {
        ItemCooldownManager itemCooldownManager = MinecraftClient.getInstance().player.getItemCooldownManager();
        if (itemCooldownManager.isCoolingDown(item)) {
            ItemCooldownManager.Entry knifeEntry = itemCooldownManager.entries.get(item);
            int timeLeft = knifeEntry.endTick - itemCooldownManager.tick;

            if (timeLeft > 0) {
                int minutes = (int) Math.floor((double) timeLeft / 1200);
                int seconds = (timeLeft - (minutes * 1200)) / 20;
                String countdown = (minutes > 0 ? minutes + "m" : "") + (seconds > 0 ? seconds + "s" : "");
                tooltipList.add(Text.translatable("tip.cooldown", countdown).withColor(0xC90000));
            }
        }
    }
}
