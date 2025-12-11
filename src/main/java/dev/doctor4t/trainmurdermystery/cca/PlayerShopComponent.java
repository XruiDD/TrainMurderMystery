package dev.doctor4t.trainmurdermystery.cca;

import dev.doctor4t.trainmurdermystery.TMM;
import dev.doctor4t.trainmurdermystery.event.BuildShopEntries;
import dev.doctor4t.trainmurdermystery.event.ShopPurchase;
import dev.doctor4t.trainmurdermystery.game.GameConstants;
import dev.doctor4t.trainmurdermystery.index.TMMItems;
import dev.doctor4t.trainmurdermystery.index.TMMSounds;
import dev.doctor4t.trainmurdermystery.util.ShopEntry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
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
    public static final ComponentKey<PlayerShopComponent> KEY = ComponentRegistry.getOrCreate(TMM.id("shop"), PlayerShopComponent.class);
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
        this.player.sendMessage(Text.translatable(translationKey).formatted(Formatting.DARK_RED), true);
        if (this.player instanceof ServerPlayerEntity serverPlayer) {
            serverPlayer.networkHandler.sendPacket(
                new PlaySoundS2CPacket(
                    Registries.SOUND_EVENT.getEntry(TMMSounds.UI_SHOP_BUY_FAIL),
                    SoundCategory.PLAYERS,
                    serverPlayer.getX(),
                    serverPlayer.getY(),
                    serverPlayer.getZ(),
                    1.0f,
                    0.9f + this.player.getRandom().nextFloat() * 0.2f,
                    player.getRandom().nextLong()
                )
            );
        }
    }

    public void tryBuy(int index) {
        // 1. Build shop entries via event
        // Empty array = no shop access
        BuildShopEntries.ShopContext context = new BuildShopEntries.ShopContext(GameConstants.SHOP_ENTRIES);
        BuildShopEntries.EVENT.invoker().buildEntries(player, context);
        List<ShopEntry> entries = context.getEntries();

        // 2. Check shop access via empty array
        if (entries.isEmpty()) {
            sendPurchaseError("shop.error.not_available");
            return;
        }

        // 3. Validate index
        if (index < 0 || index >= entries.size()) {
            sendPurchaseError("shop.error.invalid_item");
            return;
        }
        ShopEntry entry = entries.get(index);

        // 4. Fire before purchase event
        ShopPurchase.PurchaseResult purchaseResult = null;
        if (player instanceof ServerPlayerEntity serverPlayer) {
            purchaseResult = ShopPurchase.BEFORE.invoker().beforePurchase(serverPlayer, entry, index);
        }

        // 5. Handle before event result
        if (purchaseResult != null && !purchaseResult.allowed()) {
            // If event provides a custom reason, use it as literal text, otherwise use translation key
            if (purchaseResult.denyReason() != null) {
                this.player.sendMessage(Text.literal(purchaseResult.denyReason()).formatted(Formatting.DARK_RED), true);
                if (this.player instanceof ServerPlayerEntity serverPlayer) {
                    serverPlayer.networkHandler.sendPacket(new PlaySoundS2CPacket(Registries.SOUND_EVENT.getEntry(TMMSounds.UI_SHOP_BUY_FAIL), SoundCategory.PLAYERS, serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(), 1.0f, 0.9f + this.player.getRandom().nextFloat() * 0.2f, player.getRandom().nextLong()));
                }
            } else {
                sendPurchaseError("shop.error.purchase_denied");
            }
            return;
        }

        // 6. Determine actual price
        int actualPrice = entry.price();
        if (purchaseResult != null && purchaseResult.hasModifiedPrice()) {
            actualPrice = purchaseResult.modifiedPrice();
        }

        // 7. Development environment debug
        if (FabricLoader.getInstance().isDevelopmentEnvironment() && this.balance < actualPrice)
            this.balance = actualPrice * 10;

        // 8. Execute purchase
        if (this.balance >= actualPrice && !this.player.getItemCooldownManager().isCoolingDown(entry.stack().getItem()) && entry.onBuy(this.player)) {
            this.balance -= actualPrice;
            if (this.player instanceof ServerPlayerEntity serverPlayer) {
                serverPlayer.networkHandler.sendPacket(new PlaySoundS2CPacket(Registries.SOUND_EVENT.getEntry(TMMSounds.UI_SHOP_BUY), SoundCategory.PLAYERS, serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(), 1.0f, 0.9f + this.player.getRandom().nextFloat() * 0.2f, player.getRandom().nextLong()));

                // 9. Fire after purchase event
                ShopPurchase.AFTER.invoker().afterPurchase(serverPlayer, entry, index, actualPrice);
            }
        } else {
            sendPurchaseError("shop.error.purchase_failed");
        }
        this.sync();
    }

    @Override
    public void clientTick() {

    }

    @Override
    public void serverTick() {

    }

    public static boolean useBlackout(@NotNull PlayerEntity player) {
        player.getItemCooldownManager().set(TMMItems.BLACKOUT, GameConstants.ITEM_COOLDOWNS.getOrDefault(TMMItems.BLACKOUT, 0));
        return WorldBlackoutComponent.KEY.get(player.getWorld()).triggerBlackout();
    }

    public static boolean usePsychoMode(@NotNull PlayerEntity player) {
        player.getItemCooldownManager().set(TMMItems.PSYCHO_MODE, GameConstants.ITEM_COOLDOWNS.getOrDefault(TMMItems.PSYCHO_MODE, 0));
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