package dev.doctor4t.wathe.mixin.client;

import dev.doctor4t.wathe.client.WatheClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {
    @Inject(method = "onGameJoin", at = @At("HEAD"))
    private void wathe$clearPlayerEntryCache(GameJoinS2CPacket packet, CallbackInfo ci) {
        WatheClient.PLAYER_ENTRIES_CACHE.clear();
    }

    @Inject(method = "onPlayerList", at = @At("TAIL"))
    private void wathe$updatePlayerEntryCache(PlayerListS2CPacket packet, CallbackInfo ci) {
        ClientPlayNetworkHandler networkHandler = (ClientPlayNetworkHandler) (Object) this;
        for (PlayerListS2CPacket.Entry entry : packet.getEntries()) {
            PlayerListEntry playerListEntry = networkHandler.getPlayerListEntry(entry.profileId());
            if (playerListEntry != null) {
                WatheClient.PLAYER_ENTRIES_CACHE.put(entry.profileId(), playerListEntry);
            }
        }
    }
}
