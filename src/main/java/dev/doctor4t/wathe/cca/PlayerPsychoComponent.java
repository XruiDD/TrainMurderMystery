package dev.doctor4t.wathe.cca;

import dev.doctor4t.wathe.Wathe;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.util.ShopEntry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.ClientTickingComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

public class PlayerPsychoComponent implements AutoSyncedComponent, ServerTickingComponent, ClientTickingComponent {
    public static final ComponentKey<PlayerPsychoComponent> KEY = ComponentRegistry.getOrCreate(Wathe.id("psycho"), PlayerPsychoComponent.class);
    private final PlayerEntity player;
    public int psychoTicks = 0;
    public int armour = 1;

    public PlayerPsychoComponent(PlayerEntity player) {
        this.player = player;
    }

    public void sync() {
        KEY.sync(this.player);
    }

    @Override
    public void writeSyncPacket(RegistryByteBuf buf, ServerPlayerEntity recipient) {
        buf.writeVarInt(this.psychoTicks);
        buf.writeVarInt(this.armour);
    }

    @Override
    public void applySyncPacket(RegistryByteBuf buf) {
        this.psychoTicks = buf.readVarInt();
        this.armour = buf.readVarInt();
    }

    public void reset() {
        this.stopPsycho();
        this.sync();
    }

    @Override
    public void clientTick() {
        if (this.psychoTicks <= 0) return;
        this.psychoTicks--;
        if (this.player.getMainHandStack().isOf(WatheItems.BAT)) return;
        if (GameFunctions.isPlayerPlayingAndAlive(player)) {
            for (int i = 0; i < 9; i++) {
                if (!this.player.getInventory().getStack(i).isOf(WatheItems.BAT)) continue;
                this.player.getInventory().selectedSlot = i;
                break;
            }
        }
    }

    @Override
    public void serverTick() {
        if (this.psychoTicks <= 0) return;

        if (--this.psychoTicks == 0) {
            this.stopPsycho();
            this.sync(); // 结束时广播给所有人（其他玩家需要知道狂暴结束）
        } else if (this.psychoTicks % 20 == 0 && this.player instanceof ServerPlayerEntity serverPlayer) {
            // 每秒只同步给自己，校正客户端进度条时间
            // 其他玩家只需要知道是否狂暴（>0），不需要精确时间
            KEY.sync(serverPlayer);
        }
    }

    public boolean startPsycho() {
        if (ShopEntry.insertStackInFreeSlot(this.player, new ItemStack(WatheItems.BAT))) {
            this.setPsychoTicks(GameConstants.PSYCHO_TIMER);
            this.setArmour(GameConstants.PSYCHO_MODE_ARMOUR);
            GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(this.player.getWorld());
            gameWorldComponent.setPsychosActive(gameWorldComponent.getPsychosActive() + 1);
            return true;
        }
        return false;
    }

    public void stopPsycho() {
        GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(this.player.getWorld());
        gameWorldComponent.setPsychosActive(gameWorldComponent.getPsychosActive() - 1);
        this.psychoTicks = 0;
        this.player.getInventory().remove(itemStack -> itemStack.isOf(WatheItems.BAT), Integer.MAX_VALUE, this.player.playerScreenHandler.getCraftingInput());
    }

    public int getArmour() {
        return this.armour;
    }

    public void setArmour(int armour) {
        this.armour = armour;
        this.sync();
    }

    public int getPsychoTicks() {
        return this.psychoTicks;
    }

    public void setPsychoTicks(int ticks) {
        this.psychoTicks = ticks;
        this.sync();
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.@NotNull WrapperLookup registryLookup) {
        tag.putInt("psychoTicks", this.psychoTicks);
        tag.putInt("armour", this.armour);
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.@NotNull WrapperLookup registryLookup) {
        this.psychoTicks = tag.contains("psychoTicks") ? tag.getInt("psychoTicks") : 0;
        this.armour = tag.contains("armour") ? tag.getInt("armour") : 1;
    }
}