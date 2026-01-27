package dev.doctor4t.wathe.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerPsychoComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.index.WatheItems;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ServerPlayNetworkHandler.class)
public class ServerPlayNetworkHandlerMixin {
    @Shadow
    public ServerPlayerEntity player;

    @WrapMethod(method = "onUpdateSelectedSlot")
    private void wathe$invalid(UpdateSelectedSlotC2SPacket packet, @NotNull Operation<Void> original) {
        PlayerPsychoComponent component = PlayerPsychoComponent.KEY.get(this.player);
        if (component.getPsychoTicks() > 0 && !this.player.getInventory().getStack(packet.getSelectedSlot()).isOf(WatheItems.BAT))
            return;
        original.call(packet);
    }

    // 服务端限制丢弃物品和交换手
    @WrapMethod(method = "onPlayerAction")
    private void wathe$restrictPlayerActions(PlayerActionC2SPacket packet, @NotNull Operation<Void> original) {
        // 如果玩家是存活的生存模式玩家
        GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(this.player.getWorld());
        if (gameWorldComponent != null && gameWorldComponent.isRunning() && GameFunctions.isPlayerAliveAndSurvival(this.player)) {
            // 阻止丢弃物品
            if (packet.getAction() == PlayerActionC2SPacket.Action.DROP_ITEM ||
                packet.getAction() == PlayerActionC2SPacket.Action.DROP_ALL_ITEMS) {
                return;
            }
            // 阻止交换手
            if (packet.getAction() == PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND) {
                return;
            }
        }
        original.call(packet);
    }
}