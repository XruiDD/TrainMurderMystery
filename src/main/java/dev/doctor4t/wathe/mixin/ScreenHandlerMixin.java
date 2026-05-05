package dev.doctor4t.wathe.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.TrainWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScreenHandler.class)
public class ScreenHandlerMixin {

    // 游戏运行期间，ScreenHandler 关闭时阻止光标物品掉落，改为放回背包
    @WrapOperation(
            method = "onClosed",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/player/PlayerEntity;dropItem(Lnet/minecraft/item/ItemStack;Z)Lnet/minecraft/entity/ItemEntity;"
            )
    )
    private ItemEntity wathe$preventCursorDropOnScreenClose(
            PlayerEntity player,
            ItemStack stack,
            boolean retainOwnership,
            Operation<ItemEntity> original
    ) {
        if (player instanceof ServerPlayerEntity serverPlayer) {
            GameWorldComponent gameComponent = GameWorldComponent.KEY.get(serverPlayer.getWorld());
            if (gameComponent.isRunning()) {
                serverPlayer.getInventory().insertStack(stack);
                return null;
            }
        }
        return original.call(player, stack, retainOwnership);
    }

    // 游戏运行期间，阻止打开容器时通过槽位点击丢弃物品：
    //   1) 容器内按 Q (SlotActionType.THROW) 丢悬停槽位上的物品
    //   2) 把光标物品点到容器外松手 (SlotActionType.PICKUP, slotIndex == EMPTY_SPACE_SLOT_INDEX(-999))
    // 直接 HEAD cancel，光标/槽位物品保持原状；光标残留物品在玩家关闭容器时由
    // wathe$preventCursorDropOnScreenClose 塞回背包。
    @Inject(
            method = "internalOnSlotClick",
            at = @At("HEAD"),
            cancellable = true
    )
    private void wathe$preventInScreenDrop(int slotIndex, int button, SlotActionType actionType, PlayerEntity player, CallbackInfo ci) {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return;

        TrainWorldComponent trainWorldComponent = TrainWorldComponent.KEY.get(serverPlayer.getWorld());
        if (trainWorldComponent == null || !trainWorldComponent.hasHud()
                || !GameFunctions.isPlayerAliveAndSurvival(serverPlayer)) {
            return;
        }

        if (actionType == SlotActionType.THROW) {
            ci.cancel();
            return;
        }
        if (actionType == SlotActionType.PICKUP && slotIndex == ScreenHandler.EMPTY_SPACE_SLOT_INDEX) {
            ci.cancel();
        }
    }
}
