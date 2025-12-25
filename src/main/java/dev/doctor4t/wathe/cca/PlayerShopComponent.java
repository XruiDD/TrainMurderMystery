package dev.doctor4t.wathe.cca;

import dev.doctor4t.wathe.Wathe;
import dev.doctor4t.wathe.api.event.ShopPurchase;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.index.WatheItems;
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
import org.ladysnake.cca.api.v3.component.tick.ClientTickingComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

import java.util.List;

public class PlayerShopComponent implements AutoSyncedComponent, ServerTickingComponent, ClientTickingComponent {
    public static final ComponentKey<PlayerShopComponent> KEY = ComponentRegistry.getOrCreate(Wathe.id("shop"), PlayerShopComponent.class);
    private final PlayerEntity player;
    public int balance = 0;

    public PlayerShopComponent(PlayerEntity player) {
        this.player = player;
    }

    public void sync() {
        KEY.sync(this.player);
    }

    public void reset() {
        this.balance = 0;
        this.sync();
    }

    public void addToBalance(int amount) {
        this.setBalance(this.balance + amount);
    }

    public void setBalance(int amount) {
        if (this.balance != amount) {
            this.balance = amount;
            this.sync();
        }
    }


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
        return this.balance >= price
            && !this.player.getItemCooldownManager().isCoolingDown(entry.stack().getItem())
            && entry.onBuy(this.player);
    }

    private void completePurchase(ServerPlayerEntity player, ShopEntry entry, int index, int pricePaid) {
        this.balance -= pricePaid;
        playSound(player, WatheSounds.UI_SHOP_BUY);
        ShopPurchase.AFTER.invoker().afterPurchase(player, entry, index, pricePaid);
    }

    @Override
    public void clientTick() {

    }

    @Override
    public void serverTick() {

    }

    public static boolean useBlackout(@NotNull PlayerEntity player) {
        player.getItemCooldownManager().set(WatheItems.BLACKOUT, GameConstants.ITEM_COOLDOWNS.getOrDefault(WatheItems.BLACKOUT, 0));
        return WorldBlackoutComponent.KEY.get(player.getWorld()).triggerBlackout();
    }

    public static boolean usePsychoMode(@NotNull PlayerEntity player) {
        player.getItemCooldownManager().set(WatheItems.PSYCHO_MODE, GameConstants.ITEM_COOLDOWNS.getOrDefault(WatheItems.PSYCHO_MODE, 0));
        return PlayerPsychoComponent.KEY.get(player).startPsycho();
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putInt("Balance", this.balance);
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.balance = tag.getInt("Balance");
    }
}