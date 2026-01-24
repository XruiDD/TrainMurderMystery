package dev.doctor4t.wathe.util;

import dev.doctor4t.wathe.Wathe;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

public class ShopEntry {
    private final String id;
    private final ItemStack displayStack;
    private final @Nullable ItemStack actualStack;
    private final int price;
    private final Type type;
    private final int cooldownTicks;
    private final int initialCooldownTicks;
    private final int maxStock;
    private final @Nullable Predicate<PlayerEntity> customBuyHandler;

    public enum Type {
        WEAPON("gui/shop_slot_weapon"),
        POISON("gui/shop_slot_poison"),
        TOOL("gui/shop_slot_tool");

        final Identifier texture;

        Type(String texture) {
            this.texture = Wathe.id(texture);
        }

        public Identifier getTexture() {
            return texture;
        }
    }

    /**
     * Legacy constructor for backward compatibility.
     */
    public ShopEntry(ItemStack stack, int price, Type type) {
        this.id = generateIdFromStack(stack);
        this.displayStack = stack;
        this.actualStack = null;
        this.price = price;
        this.type = type;
        this.cooldownTicks = 0;
        this.initialCooldownTicks = 0;
        this.maxStock = -1;
        this.customBuyHandler = null;
    }

    protected ShopEntry(String id, ItemStack displayStack, @Nullable ItemStack actualStack,
                       int price, Type type, int cooldownTicks, int initialCooldownTicks, int maxStock,
                       @Nullable Predicate<PlayerEntity> customBuyHandler) {
        this.id = id;
        this.displayStack = displayStack;
        this.actualStack = actualStack;
        this.price = price;
        this.type = type;
        this.cooldownTicks = cooldownTicks;
        this.initialCooldownTicks = initialCooldownTicks;
        this.maxStock = maxStock;
        this.customBuyHandler = customBuyHandler;
    }

    private static String generateIdFromStack(ItemStack stack) {
        return stack.getItem().toString().replace(":", "_");
    }

    public boolean onBuy(@NotNull PlayerEntity player) {
        if (customBuyHandler != null) {
            return customBuyHandler.test(player);
        }
        ItemStack stackToGive = getActualStack().copy();
        return insertStackInFreeSlot(player, stackToGive);
    }

    public static boolean insertStackInFreeSlot(@NotNull PlayerEntity player, ItemStack stackToInsert) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.isEmpty()) {
                player.getInventory().setStack(i, stackToInsert);
                return true;
            }
        }
        return false;
    }

    public String id() {
        return this.id;
    }

    public ItemStack stack() {
        return this.displayStack;
    }

    public ItemStack displayStack() {
        return this.displayStack;
    }

    public ItemStack getActualStack() {
        return this.actualStack != null ? this.actualStack : this.displayStack;
    }

    public int price() {
        return this.price;
    }

    public Type type() {
        return type;
    }

    public int cooldownTicks() {
        return this.cooldownTicks;
    }

    public int initialCooldownTicks() {
        return this.initialCooldownTicks;
    }

    public int maxStock() {
        return this.maxStock;
    }

    public boolean hasStockLimit() {
        return this.maxStock > 0;
    }

    public boolean hasCooldown() {
        return this.cooldownTicks > 0;
    }

    public boolean hasInitialCooldown() {
        return this.initialCooldownTicks > 0;
    }

    public static class Builder {
        private final String id;
        private final ItemStack displayStack;
        private final int price;
        private final Type type;

        private @Nullable ItemStack actualStack = null;
        private int cooldownTicks = 0;
        private int initialCooldownTicks = 0;
        private int maxStock = -1;
        private @Nullable Predicate<PlayerEntity> customBuyHandler = null;

        public Builder(String id, ItemStack displayStack, int price, Type type) {
            this.id = id;
            this.displayStack = displayStack;
            this.price = price;
            this.type = type;
        }

        public Builder actualStack(ItemStack stack) {
            this.actualStack = stack;
            return this;
        }

        public Builder cooldown(int ticks) {
            this.cooldownTicks = ticks;
            return this;
        }

        public Builder initialCooldown(int ticks) {
            this.initialCooldownTicks = ticks;
            return this;
        }

        public Builder stock(int max) {
            this.maxStock = max;
            return this;
        }

        public Builder onBuy(Predicate<PlayerEntity> handler) {
            this.customBuyHandler = handler;
            return this;
        }

        public ShopEntry build() {
            return new ShopEntry(id, displayStack, actualStack, price, type,
                                 cooldownTicks, initialCooldownTicks, maxStock, customBuyHandler);
        }
    }
}
