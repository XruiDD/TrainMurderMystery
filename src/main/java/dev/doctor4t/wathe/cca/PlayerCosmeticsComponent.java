package dev.doctor4t.wathe.cca;

import dev.doctor4t.wathe.Wathe;
import dev.doctor4t.wathe.client.skin.PlayerSkinTextureManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;

import java.util.HashMap;
import java.util.Map;

/**
 * 玩家装备的非物品类 cosmetic（目前仅 PLAYER_SKIN）。
 * <p>
 * Server: 在 ServerPlayConnectionEvents.JOIN 与 PsychoModeEvents.ON_PSYCHO_START 之后由
 * {@link dev.doctor4t.wathe.cosmetic.CosmeticApiClient} 拉取并通过 {@link #setPlayerSkins} 写入。
 * <p>
 * Client: AutoSyncedComponent 自动通过 entity tracking 同步到所有 trackers；applySyncPacket 顺手预热纹理。
 */
public class PlayerCosmeticsComponent implements AutoSyncedComponent {
    public static final ComponentKey<PlayerCosmeticsComponent> KEY =
            ComponentRegistry.getOrCreate(Wathe.id("cosmetics"), PlayerCosmeticsComponent.class);

    /**
     * @param cosmeticId  目录里唯一标识，便于 client 调试
     * @param textureUrl  皮肤 PNG 完整 URL
     * @param model       "WIDE" 或 "SLIM"
     */
    public record PlayerSkinEntry(String cosmeticId, String textureUrl, String model) {}

    private final PlayerEntity player;
    private final Map<String, PlayerSkinEntry> playerSkins = new HashMap<>();

    public PlayerCosmeticsComponent(PlayerEntity player) {
        this.player = player;
    }

    public @Nullable PlayerSkinEntry getPlayerSkin(String slot) {
        return playerSkins.get(slot);
    }

    /**
     * Server 调用：覆盖整个 player skin map 并触发 CCA sync。
     * 即使 entries 为空也会调用，覆盖旧 component 状态。
     */
    public void setPlayerSkins(Map<String, PlayerSkinEntry> entries) {
        this.playerSkins.clear();
        this.playerSkins.putAll(entries);
        KEY.sync(this.player);
    }

    @Override
    public void writeSyncPacket(@NotNull RegistryByteBuf buf, @NotNull ServerPlayerEntity recipient) {
        buf.writeVarInt(playerSkins.size());
        for (Map.Entry<String, PlayerSkinEntry> e : playerSkins.entrySet()) {
            PlayerSkinEntry entry = e.getValue();
            buf.writeString(e.getKey());
            buf.writeString(entry.cosmeticId());
            buf.writeString(entry.textureUrl());
            buf.writeString(entry.model());
        }
    }

    @Override
    public void applySyncPacket(@NotNull RegistryByteBuf buf) {
        playerSkins.clear();
        int n = buf.readVarInt();
        for (int i = 0; i < n; i++) {
            String slot = buf.readString();
            String cid = buf.readString();
            String url = buf.readString();
            String model = buf.readString();
            playerSkins.put(slot, new PlayerSkinEntry(cid, url, model));
        }
        // 客户端预热：组件刚同步过来，立即触发 HTTP 拉皮肤
        if (player.getWorld().isClient) {
            for (PlayerSkinEntry entry : playerSkins.values()) {
                PlayerSkinTextureManager.getInstance().ensureLoaded(entry.textureUrl());
            }
        }
    }

    // 不持久化到 NBT —— 每次连接通过 API 重新拉取
    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.@NotNull WrapperLookup registryLookup) {}

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.@NotNull WrapperLookup registryLookup) {}
}
