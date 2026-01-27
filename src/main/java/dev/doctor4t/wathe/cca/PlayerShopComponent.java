package dev.doctor4t.wathe.cca;

import dev.doctor4t.wathe.Wathe;
import dev.doctor4t.wathe.api.event.ShopPurchase;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.index.WatheSounds;
import dev.doctor4t.wathe.util.ShopEntry;
import dev.doctor4t.wathe.util.ShopUtils;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlayerShopComponent implements AutoSyncedComponent, ServerTickingComponent {
    public static final ComponentKey<PlayerShopComponent> KEY = ComponentRegistry.getOrCreate(Wathe.id("shop"), PlayerShopComponent.class);
    private final PlayerEntity player;
    public int balance = 0;

    // Custom cooldown system - entry ID -> remaining cooldown ticks
    private final Map<String, Integer> cooldowns = new HashMap<>();

    // Stock system - entry ID -> remaining stock
    private final Map<String, Integer> stock = new HashMap<>();

    // Max stock cache - entry ID -> max stock (for display)
    private final Map<String, Integer> maxStockCache = new HashMap<>();

    public PlayerShopComponent(PlayerEntity player) {
        this.player = player;
    }

    @Override
    public boolean shouldSyncWith(ServerPlayerEntity player) {
        return player == this.player;
    }

    public void sync() {
        KEY.sync(this.player);
    }

    // === Shop Initialization ===

    /**
     * Initializes the shop state for a new game.
     * Should be called after roles are assigned.
     *
     * @param entries the shop entries available to this player
     */
    public void initializeShop(List<ShopEntry> entries) {
        cooldowns.clear();
        stock.clear();
        maxStockCache.clear();

        for (ShopEntry entry : entries) {
            // Set initial cooldowns
            if (entry.hasInitialCooldown()) {
                cooldowns.put(entry.id(), entry.initialCooldownTicks());
            }

            // Set initial stock
            if (entry.hasStockLimit()) {
                stock.put(entry.id(), entry.maxStock());
                maxStockCache.put(entry.id(), entry.maxStock());
            }
        }

        this.sync();
    }

    // === Cooldown Management ===

    /**
     * Checks if an entry is on cooldown.
     */
    public boolean isOnCooldown(String entryId) {
        return cooldowns.getOrDefault(entryId, 0) > 0;
    }

    /**
     * Gets the remaining cooldown ticks for an entry.
     */
    public int getRemainingCooldown(String entryId) {
        return cooldowns.getOrDefault(entryId, 0);
    }

    /**
     * Applies cooldown after a successful purchase.
     */
    private void applyCooldown(ShopEntry entry) {
        if (entry.hasCooldown()) {
            cooldowns.put(entry.id(), entry.cooldownTicks());
        }
    }

    // === Stock Management ===

    /**
     * Checks if an entry is in stock.
     */
    public boolean isInStock(String entryId) {
        if (!maxStockCache.containsKey(entryId)) {
            // No stock limit
            return true;
        }
        return stock.getOrDefault(entryId, 0) > 0;
    }

    /**
     * Gets the remaining stock for an entry.
     * Returns -1 if there's no stock limit.
     */
    public int getRemainingStock(String entryId) {
        if (!maxStockCache.containsKey(entryId)) {
            return -1;
        }
        return stock.getOrDefault(entryId, 0);
    }

    /**
     * Gets the maximum stock for an entry.
     * Returns -1 if there's no stock limit.
     */
    public int getMaxStock(String entryId) {
        return maxStockCache.getOrDefault(entryId, -1);
    }

    /**
     * Consumes one stock for an entry.
     */
    private void consumeStock(String entryId) {
        if (maxStockCache.containsKey(entryId)) {
            int remaining = stock.getOrDefault(entryId, 0);
            if (remaining > 0) {
                stock.put(entryId, remaining - 1);
            }
        }
    }

    // === Tick ===

    private static final int SYNC_INTERVAL = 20; // Sync every second (20 ticks)
    private int syncTickCounter = 0;

    @Override
    public void serverTick() {
        if (cooldowns.isEmpty()) {
            syncTickCounter = 0;
            return;
        }

        boolean cooldownEnded = false;
        var iterator = cooldowns.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            int remaining = entry.getValue();
            if (remaining > 0) {
                entry.setValue(remaining - 1);
                if (remaining - 1 <= 0) {
                    iterator.remove();
                    cooldownEnded = true; // A cooldown just ended
                }
            }
        }

        // Sync immediately when a cooldown ends (important for UI)
        if (cooldownEnded) {
            syncTickCounter = 0;
            this.sync();
            return;
        }

        // Otherwise, sync periodically (every second)
        syncTickCounter++;
        if (syncTickCounter >= SYNC_INTERVAL) {
            syncTickCounter = 0;
            this.sync();
        }
    }

    // === Reset ===

    public void reset() {
        this.balance = 0;
        this.cooldowns.clear();
        this.stock.clear();
        this.maxStockCache.clear();
        this.sync();
    }

    // === Balance Management ===

    public void addToBalance(int amount) {
        this.setBalance(this.balance + amount);
    }

    public void setBalance(int amount) {
        if (this.balance != amount) {
            this.balance = amount;
            this.sync();
        }
    }

    // === Purchase Logic ===

    private void sendPurchaseError(String translationKey) {
        Text message = Text.translatable(translationKey).formatted(Formatting.DARK_RED);
        this.player.sendMessage(message, true);

        if (this.player instanceof ServerPlayerEntity serverPlayer) {
            playSound(serverPlayer, WatheSounds.UI_SHOP_BUY_FAIL);
        }
    }

    private void playSound(ServerPlayerEntity player, SoundEvent sound) {
        player.networkHandler.sendPacket(
            new PlaySoundS2CPacket(
                Registries.SOUND_EVENT.getEntry(sound),
                SoundCategory.PLAYERS,
                player.getX(),
                player.getY(),
                player.getZ(),
                1.0f,
                0.9f + this.player.getRandom().nextFloat() * 0.2f,
                this.player.getRandom().nextLong()
            )
        );
    }

    public void tryBuy(int index) {
        // SECURITY: Only allow purchases on server side
        if (!(this.player instanceof ServerPlayerEntity serverPlayer)) {
            return;
        }

        if(!GameFunctions.isPlayerPlayingAndAlive(this.player)){
            sendPurchaseError("shop.error.purchase_denied");
            return;
        }

        // Development environment: auto-fill balance if needed
        if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
            ensureSufficientBalanceForTesting(index);
        }

        // Build shop entries for this player
        List<ShopEntry> entries = buildShopEntries();
        if (entries.isEmpty()) {
            sendPurchaseError("shop.error.not_available");
            return;
        }

        // Validate index
        if (index < 0 || index >= entries.size()) {
            sendPurchaseError("shop.error.invalid_item");
            return;
        }

        ShopEntry entry = entries.get(index);

        // Check cooldown
        if (isOnCooldown(entry.id())) {
            sendPurchaseError("shop.error.on_cooldown");
            return;
        }

        // Check stock
        if (!isInStock(entry.id())) {
            sendPurchaseError("shop.error.out_of_stock");
            return;
        }

        // Check if purchase is allowed via event
        ShopPurchase.PurchaseResult purchaseResult = ShopPurchase.BEFORE.invoker()
            .beforePurchase(serverPlayer, entry, index);

        if (purchaseResult != null && !purchaseResult.allowed()) {
            sendPurchaseError(purchaseResult.denyReason() == null ? "shop.error.purchase_denied" : purchaseResult.denyReason());
            return;
        }

        // Determine actual price (may be modified by event)
        int actualPrice = purchaseResult != null && purchaseResult.hasModifiedPrice()
            ? purchaseResult.modifiedPrice()
            : entry.price();

        // Execute purchase
        if (canAffordAndBuy(entry, actualPrice)) {
            completePurchase(serverPlayer, entry, index, actualPrice);
        } else {
            sendPurchaseError("shop.error.purchase_failed");
        }

        this.sync();
    }

    private List<ShopEntry> buildShopEntries() {
        return ShopUtils.getShopEntriesForPlayer(player);
    }

    private void ensureSufficientBalanceForTesting(int index) {
        // In dev environment, automatically set balance for testing
        List<ShopEntry> entries = buildShopEntries();
        if (index >= 0 && index < entries.size()) {
            int requiredPrice = entries.get(index).price();
            if (this.balance < requiredPrice) {
                this.balance = requiredPrice * 10;
            }
        }
    }

    private boolean canAffordAndBuy(ShopEntry entry, int price) {
        // Only check balance and execute onBuy - cooldown/stock already checked
        return this.balance >= price && entry.onBuy(this.player);
    }

    private void completePurchase(ServerPlayerEntity player, ShopEntry entry, int index, int pricePaid) {
        this.balance -= pricePaid;

        // Apply cooldown
        applyCooldown(entry);

        // Consume stock
        consumeStock(entry.id());

        playSound(player, WatheSounds.UI_SHOP_BUY);
        ShopPurchase.AFTER.invoker().afterPurchase(player, entry, index, pricePaid);
    }

    public static boolean useBlackout(@NotNull PlayerEntity player) {
        boolean success = WorldBlackoutComponent.KEY.get(player.getWorld()).triggerBlackout();
        if (success && player instanceof ServerPlayerEntity serverPlayer) {
            // 关灯成功后，给所有杀手设置冷却
            applyBlackoutCooldownToAllKillers(serverPlayer);
        }
        return success;
    }

    private static void applyBlackoutCooldownToAllKillers(ServerPlayerEntity purchaser) {
        GameWorldComponent gameComponent = GameWorldComponent.KEY.get(purchaser.getWorld());
        int cooldownTicks = GameConstants.getInTicks(5, 0); // 5分钟冷却

        for (ServerPlayerEntity killer : purchaser.getServerWorld().getPlayers()) {
            if (gameComponent.canUseKillerFeatures(killer)) {
                PlayerShopComponent shop = KEY.get(killer);
                shop.cooldowns.put("blackout", cooldownTicks);
                shop.sync();
            }
        }
    }

    public static boolean usePsychoMode(@NotNull PlayerEntity player) {
        // No longer using MC cooldown manager - cooldown is handled by PlayerShopComponent
        return PlayerPsychoComponent.KEY.get(player).startPsycho();
    }

    // === NBT Serialization ===

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putInt("Balance", this.balance);

        // Save cooldowns
        NbtCompound cooldownsNbt = new NbtCompound();
        for (var entry : cooldowns.entrySet()) {
            cooldownsNbt.putInt(entry.getKey(), entry.getValue());
        }
        tag.put("Cooldowns", cooldownsNbt);

        // Save stock
        NbtCompound stockNbt = new NbtCompound();
        for (var entry : stock.entrySet()) {
            stockNbt.putInt(entry.getKey(), entry.getValue());
        }
        tag.put("Stock", stockNbt);

        // Save max stock cache
        NbtCompound maxStockNbt = new NbtCompound();
        for (var entry : maxStockCache.entrySet()) {
            maxStockNbt.putInt(entry.getKey(), entry.getValue());
        }
        tag.put("MaxStock", maxStockNbt);
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.balance = tag.getInt("Balance");

        // Load cooldowns
        cooldowns.clear();
        if (tag.contains("Cooldowns")) {
            NbtCompound cooldownsNbt = tag.getCompound("Cooldowns");
            for (String key : cooldownsNbt.getKeys()) {
                cooldowns.put(key, cooldownsNbt.getInt(key));
            }
        }

        // Load stock
        stock.clear();
        if (tag.contains("Stock")) {
            NbtCompound stockNbt = tag.getCompound("Stock");
            for (String key : stockNbt.getKeys()) {
                stock.put(key, stockNbt.getInt(key));
            }
        }

        // Load max stock cache
        maxStockCache.clear();
        if (tag.contains("MaxStock")) {
            NbtCompound maxStockNbt = tag.getCompound("MaxStock");
            for (String key : maxStockNbt.getKeys()) {
                maxStockCache.put(key, maxStockNbt.getInt(key));
            }
        }
    }
}
